package com.legalconnect.lawyerbooking.dto;

import com.legalconnect.lawyerbooking.enums.CaseType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Set;

public class LawyerRegistrationRequest {
    
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 100, message = "Username must be between 3 and 100 characters")
    private String username;
    
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    private String password;
    
    @NotBlank(message = "Full name is required")
    @Size(max = 255, message = "Full name must not exceed 255 characters")
    private String fullName;
    
    @Email(message = "Email should be valid")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;
    
    @NotBlank(message = "Bar number is required")
    @Size(max = 100, message = "Bar number must not exceed 100 characters")
    private String barNumber;
    
    private Set<CaseType> specializations;

    public LawyerRegistrationRequest() {}

    public LawyerRegistrationRequest(String username, String password, String fullName, String email, String barNumber, Set<CaseType> specializations) {
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.email = email;
        this.barNumber = barNumber;
        this.specializations = specializations;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
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

    public Set<CaseType> getSpecializations() {
        return specializations;
    }

    public void setSpecializations(Set<CaseType> specializations) {
        this.specializations = specializations;
    }
}

