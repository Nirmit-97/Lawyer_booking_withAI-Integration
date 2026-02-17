package com.legalconnect.lawyerbooking.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.servlet.http.HttpServletRequest;

import com.legalconnect.lawyerbooking.service.AudioProcessingService;
import com.legalconnect.lawyerbooking.repository.ClientAudioRepository;
import com.legalconnect.lawyerbooking.entity.ClientAudio;
import com.legalconnect.lawyerbooking.dto.ClientAudioDTO;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/audio")
@CrossOrigin(origins = "*")
public class AudioController {

    @Autowired
    private AudioProcessingService audioService;

    @Autowired
    private ClientAudioRepository repository;

    @Autowired
    private com.legalconnect.lawyerbooking.service.CaseService caseService;

    @Autowired
    private com.legalconnect.lawyerbooking.service.AuthorizationService authorizationService;

    @Autowired
    private com.legalconnect.lawyerbooking.repository.LawyerRepository lawyerRepository;

    @Autowired
    private com.legalconnect.lawyerbooking.service.RateLimitService rateLimitService;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadAudio(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "caseTitle", required = false) String caseTitle,
            @RequestParam(value = "lawyerId", required = false) Long lawyerId,
            HttpServletRequest request) {

        System.out.println("API ENTRY: POST /api/audio/upload for user: " + userId + " (file: " + (file != null ? file.getOriginalFilename() : "null") + ") with lawyer: " + lawyerId);
        try {
            String rateLimitKey = (userId != null) ? userId.toString() : request.getRemoteAddr();
            if (!rateLimitService.tryConsumeAi(rateLimitKey)) {
                return ResponseEntity.status(429).body("{\"error\": \"Rate limit exceeded for AI video/audio processing. Please try again later.\"}");
            }

            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body("{\"error\": \"Audio file is missing or empty\"}");
            }

            // ... (rest of validation) ...
            long maxSize = 20 * 1024 * 1024; // 20MB
            if (file.getSize() > maxSize) {
                return ResponseEntity.badRequest()
                    .body("{\"error\": \"File size exceeds 20MB limit.\"}");
            }

            // Using the refactored, robust method from AudioProcessingService
            ClientAudio saved = audioService.processAndCreateCase(file, userId, caseTitle, lawyerId);
            
            // Convert to DTO using service helper to include caseTitle and masked audio
            ClientAudioDTO dto = audioService.convertToDTO(saved);
            
            System.out.println("Returning DTO with masked audio: " + 
                (dto.getMaskedTextAudioBase64() != null ? 
                    dto.getMaskedTextAudioBase64().length() + " characters (base64)" : "null"));
            
            return ResponseEntity.ok(dto);

        } catch (RuntimeException e) {
            System.err.println("Error processing audio: " + e.getMessage());
            e.printStackTrace();
            
            // Return user-friendly error message
            String errorMessage = e.getMessage();
            if (errorMessage != null && errorMessage.contains("Whisper")) {
                errorMessage = "Error processing audio with Whisper API. " +
                    "Please check: 1) Your internet connection, 2) Audio file format is supported, " +
                    "3) Audio file is not corrupted. Original error: " + e.getMessage();
            } else if (errorMessage != null && errorMessage.contains("timeout")) {
                errorMessage = "Audio processing timed out. The audio file might be too long. " +
                    "Please try with a shorter audio file (under 5 minutes).";
            }
            
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", errorMessage);
            return ResponseEntity.status(500).body(errorResponse);
        } catch (Exception e) {
            System.err.println("Unexpected error processing audio: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Unexpected error: " + (e.getMessage() != null ? e.getMessage() : "Unknown error"));
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @GetMapping("/all")
    public ResponseEntity<List<ClientAudioDTO>> getAllRecords() {
        com.legalconnect.lawyerbooking.security.UserPrincipal currentUser = authorizationService.getCurrentUser();
        String role = currentUser.getRole();
        Long userId = currentUser.getUserId();

        List<ClientAudioDTO> records;
        if ("admin".equalsIgnoreCase(role)) {
            records = audioService.getAllAudioForAdmin();
        } else if ("lawyer".equalsIgnoreCase(role)) {
            records = audioService.getAudioForLawyer(userId);
        } else if ("user".equalsIgnoreCase(role)) {
            records = audioService.getAudioForUser(userId);
        } else {
            return ResponseEntity.status(401).build();
        }
        
        System.out.println("Fetching all records for " + role + ", count: " + records.size());
        return ResponseEntity.ok(records);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ClientAudioDTO> getRecordById(@PathVariable("id") Long id) {
        ClientAudio record = repository.findById(id)
            .orElseThrow(() -> new RuntimeException("Record not found with id: " + id));
        
        // STRICT SECURITY: Validate access using AuthorizationService
        if (record.getCaseId() != null) {
            authorizationService.verifyCaseAccess(record.getCaseId());
        } else {
            // If audio is not linked to a case, check if the current user owns it
            com.legalconnect.lawyerbooking.security.UserPrincipal currentUser = authorizationService.getCurrentUser();
            if ("user".equalsIgnoreCase(currentUser.getRole()) && !record.getUserId().equals(currentUser.getUserId())) {
                throw new com.legalconnect.lawyerbooking.exception.UnauthorizedException("Access denied to this audio record");
            }
        }
        
        System.out.println("Fetching record ID: " + id + 
            ", English masked audio: " + (record.getMaskedTextAudio() != null ? 
                record.getMaskedTextAudio().length + " bytes" : "null") +
            ", Gujarati masked audio: " + (record.getMaskedGujaratiAudio() != null ? 
                record.getMaskedGujaratiAudio().length + " bytes" : "null"));
        
        ClientAudioDTO dto = audioService.convertToDTO(record);
        
        return ResponseEntity.ok(dto);
    }
}
