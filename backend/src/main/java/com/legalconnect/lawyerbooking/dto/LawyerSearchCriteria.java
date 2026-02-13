package com.legalconnect.lawyerbooking.dto;

import com.legalconnect.lawyerbooking.enums.CaseType;

public class LawyerSearchCriteria {
    private CaseType specialization;
    private Double minRating;
    private Integer minExperience;
    private Integer minCompletedCases;
    private String availability;
    private String name;
    private Boolean verified;

    // Getters and Setters
    public CaseType getSpecialization() { return specialization; }
    public void setSpecialization(CaseType specialization) { this.specialization = specialization; }

    public Double getMinRating() { return minRating; }
    public void setMinRating(Double minRating) { this.minRating = minRating; }

    public Integer getMinExperience() { return minExperience; }
    public void setMinExperience(Integer minExperience) { this.minExperience = minExperience; }

    public Integer getMinCompletedCases() { return minCompletedCases; }
    public void setMinCompletedCases(Integer minCompletedCases) { this.minCompletedCases = minCompletedCases; }

    public String getAvailability() { return availability; }
    public void setAvailability(String availability) { this.availability = availability; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Boolean getVerified() { return verified; }
    public void setVerified(Boolean verified) { this.verified = verified; }

    @Override
    public String toString() {
        return "LawyerSearchCriteria{" +
                "specialization='" + specialization + '\'' +
                ", minRating=" + minRating +
                ", minExperience=" + minExperience +
                ", minCompletedCases=" + minCompletedCases +
                ", availability='" + availability + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
