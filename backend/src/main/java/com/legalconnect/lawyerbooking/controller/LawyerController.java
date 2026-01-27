package com.legalconnect.lawyerbooking.controller;

import com.legalconnect.lawyerbooking.dto.LawyerProfileDTO;
import com.legalconnect.lawyerbooking.entity.Lawyer;
import com.legalconnect.lawyerbooking.exception.ResourceNotFoundException;
import com.legalconnect.lawyerbooking.repository.LawyerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/lawyers")
@CrossOrigin(origins = "*")
public class LawyerController {

    @Autowired
    private LawyerRepository lawyerRepository;

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
                lawyer.getEmail()
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
                savedLawyer.getEmail()
        );

        return ResponseEntity.ok(responseDTO);
    }
}
