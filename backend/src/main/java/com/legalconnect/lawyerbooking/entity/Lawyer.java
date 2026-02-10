package com.legalconnect.lawyerbooking.entity;

import com.legalconnect.lawyerbooking.enums.CaseType;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "lawyers")
public class Lawyer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 100)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(length = 255)
    private String email;

    @Column(name = "full_name", length = 255)
    private String fullName;

    @Column(name = "bar_number", length = 100)
    private String barNumber;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "lawyer_specializations", joinColumns = @JoinColumn(name = "lawyer_id"))
    @Convert(converter = com.legalconnect.lawyerbooking.converter.CaseTypeConverter.class)
    @Column(name = "specialization")
    private Set<CaseType> specializations = new HashSet<>();

    @Column(name = "years_of_experience")
    private Integer yearsOfExperience;

    @Column(name = "languages_known", length = 255)
    private String languagesKnown;

    @Column(name = "rating")
    private Double rating;

    @Column(name = "upi_id")
    private String upiId;

    @Column(name = "completed_cases_count")
    private Integer completedCasesCount;

    @Column(name = "profile_photo_url", length = 500)
    private String profilePhotoUrl;

    @Column(name = "availability_info", length = 500)
    private String availabilityInfo;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getBarNumber() { return barNumber; }
    public void setBarNumber(String barNumber) { this.barNumber = barNumber; }

    public Set<CaseType> getSpecializations() {
        return specializations;
    }

    public void setSpecializations(Set<CaseType> specializations) {
        this.specializations = specializations;
    }

    public Integer getYearsOfExperience() { return yearsOfExperience; }
    public void setYearsOfExperience(Integer yearsOfExperience) { this.yearsOfExperience = yearsOfExperience; }

    public String getLanguagesKnown() { return languagesKnown; }
    public void setLanguagesKnown(String languagesKnown) { this.languagesKnown = languagesKnown; }

    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }

    public String getUpiId() { return upiId; }
    public void setUpiId(String upiId) { this.upiId = upiId; }

    public Integer getCompletedCasesCount() { return completedCasesCount; }
    public void setCompletedCasesCount(Integer completedCasesCount) { this.completedCasesCount = completedCasesCount; }

    public String getProfilePhotoUrl() { return profilePhotoUrl; }
    public void setProfilePhotoUrl(String profilePhotoUrl) { this.profilePhotoUrl = profilePhotoUrl; }

    public String getAvailabilityInfo() { return availabilityInfo; }
    public void setAvailabilityInfo(String availabilityInfo) { this.availabilityInfo = availabilityInfo; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

