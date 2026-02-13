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
    private final LawyerRepository lawyerRepository;
    private final GenderDetectionService genderDetectionService;

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
            GenderDetectionService genderDetectionService) {
        this.whisperService = whisperService;
        this.maskingService = maskingService;
        this.textToSpeechService = textToSpeechService;
        this.translationService = translationService;
        this.repository = repository;
        this.caseService = caseService;
        this.classificationService = classificationService;
        this.lawyerRepository = lawyerRepository;
        this.genderDetectionService = genderDetectionService;
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
                .orElseThrow(() -> new com.legalconnect.lawyerbooking.exception.ResourceNotFoundException(
                        "Lawyer not found"));

        java.util.Set<CaseType> specs = lawyer.getSpecializations();
        // If no specs, they might only see audio for cases explicitly assigned to them
        // The JPQL handles this if we pass the specs (even if empty)
        return repository.findForLawyer(lawyerId, specs).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public ClientAudioDTO convertToDTO(ClientAudio ca) {
        ClientAudioDTO dto = new ClientAudioDTO(
                ca.getId(),
                ca.getLanguage(),
                ca.getOriginalEnglishText(),
                ca.getMaskedEnglishText(),
                ca.getMaskedTextAudio(),
                ca.getMaskedGujaratiText(),
                ca.getMaskedGujaratiAudio(),
                ca.getUserId(),
                ca.getCaseId(),
                ca.getLawyerId());

        if (ca.getCaseId() != null) {
            try {
                // Fetch case title to replace log identification with something descriptive
                CaseDTO caseDTO = caseService.getCaseById(ca.getCaseId());
                if (caseDTO != null) {
                    dto.setCaseTitle(caseDTO.getCaseTitle());
                }
            } catch (Exception e) {
                logger.warn("Could not fetch case title for audio record {}: {}", ca.getId(), e.getMessage());
            }
        }

        return dto;
    }

    /**
     * Orchestrates the full audio processing workflow, including case creation if
     * userId is present.
     * 
     * @param audio     The uploaded audio file
     * @param userId    The ID of the uploading user (optional)
     * @param caseTitle The title for the created case (optional)
     * @return The processed and saved ClientAudio entity
     */
    @Transactional
    public ClientAudio processAndCreateCase(MultipartFile audio, Long userId, String caseTitle, Long lawyerId) {
        // Core Processing Phase
        ClientAudio clientAudio = processAudioPipeline(audio, userId);

        // Case Creation Phase
        if (userId != null) {
            linkToCase(clientAudio, userId, caseTitle, audio.getOriginalFilename(), lawyerId);
        } else {
            logger.warn("UserId is null, skipping case creation for audio ID: {}", clientAudio.getId());
        }

        return clientAudio;
    }

    // Legacy method support if needed, or redirect to main flow
    public ClientAudio process(MultipartFile audio) {
        return processAndCreateCase(audio, null, null, null);
    }

    public ClientAudio process(MultipartFile audio, Long userId) {
        return processAndCreateCase(audio, userId, null, null);
    }

    private ClientAudio processAudioPipeline(MultipartFile audio, Long userId) {
        try {
            logger.info("Starting audio pipeline for file: {} (size: {} bytes)",
                    audio.getOriginalFilename(), audio.getSize());

            // 1. Gender Detection
            String gender = detectGender(audio);

            // 2. Transcription
            String originalEnglish = transcribeAudio(audio);

            // 3. Masking
            String maskedEnglish = maskPersonalInfo(originalEnglish);

            // 4. Translation (Keep translation text, but skip audio)
            String maskedGujarati = translateToGujarati(maskedEnglish);

            // TTS is now generated ON-DEMAND via TTSController to save costs.
            // We initialize with null audio bytes.
            byte[] maskedTextAudio = null;
            byte[] maskedGujaratiAudio = null;

            // 5. Persistence
            return saveClientAudio(userId, originalEnglish, maskedEnglish,
                    maskedTextAudio, maskedGujarati, maskedGujaratiAudio, gender);

        } catch (Exception e) {
            logger.error("Audio pipeline failed: {}", e.getMessage(), e);
            throw new AudioProcessingException("Failed to process audio file", e);
        }
    }

    private String detectGender(MultipartFile audio) {
        logger.debug("Step 0: Detecting gender from audio...");
        try {
            String detectedGender = genderDetectionService.detectGender(audio);
            logger.info("✓ Gender detection completed: '{}'", detectedGender);
            return detectedGender;
        } catch (Exception e) {
            logger.error("Gender detection failed, defaulting to NEUTRAL", e);
            return "NEUTRAL";
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

    private String translateToGujarati(String text) {
        logger.debug("Step 4: Translating to Gujarati...");
        try {
            return translationService.translateToGujarati(text);
        } catch (Exception e) {
            logger.error("Gujarati translation failed", e);
            return null; // Non-blocking failure
        }
    }

    private ClientAudio saveClientAudio(Long userId, String original, String masked,
            byte[] audioEn, String gujarati, byte[] audioGu, String gender) {
        logger.info("Saving ClientAudio with gender: '{}'", gender);
        ClientAudio ca = new ClientAudio();
        ca.setUserId(userId);
        ca.setLanguage("english");
        ca.setOriginalEnglishText(original);
        ca.setMaskedEnglishText(masked);
        ca.setMaskedTextAudio(audioEn);
        ca.setMaskedGujaratiText(gujarati);
        ca.setMaskedGujaratiAudio(audioGu);
        ca.setGender(gender);
        ClientAudio saved = repository.save(ca);
        logger.info("✓ Saved ClientAudio ID: {}, Gender in DB: '{}'", saved.getId(), saved.getGender());
        return saved;
    }

    private void linkToCase(ClientAudio clientAudio, Long userId, String caseTitle, String fileName, Long lawyerId) {
        try {
            String title = caseTitle;

            if (title == null || title.trim().isEmpty()) {
                logger.debug("Step 5.5: Generating AI Title...");
                title = classificationService.generateTitle(clientAudio.getMaskedEnglishText());

                if (title == null) {
                    title = "Case from Audio - " + (fileName != null ? fileName : "recording");
                }
            }

            CaseRequest caseRequest = new CaseRequest();
            caseRequest.setUserId(userId);
            caseRequest.setCaseTitle(title);
            caseRequest.setLawyerId(lawyerId);

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

            System.out.println(">>> [STEP 7] Triggering CaseService.createCase for user: " + userId);
            logger.info("Step 7: Creating case via CaseService for user {}...", userId);
            CaseDTO caseDTO = caseService.createCase(caseRequest);

            if (caseDTO != null && caseDTO.getId() != null) {
                System.out.println(">>> [STEP 8] Case created SUCCESSFULLY! ID: " + caseDTO.getId());
                logger.info("Step 8: Successfully created case ID: {}", caseDTO.getId());
                clientAudio.setCaseId(caseDTO.getId());
                repository.save(clientAudio);
                System.out.println(">>> [STEP 9] Audio linked to Case ID: " + caseDTO.getId());
                logger.info("Step 9: Linked audio ID {} to new Case ID {}", clientAudio.getId(), caseDTO.getId());
            } else {
                System.err.println(">>> [STEP 8 FAIL] CaseDTO is NULL or missing ID!");
                logger.error("Step 8 FAIL: CaseService returned null or DTO with no ID!");
            }
        } catch (Exception e) {
            System.err.println(">>> [AUDIO PROCESSING ERROR] " + e.getMessage());
            e.printStackTrace();
            logger.error("Failed to create/link case for user {}: {}", userId, e.getMessage(), e);
            // We do NOT throw here to preserve the saved audio
        }
    }
}
