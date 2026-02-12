package com.legalconnect.lawyerbooking.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.legalconnect.lawyerbooking.service.OpenAITextToSpeechService;
import com.legalconnect.lawyerbooking.repository.ClientAudioRepository;
import com.legalconnect.lawyerbooking.entity.ClientAudio;
import com.legalconnect.lawyerbooking.exception.ResourceNotFoundException;
import java.util.Base64;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/tts")
@CrossOrigin(origins = "*")
public class TTSController {

    private static final Logger logger = LoggerFactory.getLogger(TTSController.class);

    @Autowired
    private OpenAITextToSpeechService ttsService;

    @Autowired
    private ClientAudioRepository clientAudioRepository;

    @PostMapping("/generate")
    public ResponseEntity<Map<String, String>> generateTTS(@RequestBody Map<String, Object> request) {
        try {
            Long caseId = Long.valueOf(request.get("caseId").toString());
            String language = (String) request.getOrDefault("language", "en");

            logger.info("Received TTS request for caseId: {}, language: {}", caseId, language);

            // 1. Retrieve ClientAudio by caseId
            // Note: We assume one audio record per case for now. 
            // If multiple exist (unlikely in current flow), we take the first one.
            ClientAudio clientAudio = clientAudioRepository.findAllAudio().stream()
                .filter(ca -> caseId.equals(ca.getCaseId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Audio record not found for case ID: " + caseId));

            byte[] audioBytes = null;

            // 2. Check if audio already exists (Cache Hit)
            if ("gu".equalsIgnoreCase(language)) {
                if (clientAudio.getMaskedGujaratiAudio() != null && clientAudio.getMaskedGujaratiAudio().length > 0) {
                    logger.info("Returning CACHED Gujarati audio for case {}", caseId);
                    audioBytes = clientAudio.getMaskedGujaratiAudio();
                } else {
                    // Generate new
                    logger.info("Generating NEW Gujarati audio for case {}", caseId);
                    String textToSpeak = clientAudio.getMaskedGujaratiText();
                    if (textToSpeak == null || textToSpeak.isEmpty()) {
                        return ResponseEntity.badRequest().body(Map.of("error", "No Gujarati text available to speak"));
                    }
                    audioBytes = ttsService.textToSpeech(textToSpeak, "gu");
                    
                    // Save to DB (Cache)
                    clientAudio.setMaskedGujaratiAudio(audioBytes);
                    clientAudioRepository.save(clientAudio);
                }
            } else {
                // Default to English
                if (clientAudio.getMaskedTextAudio() != null && clientAudio.getMaskedTextAudio().length > 0) {
                    logger.info("Returning CACHED English audio for case {}", caseId);
                    audioBytes = clientAudio.getMaskedTextAudio();
                } else {
                    // Generate new
                    logger.info("Generating NEW English audio for case {}", caseId);
                    String textToSpeak = clientAudio.getMaskedEnglishText();
                    if (textToSpeak == null || textToSpeak.isEmpty()) {
                        return ResponseEntity.badRequest().body(Map.of("error", "No English text available to speak"));
                    }
                    audioBytes = ttsService.textToSpeech(textToSpeak, "en");
                    
                    // Save to DB (Cache)
                    clientAudio.setMaskedTextAudio(audioBytes);
                    clientAudioRepository.save(clientAudio);
                }
            }

            // 3. Return as Base64
            String base64Audio = Base64.getEncoder().encodeToString(audioBytes);
            Map<String, String> response = new HashMap<>();
            response.put("audio", base64Audio);
            response.put("language", language);
            
            return ResponseEntity.ok(response);

        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid case ID format"));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("TTS generation failed", e);
            return ResponseEntity.status(500).body(Map.of("error", "Failed to generate audio: " + e.getMessage()));
        }
    }
}
