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
    private String bio;
    private String consultationModes;
    private String headline;
    private Boolean verified;
    private String experienceTimeline;
    private String notableSuccesses;

    public LawyerProfileDTO() {}

    public LawyerProfileDTO(Long id, String fullName, Set<CaseType> specializations,
                            Integer yearsOfExperience, String languagesKnown, Double rating,
                            Integer completedCasesCount, String profilePhotoUrl, String availabilityInfo,
                            String barNumber, String email, String bio, String consultationModes, String headline, Boolean verified, String experienceTimeline, String notableSuccesses) {
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
        this.bio = bio;
        this.consultationModes = consultationModes;
        this.headline = headline;
        this.verified = verified;
        this.experienceTimeline = experienceTimeline;
        this.notableSuccesses = notableSuccesses;
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

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getConsultationModes() { return consultationModes; }
    public void setConsultationModes(String consultationModes) { this.consultationModes = consultationModes; }

    public String getHeadline() { return headline; }
    public void setHeadline(String headline) { this.headline = headline; }

    public Boolean isVerified() { return verified; }
    public void setVerified(Boolean verified) { this.verified = verified; }

    public String getExperienceTimeline() { return experienceTimeline; }
    public void setExperienceTimeline(String experienceTimeline) { this.experienceTimeline = experienceTimeline; }

    public String getNotableSuccesses() { return notableSuccesses; }
    public void setNotableSuccesses(String notableSuccesses) { this.notableSuccesses = notableSuccesses; }
}
