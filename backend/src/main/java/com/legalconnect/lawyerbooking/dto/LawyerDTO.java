package com.legalconnect.lawyerbooking.dto;

import com.legalconnect.lawyerbooking.enums.CaseType;
import java.util.Set;

/**
 * Data Transfer Object for Lawyer information.
 * 
 * SECURITY: This DTO explicitly excludes sensitive fields like password.
 * Used for public-facing APIs and search results.
 */
public class LawyerDTO {
    private Long id;
    private String fullName;
    private Set<CaseType> specializations;
    private Integer yearsOfExperience;
    private Double rating;
    private Integer completedCasesCount;
    private Integer totalCasesCount;
    private String availabilityInfo;
    private String accountStatus;
    private String email;
    private String barNumber;
    private String languagesKnown;
    private String profilePhotoUrl;
    
    // Constructors
    public LawyerDTO() {}
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getFullName() {
        return fullName;
    }
    
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
    
    public Set<CaseType> getSpecializations() {
        return specializations;
    }
    
    public void setSpecializations(Set<CaseType> specializations) {
        this.specializations = specializations;
    }
    
    public Integer getYearsOfExperience() {
        return yearsOfExperience;
    }
    
    public void setYearsOfExperience(Integer yearsOfExperience) {
        this.yearsOfExperience = yearsOfExperience;
    }
    
    public Double getRating() {
        return rating;
    }
    
    public void setRating(Double rating) {
        this.rating = rating;
    }
    
    public Integer getCompletedCasesCount() {
        return completedCasesCount;
    }
    
    public void setCompletedCasesCount(Integer completedCasesCount) {
        this.completedCasesCount = completedCasesCount;
    }
    
    public Integer getTotalCasesCount() {
        return totalCasesCount;
    }
    
    public void setTotalCasesCount(Integer totalCasesCount) {
        this.totalCasesCount = totalCasesCount;
    }
    
    public String getAvailabilityInfo() {
        return availabilityInfo;
    }
    
    public void setAvailabilityInfo(String availabilityInfo) {
        this.availabilityInfo = availabilityInfo;
    }
    
    public String getAccountStatus() {
        return accountStatus;
    }
    
    public void setAccountStatus(String accountStatus) {
        this.accountStatus = accountStatus;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getBarNumber() {
        return barNumber;
    }
    
    public void setBarNumber(String barNumber) {
        this.barNumber = barNumber;
    }
    
    public String getLanguagesKnown() {
        return languagesKnown;
    }
    
    public void setLanguagesKnown(String languagesKnown) {
        this.languagesKnown = languagesKnown;
    }
    
    public String getProfilePhotoUrl() {
        return profilePhotoUrl;
    }
    
    public void setProfilePhotoUrl(String profilePhotoUrl) {
        this.profilePhotoUrl = profilePhotoUrl;
    }
}
