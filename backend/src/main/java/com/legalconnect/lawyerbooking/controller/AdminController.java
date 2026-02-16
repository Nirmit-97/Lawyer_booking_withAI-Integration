package com.legalconnect.lawyerbooking.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.legalconnect.lawyerbooking.dto.LoginRequest;
import com.legalconnect.lawyerbooking.dto.LoginResponse;
import com.legalconnect.lawyerbooking.repository.AdminRepository;
import com.legalconnect.lawyerbooking.repository.UserRepository;
import com.legalconnect.lawyerbooking.repository.LawyerRepository;
import com.legalconnect.lawyerbooking.repository.CaseRepository;
import com.legalconnect.lawyerbooking.entity.Admin;
import com.legalconnect.lawyerbooking.entity.User;
import com.legalconnect.lawyerbooking.entity.Lawyer;
import com.legalconnect.lawyerbooking.entity.Case;
import com.legalconnect.lawyerbooking.service.PasswordService;
import com.legalconnect.lawyerbooking.dto.CaseDTO;
import com.legalconnect.lawyerbooking.util.JwtUtil;
import com.legalconnect.lawyerbooking.enums.CaseStatus;
import com.legalconnect.lawyerbooking.enums.CaseType;
import com.legalconnect.lawyerbooking.entity.SystemSetting;
import com.legalconnect.lawyerbooking.repository.SystemSettingRepository;

import java.util.Optional;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LawyerRepository lawyerRepository;

    @Autowired
    private SystemSettingRepository systemSettingRepository;

    @Autowired
    private com.legalconnect.lawyerbooking.service.CaseService caseService;

    @Autowired
    private CaseRepository caseRepository;

    @Autowired
    private PasswordService passwordService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private com.legalconnect.lawyerbooking.service.AuditLogService auditLogService;

    // Admin Login
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> adminLogin(@RequestBody LoginRequest request) {
        try {
            System.out.println("Admin login attempt - Username: " + request.getUsername());

            Optional<Admin> adminOpt = adminRepository.findByUsername(request.getUsername());

            if (adminOpt.isEmpty()) {
                return ResponseEntity.status(401)
                    .body(new LoginResponse(false, "Invalid username or password"));
            }

            Admin admin = adminOpt.get();

            boolean passwordValid;
            if (passwordService.isHashed(admin.getPassword())) {
                passwordValid = passwordService.verifyPassword(request.getPassword(), admin.getPassword());
            } else {
                passwordValid = admin.getPassword().equals(request.getPassword());
                if (passwordValid) {
                    admin.setPassword(passwordService.hashPassword(request.getPassword()));
                    adminRepository.save(admin);
                }
            }

            if (passwordValid) {
                String token = jwtUtil.generateToken(admin.getId(), admin.getUsername(), com.legalconnect.lawyerbooking.enums.Role.ADMIN);

                LoginResponse response = new LoginResponse(true, "Login successful");
                response.setUserType("admin");
                response.setUsername(admin.getUsername());
                response.setFullName(admin.getFullName());
                response.setId(admin.getId());
                response.setToken(token);
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(401)
                    .body(new LoginResponse(false, "Invalid username or password"));
            }
        } catch (Exception e) {
            System.err.println("Error in admin login: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500)
                .body(new LoginResponse(false, "Internal server error: " + e.getMessage()));
        }
    }

    // Dashboard Statistics
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        try {
            Map<String, Object> stats = new HashMap<>();
            
            long totalUsers = userRepository.count();
            long totalLawyers = lawyerRepository.count();
            long totalCases = caseRepository.countByDeletedFalse();
            
            // Calculate pending audits (unverified lawyers + published cases)
            long unverifiedLawyers = lawyerRepository.countByVerifiedFalse();
            long publishedCases = caseRepository.countByCaseStatusAndDeletedFalse(CaseStatus.PUBLISHED);
            long pendingAudits = unverifiedLawyers + publishedCases;
            
            stats.put("totalUsers", totalUsers);
            stats.put("totalLawyers", totalLawyers);
            stats.put("totalCases", totalCases);
            stats.put("pendingAudits", pendingAudits);
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            System.err.println("Error fetching stats: " + e.getMessage());
            return ResponseEntity.status(500).body(null);
        }
    }

    // Get All Users
    @GetMapping("/users")
    public ResponseEntity<Page<User>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<User> users = userRepository.findAll(pageable);
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            System.err.println("Error fetching users: " + e.getMessage());
            return ResponseEntity.status(500).body(null);
        }
    }

    // Get User by ID
    @GetMapping("/users/{id}")
    public ResponseEntity<User> getUserById(@PathVariable("id") Long id) {
        try {
            Optional<User> user = userRepository.findById(id);
            return user.map(ResponseEntity::ok)
                      .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    // Update User
    @PutMapping("/users/{id}")
    public ResponseEntity<User> updateUser(jakarta.servlet.http.HttpServletRequest request, @PathVariable("id") Long id, @RequestBody User userUpdate) {
        try {
            Optional<User> userOpt = userRepository.findById(id);
            if (userOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            User user = userOpt.get();
            if (userUpdate.getFullName() != null) user.setFullName(userUpdate.getFullName());
            if (userUpdate.getEmail() != null) user.setEmail(userUpdate.getEmail());
            
            User savedUser = userRepository.save(user);
            Long adminId = extractAdminId(request);
            auditLogService.logAction(adminId, "System", "UPDATE", "USER", savedUser.getId(), "Updated user details for: " + savedUser.getFullName());
            return ResponseEntity.ok(savedUser);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    // Delete User
    @DeleteMapping("/users/{id}")
    public ResponseEntity<Map<String, String>> deleteUser(jakarta.servlet.http.HttpServletRequest request, @PathVariable("id") Long id) {
        try {
            if (!userRepository.existsById(id)) {
                return ResponseEntity.notFound().build();
            }
            userRepository.deleteById(id);
            Long adminId = extractAdminId(request);
            auditLogService.logAction(adminId, "System", "DELETE", "USER", id, "Deleted user account with ID: " + id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "User deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    // Get All Lawyers
    @GetMapping("/lawyers")
    public ResponseEntity<Page<Lawyer>> getAllLawyers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Lawyer> lawyers = lawyerRepository.findAll(pageable);
            return ResponseEntity.ok(lawyers);
        } catch (Exception e) {
            System.err.println("Error fetching lawyers: " + e.getMessage());
            return ResponseEntity.status(500).body(null);
        }
    }

    // Get Lawyer by ID
    @GetMapping("/lawyers/{id}")
    public ResponseEntity<Lawyer> getLawyerById(@PathVariable("id") Long id) {
        try {
            Optional<Lawyer> lawyer = lawyerRepository.findById(id);
            return lawyer.map(ResponseEntity::ok)
                        .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    // Update Lawyer
    @PutMapping("/lawyers/{id}")
    public ResponseEntity<Lawyer> updateLawyer(jakarta.servlet.http.HttpServletRequest request, @PathVariable("id") Long id, @RequestBody Lawyer lawyerUpdate) {
        try {
            Optional<Lawyer> lawyerOpt = lawyerRepository.findById(id);
            if (lawyerOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Lawyer lawyer = lawyerOpt.get();
            if (lawyerUpdate.getFullName() != null) lawyer.setFullName(lawyerUpdate.getFullName());
            if (lawyerUpdate.getEmail() != null) lawyer.setEmail(lawyerUpdate.getEmail());
            
            // Handle specialization update 
            if (lawyerUpdate.getSpecializations() != null && !lawyerUpdate.getSpecializations().isEmpty()) {
                lawyer.setSpecializations(lawyerUpdate.getSpecializations());
            }
            
            if (lawyerUpdate.getBarNumber() != null) lawyer.setBarNumber(lawyerUpdate.getBarNumber());
            
            Lawyer savedLawyer = lawyerRepository.save(lawyer);
            Long adminId = extractAdminId(request);
            auditLogService.logAction(adminId, "System", "UPDATE", "LAWYER", savedLawyer.getId(), "Updated lawyer profile for: " + savedLawyer.getFullName());
            return ResponseEntity.ok(savedLawyer);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    // Delete Lawyer
    @DeleteMapping("/lawyers/{id}")
    public ResponseEntity<Map<String, String>> deleteLawyer(jakarta.servlet.http.HttpServletRequest request, @PathVariable("id") Long id) {
        try {
            if (!lawyerRepository.existsById(id)) {
                return ResponseEntity.notFound().build();
            }
            lawyerRepository.deleteById(id);
            Long adminId = extractAdminId(request);
            auditLogService.logAction(adminId, "System", "DELETE", "LAWYER", id, "Deleted lawyer account with ID: " + id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Lawyer deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    // Get All Cases
    @GetMapping("/cases")
    public ResponseEntity<Page<CaseDTO>> getAllCases(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            List<CaseDTO> casesList = caseService.getAllCasesForAdmin();
            // Since getAllCasesForAdmin returns a List, we'll wrap it in a Page if needed, 
            // but the original logic was findAll(pageable). Let's keep it simple for now 
            // or adapt the service to accept pageable.
            int start = (int) pageable.getOffset();
            if (start > casesList.size()) {
                start = casesList.size();
            }
            int end = Math.min((start + pageable.getPageSize()), casesList.size());
            
            Page<CaseDTO> cases = new org.springframework.data.domain.PageImpl<>(
                casesList.subList(start, end), pageable, casesList.size());
            return ResponseEntity.ok(cases);
        } catch (Exception e) {
            System.err.println("Error fetching cases: " + e.getMessage());
            return ResponseEntity.status(500).body(null);
        }
    }

    // Get Case by ID
    @GetMapping("/cases/{id}")
    public ResponseEntity<CaseDTO> getCaseById(@PathVariable("id") Long id) {
        try {
            return caseRepository.findById(id)
                            .map(caseService::convertToDTO)
                            .map(ResponseEntity::ok)
                            .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    // Update Case
    @PutMapping("/cases/{id}")
    public ResponseEntity<CaseDTO> updateCase(jakarta.servlet.http.HttpServletRequest request, @PathVariable("id") Long id, @RequestBody Case caseUpdate) {
        try {
            Optional<Case> caseOpt = caseRepository.findById(id);
            if (caseOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Case caseEntity = caseOpt.get();
            if (caseUpdate.getCaseTitle() != null) caseEntity.setCaseTitle(caseUpdate.getCaseTitle());
            if (caseUpdate.getCaseStatus() != null) caseEntity.setCaseStatus(caseUpdate.getCaseStatus());
            
            // Map string update to Enum
            if (caseUpdate.getCaseType() != null) {
                caseEntity.setCaseType(caseUpdate.getCaseType());
            }
            if (caseUpdate.getDescription() != null) caseEntity.setDescription(caseUpdate.getDescription());
            
            Case savedCase = caseRepository.save(caseEntity);
            Long adminId = extractAdminId(request);
            auditLogService.logAction(adminId, "System", "UPDATE", "CASE", savedCase.getId(), "Updated case details for: " + savedCase.getCaseTitle());
            return ResponseEntity.ok(caseService.convertToDTO(savedCase));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    // Reassign Case
    @PutMapping("/cases/{id}/reassign")
    public ResponseEntity<CaseDTO> reassignCase(jakarta.servlet.http.HttpServletRequest request, @PathVariable("id") Long id, @RequestBody Map<String, Long> requestData) {
        try {
            Optional<Case> caseOpt = caseRepository.findById(id);
            if (caseOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Case caseEntity = caseOpt.get();
            Long newLawyerId = requestData.get("lawyerId");
            
            if (newLawyerId != null && lawyerRepository.existsById(newLawyerId)) {
                caseEntity.setLawyerId(newLawyerId);
                Case savedCase = caseRepository.save(caseEntity);
                Long adminId = extractAdminId(request);
                auditLogService.logAction(adminId, "System", "REASSIGN", "CASE", savedCase.getId(), "Reassigned case to lawyer ID: " + newLawyerId);
                return ResponseEntity.ok(caseService.convertToDTO(savedCase));
            } else {
                return ResponseEntity.badRequest().body(null);
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    // Verify Lawyer
    @PutMapping("/lawyers/{id}/verify")
    public ResponseEntity<Lawyer> verifyLawyer(jakarta.servlet.http.HttpServletRequest request, @PathVariable("id") Long id, @RequestParam(defaultValue = "true") boolean verified) {
        try {
            Optional<Lawyer> lawyerOpt = lawyerRepository.findById(id);
            if (lawyerOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Lawyer lawyer = lawyerOpt.get();
            lawyer.setVerified(verified);
            Lawyer savedLawyer = lawyerRepository.save(lawyer);
            Long adminId = extractAdminId(request);
            auditLogService.logAction(adminId, "System", "VERIFY", "LAWYER", savedLawyer.getId(), "Set lawyer verification to: " + verified);
            return ResponseEntity.ok(savedLawyer);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    // Delete Case
    @DeleteMapping("/cases/{id}")
    public ResponseEntity<Map<String, String>> deleteCase(jakarta.servlet.http.HttpServletRequest request, @PathVariable("id") Long id) {
        try {
            if (!caseRepository.existsById(id)) {
                return ResponseEntity.notFound().build();
            }
            // Use service for soft delete and broadcast
            caseService.deleteCase(id);
            Long adminId = extractAdminId(request);
            auditLogService.logAction(adminId, "System", "DELETE", "CASE", id, "Soft-deleted case with ID: " + id);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Case deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Error deleting case: " + e.getMessage());
            return ResponseEntity.status(500).body(null);
        }
    }

    // Get Audit Logs
    @GetMapping("/audit-logs")
    public ResponseEntity<Page<com.legalconnect.lawyerbooking.entity.AuditLog>> getAuditLogs(
            jakarta.servlet.http.HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Page<com.legalconnect.lawyerbooking.entity.AuditLog> logs = auditLogService.getAuditLogs(page, size);
            
            // Baseline log if system is empty
            if (logs.isEmpty() && page == 0) {
                Long adminId = extractAdminId(request);
                auditLogService.logAction(adminId, "System", "STARTUP", "SYSTEM", 0L, "Audit logging system verified and active");
                logs = auditLogService.getAuditLogs(page, size);
            }
            
            return ResponseEntity.ok(logs);
        } catch (Exception e) {
            System.err.println("Error fetching audit logs: " + e.getMessage());
            return ResponseEntity.status(500).body(null);
        }
    }

    // Get Analytics Data
    @GetMapping("/analytics")
    public ResponseEntity<Map<String, Object>> getAnalytics() {
        try {
            Map<String, Object> analytics = new HashMap<>();
            
            // User growth (mock data for now - can be enhanced with actual monthly counts)
            analytics.put("userGrowth", List.of(45, 52, 68, 85, 102, 120));
            
            // Case distribution by type
            Map<String, Long> caseDistribution = new HashMap<>();
            caseDistribution.put("CRIMINAL", caseRepository.countByCaseTypeAndDeletedFalse(CaseType.CRIMINAL));
            caseDistribution.put("CIVIL", caseRepository.countByCaseTypeAndDeletedFalse(CaseType.CIVIL));
            caseDistribution.put("FAMILY", caseRepository.countByCaseTypeAndDeletedFalse(CaseType.FAMILY));
            caseDistribution.put("CORPORATE", caseRepository.countByCaseTypeAndDeletedFalse(CaseType.CORPORATE));
            caseDistribution.put("OTHER", caseRepository.countByCaseTypeAndDeletedFalse(CaseType.OTHER));
            analytics.put("caseDistribution", caseDistribution);
            
            // Case status breakdown
            Map<String, Long> statusBreakdown = new HashMap<>();
            statusBreakdown.put("PUBLISHED", caseRepository.countByCaseStatusAndDeletedFalse(CaseStatus.PUBLISHED));
            statusBreakdown.put("PENDING_APPROVAL", caseRepository.countByCaseStatusAndDeletedFalse(CaseStatus.PENDING_APPROVAL));
            statusBreakdown.put("IN_PROGRESS", caseRepository.countByCaseStatusAndDeletedFalse(CaseStatus.IN_PROGRESS));
            statusBreakdown.put("CLOSED", caseRepository.countByCaseStatusAndDeletedFalse(CaseStatus.CLOSED));
            analytics.put("caseStatus", statusBreakdown);
            
            return ResponseEntity.ok(analytics);
        } catch (Exception e) {
            System.err.println("Error fetching analytics: " + e.getMessage());
            return ResponseEntity.status(500).body(null);
        }
    }
        // -- System Settings --

    @GetMapping("/settings")
    public ResponseEntity<List<SystemSetting>> getSystemSettings() {
        try {
            List<SystemSetting> settings = systemSettingRepository.findAll();
            if (settings.isEmpty()) {
                // Initialize default settings if none exist
                initializeDefaultSettings();
                settings = systemSettingRepository.findAll();
            }
            return ResponseEntity.ok(settings);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    @PostMapping("/settings")
    public ResponseEntity<SystemSetting> updateSetting(@RequestBody SystemSetting setting) {
        try {
            Optional<SystemSetting> existing = systemSettingRepository.findBySettingKey(setting.getSettingKey());
            if (existing.isPresent()) {
                SystemSetting toUpdate = existing.get();
                toUpdate.setSettingValue(setting.getSettingValue());
                return ResponseEntity.ok(systemSettingRepository.save(toUpdate));
            }
            return ResponseEntity.ok(systemSettingRepository.save(setting));
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    private void initializeDefaultSettings() {
        systemSettingRepository.save(new SystemSetting(null, "maintenance_mode", "false", "Enable/Disable system-wide maintenance mode", "SYSTEM"));
        systemSettingRepository.save(new SystemSetting(null, "ai_provider", "openai", "Active AI model provider (openai/internal)", "AI"));
        systemSettingRepository.save(new SystemSetting(null, "whisper_model", "whisper-1", "OpenAI Whisper model version", "AI"));
        systemSettingRepository.save(new SystemSetting(null, "auto_classification", "true", "Enable automatic case classification", "AI"));
    }

    private Long extractAdminId(jakarta.servlet.http.HttpServletRequest request) {
        try {
            // First try request attribute set by JwtAuthenticationFilter
            Object attrId = request.getAttribute("userId");
            if (attrId instanceof Long) return (Long) attrId;
            if (attrId instanceof Number) return ((Number) attrId).longValue();
            
            // Fallback to manual extraction if attribute not set
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                Long id = jwtUtil.extractUserId(token);
                if (id != null) return id;
            }
        } catch (Exception e) {
            System.err.println("Error extracting admin ID: " + e.getMessage());
        }
        return 0L; // Final fallback, NEVER return null
    }
}
