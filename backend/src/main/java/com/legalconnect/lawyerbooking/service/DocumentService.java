package com.legalconnect.lawyerbooking.service;

import com.legalconnect.lawyerbooking.dto.CaseDocumentDTO;
import com.legalconnect.lawyerbooking.entity.CaseDocument;
import com.legalconnect.lawyerbooking.repository.CaseDocumentRepository;
import com.legalconnect.lawyerbooking.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DocumentService {

    @Autowired
    private CaseDocumentRepository documentRepository;

    @Autowired
    private CaseAuditLogService auditLogService;

    private final Path fileStorageLocation;

    public DocumentService(@Value("${file.upload-dir:uploads/documents}") String uploadDir) {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    @Transactional
    public CaseDocumentDTO storeFile(MultipartFile file, Long caseId, Long uploaderId, String uploaderRole) {
        String originalFileName = org.springframework.util.StringUtils.cleanPath(file.getOriginalFilename());
        
        try {
            if(originalFileName.contains("..")) {
                throw new RuntimeException("Sorry! Filename contains invalid path sequence " + originalFileName);
            }

            String fileName = UUID.randomUUID().toString() + "_" + originalFileName;
            Path targetLocation = this.fileStorageLocation.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            CaseDocument document = new CaseDocument();
            document.setCaseId(caseId);
            document.setFileName(originalFileName);
            document.setStoredFilePath(targetLocation.toString());
            document.setMimeType(file.getContentType());
            document.setFileSize(file.getSize());
            document.setUploadedById(uploaderId);
            document.setUploadedByRole(uploaderRole);

            CaseDocument saved = documentRepository.save(document);

            // Audit Log
            auditLogService.logEvent(
                caseId, 
                "DOCUMENT_UPLOAD", 
                null, 
                null, 
                "Document uploaded: " + originalFileName, 
                uploaderId, 
                uploaderRole
            );

            return convertToDTO(saved);
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + originalFileName + ". Please try again!", ex);
        }
    }

    public CaseDocument getDocumentById(Long documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with id " + documentId));
    }

    public List<CaseDocumentDTO> getDocumentsByCaseId(Long caseId) {
        return documentRepository.findByCaseId(caseId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public Resource loadFileAsResource(String filePath) {
        try {
            Path filePathObj = Paths.get(filePath);
            Resource resource = new UrlResource(filePathObj.toUri());
            if(resource.exists()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("File not found " + filePath);
            }
        } catch (MalformedURLException ex) {
            throw new ResourceNotFoundException("File not found " + filePath, ex);
        }
    }

    private CaseDocumentDTO convertToDTO(CaseDocument doc) {
        CaseDocumentDTO dto = new CaseDocumentDTO();
        dto.setId(doc.getId());
        dto.setCaseId(doc.getCaseId());
        dto.setFileName(doc.getFileName());
        dto.setMimeType(doc.getMimeType());
        dto.setFileSize(doc.getFileSize());
        dto.setUploadedById(doc.getUploadedById());
        dto.setUploadedByRole(doc.getUploadedByRole());
        dto.setCreatedAt(doc.getCreatedAt());
        return dto;
    }
}
