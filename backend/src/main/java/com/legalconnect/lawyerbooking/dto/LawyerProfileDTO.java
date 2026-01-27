package com.legalconnect.lawyerbooking.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.legalconnect.lawyerbooking.enums.CaseType;
import java.util.Set;

public class LawyerProfileDTO {
    private Long id;
    private String fullName;
    
    @JsonDeserialize(using = SpecializationsDeserializer.class)
    private Set<CaseType> specializations;
    private Integer yearsOfExperience;
    private String languagesKnown;
    private Double rating;
    private Integer completedCasesCount;
    private String profilePhotoUrl;
    private String availabilityInfo;
    private String barNumber;
    private String email;

    public LawyerProfileDTO() {}

    public LawyerProfileDTO(Long id, String fullName, Set<CaseType> specializations,
                            Integer yearsOfExperience, String languagesKnown, Double rating,
                            Integer completedCasesCount, String profilePhotoUrl, String availabilityInfo,
                            String barNumber, String email) {
        this.id = id;
        this.fullName = fullName;
        this.specializations = specializations;
        this.yearsOfExperience = yearsOfExperience;
        this.languagesKnown = languagesKnown;
        this.rating = rating;
        this.completedCasesCount = completedCasesCount;
        this.profilePhotoUrl = profilePhotoUrl;
        this.availabilityInfo = availabilityInfo;
        this.barNumber = barNumber;
        this.email = email;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public Set<CaseType> getSpecializations() {
        return specializations;
    }

    public void setSpecializations(Set<CaseType> specializations) {
        this.specializations = specializations;
    }

    // Alias for frontend compatibility (some parts expect a singular string)
    public String getSpecialization() {
        if (specializations == null || specializations.isEmpty()) return "General Practice";
        return specializations.stream()
                .map(Enum::name)
                .collect(java.util.stream.Collectors.joining(", "));
    }

    public Integer getYearsOfExperience() { return yearsOfExperience; }
    public void setYearsOfExperience(Integer yearsOfExperience) { this.yearsOfExperience = yearsOfExperience; }

    public String getLanguagesKnown() { return languagesKnown; }
    public void setLanguagesKnown(String languagesKnown) { this.languagesKnown = languagesKnown; }

    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }

    public Integer getCompletedCasesCount() { return completedCasesCount; }
    public void setCompletedCasesCount(Integer completedCasesCount) { this.completedCasesCount = completedCasesCount; }

    public String getProfilePhotoUrl() { return profilePhotoUrl; }
    public void setProfilePhotoUrl(String profilePhotoUrl) { this.profilePhotoUrl = profilePhotoUrl; }

    public String getAvailabilityInfo() { return availabilityInfo; }
    public void setAvailabilityInfo(String availabilityInfo) { this.availabilityInfo = availabilityInfo; }

    public String getBarNumber() { return barNumber; }
    public void setBarNumber(String barNumber) { this.barNumber = barNumber; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
