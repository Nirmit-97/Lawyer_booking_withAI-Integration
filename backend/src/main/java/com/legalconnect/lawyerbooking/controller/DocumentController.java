package com.legalconnect.lawyerbooking.controller;

import com.legalconnect.lawyerbooking.dto.CaseDocumentDTO;
import com.legalconnect.lawyerbooking.entity.CaseDocument;
import com.legalconnect.lawyerbooking.service.AuthorizationService;
import com.legalconnect.lawyerbooking.service.DocumentService;
import com.legalconnect.lawyerbooking.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
@CrossOrigin(origins = "*")
public class DocumentController {

    @Autowired
    private DocumentService documentService;

    @Autowired
    private AuthorizationService authorizationService;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("caseId") Long caseId) {
        
        // 1. Verify access to the case
        authorizationService.verifyCaseAccess(caseId);

        // 2. Get current user
        UserPrincipal currentUser = authorizationService.getCurrentUser();

        try {
            CaseDocumentDTO document = documentService.storeFile(
                file, 
                caseId, 
                currentUser.getUserId(), 
                currentUser.getRole()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(document);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Could not upload file: " + e.getMessage());
        }
    }

    @GetMapping("/case/{caseId}")
    public ResponseEntity<List<CaseDocumentDTO>> getDocumentsByCase(@PathVariable("caseId") Long caseId) {
        // 1. Verify access
        authorizationService.verifyCaseAccess(caseId);

        // 2. Fetch documents
        List<CaseDocumentDTO> documents = documentService.getDocumentsByCaseId(caseId);
        return ResponseEntity.ok(documents);
    }

    @GetMapping("/{documentId}/download")
    public ResponseEntity<Resource> downloadDocument(@PathVariable("documentId") Long documentId) {
        // 1. Get document metadata
        CaseDocument document = documentService.getDocumentById(documentId);

        // 2. Verify access to the associated case
        authorizationService.verifyCaseAccess(document.getCaseId());

        // 3. Load resource
        Resource resource = documentService.loadFileAsResource(document.getStoredFilePath());

        // 4. Determine content type
        String contentType = "application/octet-stream";
        if (document.getMimeType() != null) {
            contentType = document.getMimeType();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + document.getFileName() + "\"")
                .body(resource);
    }
}
