package com.legalconnect.lawyerbooking.service;

import com.legalconnect.lawyerbooking.dto.LawyerSearchCriteria;
import com.legalconnect.lawyerbooking.entity.Lawyer;
import com.legalconnect.lawyerbooking.enums.CaseType;
import com.legalconnect.lawyerbooking.repository.LawyerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import com.legalconnect.lawyerbooking.dto.LawyerDTO;
import com.legalconnect.lawyerbooking.dto.LawyerSearchResponse;

@Service
public class LawyerService {

    @Autowired
    private LawyerRepository lawyerRepository;

    /**
     * Search lawyers with caching and pagination
     */
    public LawyerSearchResponse searchLawyers(LawyerSearchCriteria criteria, Pageable pageable) {
        Page<Lawyer> page = lawyerRepository.findAll((Specification<Lawyer>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (criteria.getName() != null && !criteria.getName().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("fullName")), "%" + criteria.getName().toLowerCase() + "%"));
            }

            if (criteria.getSpecialization() != null) {
                // Check if the set of specializations contains the requested enum
                predicates.add(cb.isMember(criteria.getSpecialization(), root.get("specializations")));
            }

            if (criteria.getMinRating() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("rating"), criteria.getMinRating()));
            }

            if (criteria.getMinExperience() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("yearsOfExperience"), criteria.getMinExperience()));
            }

            if (criteria.getMinCompletedCases() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("completedCasesCount"), criteria.getMinCompletedCases()));
            }

            if (criteria.getAvailability() != null && !criteria.getAvailability().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("availabilityInfo")), "%" + criteria.getAvailability().toLowerCase() + "%"));
            }

            if (criteria.getVerified() != null) {
                predicates.add(cb.equal(root.get("verified"), criteria.getVerified()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        }, pageable);
        
        List<LawyerDTO> dtos = page.getContent().stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
        
        return new LawyerSearchResponse(
            dtos,
            page.getNumber(),
            page.getTotalPages(),
            page.getTotalElements(),
            page.getSize()
        );
    }
    
    public LawyerDTO getLawyerProfile(Long lawyerId) {
        Lawyer lawyer = lawyerRepository.findById(lawyerId)
            .orElseThrow(() -> new RuntimeException("Lawyer not found with id: " + lawyerId));
        return convertToDTO(lawyer);
    }
    
    /**
     * Update lawyer profile
     */
    public LawyerDTO updateLawyerProfile(Long lawyerId, LawyerDTO updateData) {
        Lawyer lawyer = lawyerRepository.findById(lawyerId)
            .orElseThrow(() -> new RuntimeException("Lawyer not found with id: " + lawyerId));
        
        if (updateData.getFullName() != null) {
            lawyer.setFullName(updateData.getFullName());
        }
        if (updateData.getSpecializations() != null) {
            lawyer.setSpecializations(updateData.getSpecializations());
        }
        if (updateData.getYearsOfExperience() != null) {
            lawyer.setYearsOfExperience(updateData.getYearsOfExperience());
        }
        if (updateData.getAvailabilityInfo() != null) {
            lawyer.setAvailabilityInfo(updateData.getAvailabilityInfo());
        }
        if (updateData.getLanguagesKnown() != null) {
            lawyer.setLanguagesKnown(updateData.getLanguagesKnown());
        }
        if (updateData.getProfilePhotoUrl() != null) {
            lawyer.setProfilePhotoUrl(updateData.getProfilePhotoUrl());
        }
        if (updateData.getBio() != null) {
            lawyer.setBio(updateData.getBio());
        }
        if (updateData.getConsultationModes() != null) {
            lawyer.setConsultationModes(updateData.getConsultationModes());
        }
        if (updateData.getNotableSuccesses() != null) {
            lawyer.setNotableSuccesses(updateData.getNotableSuccesses());
        }
        
        Lawyer savedLawyer = lawyerRepository.save(lawyer);
        return convertToDTO(savedLawyer);
    }
    
    public void updateLawyerRating(Long lawyerId, Double newRating) {
        Lawyer lawyer = lawyerRepository.findById(lawyerId)
            .orElseThrow(() -> new RuntimeException("Lawyer not found with id: " + lawyerId));
        
        lawyer.setRating(newRating);
        lawyerRepository.save(lawyer);
    }
    
    private LawyerDTO convertToDTO(Lawyer lawyer) {
        LawyerDTO dto = new LawyerDTO();
        dto.setId(lawyer.getId());
        dto.setFullName(lawyer.getFullName());
        dto.setSpecializations(lawyer.getSpecializations());
        dto.setYearsOfExperience(lawyer.getYearsOfExperience());
        dto.setRating(lawyer.getRating());
        dto.setCompletedCasesCount(lawyer.getCompletedCasesCount());
        dto.setAvailabilityInfo(lawyer.getAvailabilityInfo());
        dto.setEmail(lawyer.getEmail());
        dto.setBarNumber(lawyer.getBarNumber());
        dto.setLanguagesKnown(lawyer.getLanguagesKnown());
        dto.setProfilePhotoUrl(lawyer.getProfilePhotoUrl());
        dto.setVerified(lawyer.isVerified());
        dto.setBio(lawyer.getBio());
        dto.setConsultationModes(lawyer.getConsultationModes());
        dto.setNotableSuccesses(lawyer.getNotableSuccesses());
        return dto;
    }
}
