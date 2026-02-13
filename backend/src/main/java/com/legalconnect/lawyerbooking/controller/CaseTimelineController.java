package com.legalconnect.lawyerbooking.controller;

import com.legalconnect.lawyerbooking.dto.CaseAuditLogDTO;
import com.legalconnect.lawyerbooking.service.AuthorizationService;
import com.legalconnect.lawyerbooking.service.CaseAuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cases")
@CrossOrigin(origins = "*")
public class CaseTimelineController {

    @Autowired
    private CaseAuditLogService auditLogService;

    @Autowired
    private AuthorizationService authorizationService;

    @GetMapping("/{caseId}/timeline")
    public ResponseEntity<List<CaseAuditLogDTO>> getCaseTimeline(@PathVariable Long caseId) {
        // Verify same access rules as case access
        authorizationService.verifyCaseAccess(caseId);

        List<CaseAuditLogDTO> timeline = auditLogService.getTimelineForCase(caseId);
        return ResponseEntity.ok(timeline);
    }
}
