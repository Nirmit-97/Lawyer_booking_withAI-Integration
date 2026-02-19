package com.legalconnect.lawyerbooking.controller;

import com.legalconnect.lawyerbooking.dto.LawyerProfileDTO;
import com.legalconnect.lawyerbooking.entity.Lawyer;
import com.legalconnect.lawyerbooking.exception.ResourceNotFoundException;
import com.legalconnect.lawyerbooking.repository.LawyerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import com.legalconnect.lawyerbooking.dto.LawyerSearchCriteria;
import com.legalconnect.lawyerbooking.enums.CaseType;
import com.legalconnect.lawyerbooking.repository.LawyerSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/lawyers")
@CrossOrigin(origins = "*")
public class LawyerController {

    @Autowired
    private LawyerRepository lawyerRepository;

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(LawyerController.class);

    @Autowired
    private com.legalconnect.lawyerbooking.service.AuthorizationService authorizationService;

    @GetMapping("/{lawyerId}/profile")
    public ResponseEntity<LawyerProfileDTO> getLawyerProfile(@PathVariable("lawyerId") Long lawyerId) {
        Lawyer lawyer = lawyerRepository.findById(lawyerId)
                .orElseThrow(() -> new ResourceNotFoundException("Lawyer not found with ID: " + lawyerId));

        LawyerProfileDTO dto = new LawyerProfileDTO(
                lawyer.getId(),
                lawyer.getFullName(),
                lawyer.getSpecializations(),
                lawyer.getYearsOfExperience(),
                lawyer.getLanguagesKnown(),
                lawyer.getRating(),
                lawyer.getCompletedCasesCount(),
                lawyer.getProfilePhotoUrl(),
                lawyer.getAvailabilityInfo(),
                lawyer.getBarNumber(),
                lawyer.getEmail(),
                lawyer.getBio(),
                lawyer.getConsultationModes(),
                lawyer.getHeadline(),
                lawyer.isVerified(),
                lawyer.getExperienceTimeline(),
                lawyer.getNotableSuccesses()
        );

        return ResponseEntity.ok(dto);
    }

    @PutMapping("/{lawyerId}/profile")
    public ResponseEntity<LawyerProfileDTO> updateLawyerProfile(@PathVariable("lawyerId") Long lawyerId, @RequestBody LawyerProfileDTO profileDTO) {
        authorizationService.verifyLawyerAccess(lawyerId);

        Lawyer lawyer = lawyerRepository.findById(lawyerId)
                .orElseThrow(() -> new ResourceNotFoundException("Lawyer not found with ID: " + lawyerId));

        // Update fields
        if (profileDTO.getFullName() != null) lawyer.setFullName(profileDTO.getFullName());
        if (profileDTO.getSpecializations() != null) {
            lawyer.setSpecializations(profileDTO.getSpecializations());
        }

        if (profileDTO.getYearsOfExperience() != null) lawyer.setYearsOfExperience(profileDTO.getYearsOfExperience());
        if (profileDTO.getLanguagesKnown() != null) lawyer.setLanguagesKnown(profileDTO.getLanguagesKnown());
        if (profileDTO.getAvailabilityInfo() != null) lawyer.setAvailabilityInfo(profileDTO.getAvailabilityInfo());
        if (profileDTO.getEmail() != null) lawyer.setEmail(profileDTO.getEmail());
        if (profileDTO.getProfilePhotoUrl() != null) lawyer.setProfilePhotoUrl(profileDTO.getProfilePhotoUrl());
        if (profileDTO.getBarNumber() != null) lawyer.setBarNumber(profileDTO.getBarNumber());
        
        logger.info("Updating Profile for Lawyer ID: {}. Received Bio: '{}'", lawyerId, profileDTO.getBio());
        if (profileDTO.getBio() != null) lawyer.setBio(profileDTO.getBio());
        if (profileDTO.getConsultationModes() != null) lawyer.setConsultationModes(profileDTO.getConsultationModes());
        if (profileDTO.getHeadline() != null) lawyer.setHeadline(profileDTO.getHeadline());
        if (profileDTO.getExperienceTimeline() != null) lawyer.setExperienceTimeline(profileDTO.getExperienceTimeline());
        if (profileDTO.getNotableSuccesses() != null) lawyer.setNotableSuccesses(profileDTO.getNotableSuccesses());

        Lawyer savedLawyer = lawyerRepository.save(lawyer);

        LawyerProfileDTO responseDTO = new LawyerProfileDTO(
                savedLawyer.getId(),
                savedLawyer.getFullName(),
                savedLawyer.getSpecializations(),
                savedLawyer.getYearsOfExperience(),
                savedLawyer.getLanguagesKnown(),
                savedLawyer.getRating(),
                savedLawyer.getCompletedCasesCount(),
                savedLawyer.getProfilePhotoUrl(),
                savedLawyer.getAvailabilityInfo(),
                savedLawyer.getBarNumber(),
                savedLawyer.getEmail(),
                savedLawyer.getBio(),
                savedLawyer.getConsultationModes(),
                savedLawyer.getHeadline(),
                savedLawyer.isVerified(),
                savedLawyer.getExperienceTimeline(),
                savedLawyer.getNotableSuccesses()
        );

        return ResponseEntity.ok(responseDTO);
    }

    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchLawyers(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) CaseType specialization,
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false) Integer minExperience,
            @RequestParam(required = false) Integer minCompletedCases,
            @RequestParam(required = false) String availability,
            @RequestParam(required = false) Boolean verified,
            Pageable pageable) {

        LawyerSearchCriteria criteria = new LawyerSearchCriteria();
        criteria.setName(name);
        criteria.setSpecialization(specialization);
        criteria.setMinRating(minRating);
        criteria.setMinExperience(minExperience);
        criteria.setMinCompletedCases(minCompletedCases);
        criteria.setAvailability(availability);
        criteria.setVerified(verified);

        Specification<Lawyer> spec = LawyerSpecifications.withCriteria(criteria);
        Page<Lawyer> page = lawyerRepository.findAll(spec, pageable);

        java.util.List<LawyerProfileDTO> dtos = page.getContent().stream().map(lawyer -> new LawyerProfileDTO(
                lawyer.getId(),
                lawyer.getFullName(),
                lawyer.getSpecializations(),
                lawyer.getYearsOfExperience(),
                lawyer.getLanguagesKnown(),
                lawyer.getRating(),
                lawyer.getCompletedCasesCount(),
                lawyer.getProfilePhotoUrl(),
                lawyer.getAvailabilityInfo(),
                lawyer.getBarNumber(),
                lawyer.getEmail(),
                lawyer.getBio(),
                lawyer.getConsultationModes(),
                lawyer.getHeadline(),
                lawyer.isVerified(),
                lawyer.getExperienceTimeline(),
                lawyer.getNotableSuccesses()
        )).collect(java.util.stream.Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("lawyers", dtos);
        response.put("currentPage", page.getNumber());
        response.put("totalItems", page.getTotalElements());
        response.put("totalPages", page.getTotalPages());

        return ResponseEntity.ok(response);
    }
}
