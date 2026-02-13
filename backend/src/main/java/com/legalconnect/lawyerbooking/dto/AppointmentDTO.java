package com.legalconnect.lawyerbooking.dto;

import java.time.LocalDateTime;

public class AppointmentDTO {
    
    private Long id;
    private Long userId;
    private Long lawyerId;
    private String userFullName;
    private String lawyerFullName;
    private LocalDateTime appointmentDate;
    private Integer durationMinutes;
    private String status;
    private String meetingType;
    private String description;
    private String notes;
    private Long caseId;
    private String requestedByRole;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructors
    public AppointmentDTO() {
    }

    public AppointmentDTO(Long id, Long userId, Long lawyerId, LocalDateTime appointmentDate, 
                         Integer durationMinutes, String status, String meetingType) {
        this.id = id;
        this.userId = userId;
        this.lawyerId = lawyerId;
        this.appointmentDate = appointmentDate;
        this.durationMinutes = durationMinutes;
        this.status = status;
        this.meetingType = meetingType;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getLawyerId() {
        return lawyerId;
    }

    public void setLawyerId(Long lawyerId) {
        this.lawyerId = lawyerId;
    }

    public String getUserFullName() {
        return userFullName;
    }

    public void setUserFullName(String userFullName) {
        this.userFullName = userFullName;
    }

    public String getLawyerFullName() {
        return lawyerFullName;
    }

    public void setLawyerFullName(String lawyerFullName) {
        this.lawyerFullName = lawyerFullName;
    }

    public LocalDateTime getAppointmentDate() {
        return appointmentDate;
    }

    public void setAppointmentDate(LocalDateTime appointmentDate) {
        this.appointmentDate = appointmentDate;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMeetingType() {
        return meetingType;
    }

    public void setMeetingType(String meetingType) {
        this.meetingType = meetingType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Long getCaseId() {
        return caseId;
    }

    public void setCaseId(Long caseId) {
        this.caseId = caseId;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getRequestedByRole() {
        return requestedByRole;
    }

    public void setRequestedByRole(String requestedByRole) {
        this.requestedByRole = requestedByRole;
    }
}

