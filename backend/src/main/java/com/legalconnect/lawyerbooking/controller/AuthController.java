package com.legalconnect.lawyerbooking.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.validation.Valid;

import com.legalconnect.lawyerbooking.dto.LoginRequest;
import com.legalconnect.lawyerbooking.dto.LoginResponse;
import com.legalconnect.lawyerbooking.dto.RegistrationRequest;
import com.legalconnect.lawyerbooking.dto.RegistrationResponse;
import com.legalconnect.lawyerbooking.dto.LawyerRegistrationRequest;
import com.legalconnect.lawyerbooking.dto.TokenRefreshRequest;
import com.legalconnect.lawyerbooking.service.AuthService;
import com.legalconnect.lawyerbooking.exception.BadRequestException;
import com.legalconnect.lawyerbooking.exception.UnauthorizedException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/user/login")
    public ResponseEntity<LoginResponse> userLogin(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.loginUser(request));
    }

    @PostMapping("/lawyer/login")
    public ResponseEntity<LoginResponse> lawyerLogin(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.loginLawyer(request));
    }
    
    @PostMapping("/user/register")
    public ResponseEntity<RegistrationResponse> userRegister(@Valid @RequestBody RegistrationRequest request) {
        return ResponseEntity.status(201).body(authService.registerUser(request));
    }

    @PostMapping("/lawyer/register")
    public ResponseEntity<RegistrationResponse> lawyerRegister(@Valid @RequestBody LawyerRegistrationRequest request) {
        return ResponseEntity.status(201).body(authService.registerLawyer(request));
    }
    
    // Admin login usually handled separately but if exposed via auth controller:
    @PostMapping("/admin/login")
    public ResponseEntity<LoginResponse> adminLogin(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.loginAdmin(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refreshToken(@Valid @RequestBody TokenRefreshRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request.getRefreshToken()));
    }
    
    // Exception Handlers specific to Auth if GlobalExceptionHandler doesn't cover them perfectly yet
    // But assuming GlobalExceptionHandler is active, we can rely on it.
}

