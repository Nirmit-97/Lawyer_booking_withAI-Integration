package com.legalconnect.lawyerbooking.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.legalconnect.lawyerbooking.entity.ClientAudio;
import com.legalconnect.lawyerbooking.dto.CaseRequest;
import com.legalconnect.lawyerbooking.dto.CaseDTO;
import com.legalconnect.lawyerbooking.dto.ClientAudioDTO;
import com.legalconnect.lawyerbooking.repository.ClientAudioRepository;
import com.legalconnect.lawyerbooking.exception.AudioProcessingException;
import com.legalconnect.lawyerbooking.enums.CaseType;
import com.legalconnect.lawyerbooking.repository.LawyerRepository;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service responsible for processing audio files...
 */
@Service
public class AudioProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(AudioProcessingService.class);

    private final OpenAIWhisperService whisperService;
    private final TextMaskingService maskingService;
    private final OpenAITextToSpeechService textToSpeechService;
    private final TextTranslationService translationService;
    private final ClientAudioRepository repository;
    private final CaseService caseService;
    private final CaseClassificationService classificationService;
    private final AudioAnalysisService audioAnalysisService;
    private final LawyerRepository lawyerRepository;

    @Autowired
    public AudioProcessingService(
            OpenAIWhisperService whisperService,
            TextMaskingService maskingService,
            OpenAITextToSpeechService textToSpeechService,
            TextTranslationService translationService,
            ClientAudioRepository repository,
            CaseService caseService,
            CaseClassificationService classificationService,
            LawyerRepository lawyerRepository,
            AudioAnalysisService audioAnalysisService) {
        this.whisperService = whisperService;
        this.maskingService = maskingService;
        this.textToSpeechService = textToSpeechService;
        this.translationService = translationService;
        this.repository = repository;
        this.caseService = caseService;
        this.classificationService = classificationService;
        this.lawyerRepository = lawyerRepository;
        this.audioAnalysisService = audioAnalysisService;
    }

    public List<ClientAudioDTO> getAllAudioForAdmin() {
        return repository.findAllAudio().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<ClientAudioDTO> getAudioForUser(Long userId) {
        return repository.findForUser(userId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<ClientAudioDTO> getAudioForLawyer(Long lawyerId) {
        var lawyer = lawyerRepository.findById(lawyerId)
                .orElseThrow(() -> new com.legalconnect.lawyerbooking.exception.ResourceNotFoundException("Lawyer not found"));
        
        java.util.Set<CaseType> specs = lawyer.getSpecializations();
        // If no specs, they might only see audio for cases explicitly assigned to them
        // The JPQL handles this if we pass the specs (even if empty)
        return repository.findForLawyer(lawyerId, specs).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private ClientAudioDTO convertToDTO(ClientAudio ca) {
        return new ClientAudioDTO(
            ca.getId(),
            ca.getLanguage(),
            ca.getOriginalEnglishText(),
            ca.getMaskedEnglishText(),
            ca.getMaskedTextAudio(),
            ca.getMaskedGujaratiText(),
            ca.getMaskedGujaratiAudio(),
            ca.getUserId(),
            ca.getCaseId(),
            ca.getLawyerId()
        );
    }

    /**
     * Orchestrates the full audio processing workflow, including case creation if userId is present.
     * @param audio The uploaded audio file
     * @param userId The ID of the uploading user (optional)
     * @param caseTitle The title for the created case (optional)
     * @return The processed and saved ClientAudio entity
     */
    @Transactional
    public ClientAudio processAndCreateCase(MultipartFile audio, Long userId, String caseTitle) {
        // Core Processing Phase
        ClientAudio clientAudio = processAudioPipeline(audio, userId);

        // Case Creation Phase
        if (userId != null) {
            linkToCase(clientAudio, userId, caseTitle, audio.getOriginalFilename());
        } else {
            logger.warn("UserId is null, skipping case creation for audio ID: {}", clientAudio.getId());
        }

        return clientAudio;
    }
    
    // Legacy method support if needed, or redirect to main flow
    public ClientAudio process(MultipartFile audio) {
        return processAndCreateCase(audio, null, null);
    }
    
    public ClientAudio process(MultipartFile audio, Long userId) {
        return processAndCreateCase(audio, userId, null);
    }

    private ClientAudio processAudioPipeline(MultipartFile audio, Long userId) {
        try {
            logger.info("Starting audio pipeline for file: {} (size: {} bytes)", 
                       audio.getOriginalFilename(), audio.getSize());

            // 1. Transcription
            String originalEnglish = transcribeAudio(audio);

            // 2. Masking
            String maskedEnglish = maskPersonalInfo(originalEnglish);

            // 1.5 Gender Detection
            String gender = "MALE";
            java.io.File tempFile = null;
            try {
                // Create a temporary file to analyze pitch
                tempFile = java.io.File.createTempFile("audio_analysis_", ".tmp");
                try (java.io.InputStream in = audio.getInputStream();
                     java.io.FileOutputStream out = new java.io.FileOutputStream(tempFile)) {
                    in.transferTo(out);
                }
                gender = audioAnalysisService.detectGender(tempFile);
                logger.debug("Detected Gender: {}", gender);
            } catch (Exception e) {
                logger.warn("Gender detection failed, defaulting to MALE: {}", e.getMessage());
            } finally {
                if (tempFile != null && tempFile.exists()) {
                    tempFile.delete();
                }
            }

            // 3. Audio & Translation Generation (Parallelizable in future)
            byte[] maskedTextAudio = generateEnglishAudio(maskedEnglish, gender);
            String maskedGujarati = translateToGujarati(maskedEnglish);
            byte[] maskedGujaratiAudio = generateGujaratiAudio(maskedGujarati, gender);

            // 4. Persistence
            return saveClientAudio(userId, originalEnglish, maskedEnglish, 
                                 maskedTextAudio, maskedGujarati, maskedGujaratiAudio);

        } catch (Exception e) {
            logger.error("Audio pipeline failed: {}", e.getMessage(), e);
            throw new AudioProcessingException("Failed to process audio file", e);
        }
    }

    private String transcribeAudio(MultipartFile audio) {
        logger.debug("Step 1: Transcribing audio...");
        String text = null;
        try {
            text = whisperService.translateToEnglish(audio);
        } catch (Exception e) {
             throw new AudioProcessingException("Whisper transcription failed", e);
        }
        
        if (text == null || text.trim().isEmpty()) {
            throw new AudioProcessingException("Transcription returned empty text");
        }
        logger.info("Transcription completed. Length: {}", text.length());
        return text;
    }

    private String maskPersonalInfo(String text) {
        logger.debug("Step 2: Masking personal info...");
        String masked = maskingService.maskEnglishPersonalInfo(text);
        if (masked == null || masked.trim().isEmpty()) {
            logger.warn("Masking returned empty, falling back to original");
            return text;
        }
        return masked;
    }

    private byte[] generateEnglishAudio(String text, String gender) {
        logger.debug("Step 3: Generating English TTS (Voice: {})...", gender);
        try {
            return textToSpeechService.textToSpeech(text, "en", gender);
        } catch (Exception e) {
            logger.error("English TTS failed", e);
            return null; // Non-blocking failure
        }
    }

    private String translateToGujarati(String text) {
        logger.debug("Step 4: Translating to Gujarati...");
        try {
            return translationService.translateToGujarati(text);
        } catch (Exception e) {
            logger.error("Gujarati translation failed", e);
            return null; // Non-blocking failure
        }
    }

    private byte[] generateGujaratiAudio(String text, String gender) {
        if (text == null || text.trim().isEmpty()) return null;
        
        logger.debug("Step 5: Generating Gujarati TTS (Voice: {})...", gender);
        try {
            return textToSpeechService.textToSpeech(text, "gu", gender);
        } catch (Exception e) {
            logger.error("Gujarati TTS failed", e);
            return null; // Non-blocking failure
        }
    }

    private ClientAudio saveClientAudio(Long userId, String original, String masked, 
                                      byte[] audioEn, String gujarati, byte[] audioGu) {
        ClientAudio ca = new ClientAudio();
        ca.setUserId(userId);
        ca.setLanguage("english");
        ca.setOriginalEnglishText(original);
        ca.setMaskedEnglishText(masked);
        ca.setMaskedTextAudio(audioEn);
        ca.setMaskedGujaratiText(gujarati);
        ca.setMaskedGujaratiAudio(audioGu);
        return repository.save(ca);
    }

    private void linkToCase(ClientAudio clientAudio, Long userId, String caseTitle, String fileName) {
        try {
            String title = (caseTitle != null && !caseTitle.trim().isEmpty()) 
                ? caseTitle 
                : "Case from Audio - " + (fileName != null ? fileName : "recording");

            CaseRequest caseRequest = new CaseRequest();
            caseRequest.setUserId(userId);
            caseRequest.setCaseTitle(title);
            
            // 6. Classification
            logger.debug("Step 6: Classifying case category...");
            String category = classificationService.classifyCase(clientAudio.getMaskedEnglishText());
            
            // Map classified category to CaseType enum
            try {
                if (category != null && !category.trim().isEmpty()) {
                    String normCategory = category.trim().toUpperCase().replace(" ", "_");
                    caseRequest.setCaseType(com.legalconnect.lawyerbooking.enums.CaseType.valueOf(normCategory));
                } else {
                    caseRequest.setCaseType(com.legalconnect.lawyerbooking.enums.CaseType.OTHER);
                }
            } catch (Exception e) {
                logger.warn("Failed to map audio classification '{}' to CaseType, defaulting to OTHER", category);
                caseRequest.setCaseType(com.legalconnect.lawyerbooking.enums.CaseType.OTHER);
            }
            
            // Generate description safely (max 500 chars)
            String description = clientAudio.getMaskedEnglishText() != null 
                ? clientAudio.getMaskedEnglishText() 
                : "Case created from audio upload";
            
            if (description.length() > 500) {
                description = description.substring(0, 497) + "...";
            }
            caseRequest.setDescription(description);

            logger.info("Step 7: Creating case via CaseService for user {}...", userId);
            CaseDTO caseDTO = caseService.createCase(caseRequest);
            
            if (caseDTO != null && caseDTO.getId() != null) {
                logger.info("Step 8: Successfully created case ID: {}", caseDTO.getId());
                clientAudio.setCaseId(caseDTO.getId());
                repository.save(clientAudio);
                logger.info("Step 9: Linked audio ID {} to new Case ID {}", clientAudio.getId(), caseDTO.getId());
            } else {
                logger.error("Step 8 FAIL: CaseService returned null or DTO with no ID!");
            }
        } catch (Exception e) {
            logger.error("Failed to create/link case for user {}: {}", userId, e.getMessage(), e);
            // We do NOT throw here to preserve the saved audio
        }
    }
}
