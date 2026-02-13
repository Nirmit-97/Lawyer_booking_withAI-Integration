package com.legalconnect.lawyerbooking.service;

import com.legalconnect.lawyerbooking.dto.LoginRequest;
import com.legalconnect.lawyerbooking.dto.LoginResponse;
import com.legalconnect.lawyerbooking.dto.RegistrationRequest;
import com.legalconnect.lawyerbooking.dto.RegistrationResponse;
import com.legalconnect.lawyerbooking.dto.LawyerRegistrationRequest;
import com.legalconnect.lawyerbooking.entity.User;
import com.legalconnect.lawyerbooking.entity.Lawyer;
import com.legalconnect.lawyerbooking.entity.Admin;
import com.legalconnect.lawyerbooking.repository.UserRepository;
import com.legalconnect.lawyerbooking.repository.LawyerRepository;
import com.legalconnect.lawyerbooking.repository.AdminRepository;
import com.legalconnect.lawyerbooking.repository.RefreshTokenRepository;
import com.legalconnect.lawyerbooking.util.JwtUtil;
import com.legalconnect.lawyerbooking.entity.RefreshToken;
import com.legalconnect.lawyerbooking.exception.BadRequestException;
import com.legalconnect.lawyerbooking.exception.UnauthorizedException;
import com.legalconnect.lawyerbooking.enums.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LawyerRepository lawyerRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordService passwordService;

    @Transactional
    public LoginResponse loginUser(LoginRequest request) {
        validateLoginRequest(request);

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UnauthorizedException("Invalid username or password"));

        verifyPassword(request.getPassword(), user.getPassword(), user);
        
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), Role.USER);
        String refreshToken = createRefreshToken(user.getId(), user.getUsername(), "user");
        
        logger.info("User logged in successfully: {}", user.getUsername());
        
        LoginResponse response = new LoginResponse(true, "Login successful");
        response.setUserType("user");
        response.setUsername(user.getUsername());
        response.setFullName(user.getFullName());
        response.setId(user.getId());
        response.setToken(token);
        response.setRefreshToken(refreshToken);
        
        return response;
    }

    @Transactional
    public LoginResponse loginLawyer(LoginRequest request) {
        validateLoginRequest(request);

        Lawyer lawyer = lawyerRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UnauthorizedException("Invalid username or password"));

        verifyPassword(request.getPassword(), lawyer.getPassword(), lawyer);

        String token = jwtUtil.generateToken(lawyer.getId(), lawyer.getUsername(), Role.LAWYER);
        String refreshToken = createRefreshToken(lawyer.getId(), lawyer.getUsername(), "lawyer");
        
        logger.info("Lawyer logged in successfully: {}", lawyer.getUsername());

        LoginResponse response = new LoginResponse(true, "Login successful");
        response.setUserType("lawyer");
        response.setUsername(lawyer.getUsername());
        response.setFullName(lawyer.getFullName());
        response.setId(lawyer.getId());
        response.setToken(token);
        response.setRefreshToken(refreshToken);

        return response;
    }

    @Transactional
    public LoginResponse loginAdmin(LoginRequest request) {
        validateLoginRequest(request);
        
        Admin admin = adminRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UnauthorizedException("Invalid username or password"));
                
        // Add password verification logic here if Admin entity follows same pattern
        // Assuming consistent pattern:
        if (!passwordService.verifyPassword(request.getPassword(), admin.getPassword())) {
             // Fallback for plain text if needed, similar to verifyPassword method
             if (!request.getPassword().equals(admin.getPassword())) {
                  throw new UnauthorizedException("Invalid username or password");
             }
        }

        String token = jwtUtil.generateToken(admin.getId(), admin.getUsername(), Role.ADMIN);
        String refreshToken = createRefreshToken(admin.getId(), admin.getUsername(), "admin");
        
        logger.info("Admin logged in successfully: {}", admin.getUsername());

        LoginResponse response = new LoginResponse(true, "Login successful");
        response.setUserType("admin");
        response.setUsername(admin.getUsername());
        response.setFullName(admin.getFullName());
        response.setId(admin.getId());
        response.setToken(token);
        response.setRefreshToken(refreshToken);

        return response;
    }

    @Transactional
    public RegistrationResponse registerUser(RegistrationRequest request) {
        if (!passwordService.isPasswordStrong(request.getPassword())) {
            throw new BadRequestException(passwordService.getPasswordStrengthErrorMessage());
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username already exists");
        }

        User newUser = new User();
        newUser.setUsername(request.getUsername());
        newUser.setPassword(passwordService.hashPassword(request.getPassword()));
        newUser.setFullName(request.getFullName());
        newUser.setEmail(request.getEmail());

        User savedUser = userRepository.save(newUser);
        logger.info("User registered: {}", savedUser.getUsername());

        RegistrationResponse response = new RegistrationResponse(true, "Registration successful");
        response.setUsername(savedUser.getUsername());
        response.setFullName(savedUser.getFullName());
        response.setId(savedUser.getId());

        return response;
    }

    @Transactional
    public RegistrationResponse registerLawyer(LawyerRegistrationRequest request) {
        if (!passwordService.isPasswordStrong(request.getPassword())) {
            throw new BadRequestException(passwordService.getPasswordStrengthErrorMessage());
        }

        if (lawyerRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username already exists");
        }

        Lawyer newLawyer = new Lawyer();
        newLawyer.setUsername(request.getUsername());
        newLawyer.setPassword(passwordService.hashPassword(request.getPassword()));
        newLawyer.setFullName(request.getFullName());
        newLawyer.setEmail(request.getEmail());
        newLawyer.setBarNumber(request.getBarNumber());
        newLawyer.setSpecializations(request.getSpecializations());
        newLawyer.setVerified(false);
        newLawyer.setRating(0.0);
        newLawyer.setCompletedCasesCount(0);

        Lawyer savedLawyer = lawyerRepository.save(newLawyer);
        logger.info("Lawyer registered: {}", savedLawyer.getUsername());

        RegistrationResponse response = new RegistrationResponse(true, "Registration successful");
        response.setUsername(savedLawyer.getUsername());
        response.setFullName(savedLawyer.getFullName());
        response.setId(savedLawyer.getId());

        return response;
    }

    private void validateLoginRequest(LoginRequest request) {
        if (request.getUsername() == null || request.getPassword() == null) {
            throw new BadRequestException("Username and password are required");
        }
    }

    // Generic password verification with backward compatibility for plain text
    private void verifyPassword(String rawPassword, String storedPassword, Object entity) {
        boolean valid = false;
        boolean needsRehash = false;

        if (passwordService.isHashed(storedPassword)) {
            valid = passwordService.verifyPassword(rawPassword, storedPassword);
        } else {
            // Legacy plain text check
            valid = storedPassword.equals(rawPassword);
            needsRehash = valid;
        }

        if (!valid) {
            throw new UnauthorizedException("Invalid username or password");
        }

        if (needsRehash) {
            rehashPassword(entity, rawPassword);
        }
    }

    private void rehashPassword(Object entity, String rawPassword) {
        String newHash = passwordService.hashPassword(rawPassword);
        if (entity instanceof User) {
            ((User) entity).setPassword(newHash);
            userRepository.save((User) entity);
        } else if (entity instanceof Lawyer) {
            ((Lawyer) entity).setPassword(newHash);
            lawyerRepository.save((Lawyer) entity);
        }
    }

    public String createRefreshToken(Long userId, String username, String role) {
        // Delete existing refresh tokens for this user and role to prevent bloat
        refreshTokenRepository.deleteByUserIdAndUserType(userId, role);
        
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(userId);
        refreshToken.setUserType(role);
        refreshToken.setToken(UUID.randomUUID().toString()); // Simple UUID for refresh token
        refreshToken.setExpiresAt(Instant.now().plusMillis(604800000)); // 7 days
        refreshToken.setUsed(false);
        refreshToken.setCreatedAt(Instant.now());
        
        refreshTokenRepository.save(refreshToken);
        return refreshToken.getToken();
    }

    @Transactional
    public LoginResponse refreshToken(String requestToken) {
        return refreshTokenRepository.findByToken(requestToken)
                .map(this::verifyExpiration)
                .map(refreshToken -> {
                    String userType = refreshToken.getUserType();
                    Long userId = refreshToken.getUserId();
                    String username;
                    String fullName;
                    Role role;

                    if ("lawyer".equals(userType)) {
                        Lawyer lawyer = lawyerRepository.findById(userId)
                                .orElseThrow(() -> new UnauthorizedException("Lawyer not found"));
                        username = lawyer.getUsername();
                        fullName = lawyer.getFullName();
                        role = Role.LAWYER;
                    } else if ("admin".equals(userType)) {
                        Admin admin = adminRepository.findById(userId)
                                .orElseThrow(() -> new UnauthorizedException("Admin not found"));
                        username = admin.getUsername();
                        fullName = admin.getFullName();
                        role = Role.ADMIN;
                    } else {
                        User user = userRepository.findById(userId)
                                .orElseThrow(() -> new UnauthorizedException("User not found"));
                        username = user.getUsername();
                        fullName = user.getFullName();
                        role = Role.USER;
                    }

                    // Security Check: Theft detection
                    if (refreshToken.isUsed()) {
                        logger.error("Refresh token reuse detected! Potential theft for user: {}", userId);
                        refreshTokenRepository.deleteByUserIdAndUserType(userId, userType);
                        throw new UnauthorizedException("Security violation. Please login again.");
                    }

                    String newToken = jwtUtil.generateToken(userId, username, role);
                    
                    // Mark old token as used
                    refreshToken.setUsed(true);
                    refreshTokenRepository.save(refreshToken);

                    // Create NEW refresh token (Rotation)
                    String newRefreshToken = createRefreshToken(userId, username, userType);

                    LoginResponse response = new LoginResponse(true, "Token refreshed successfully");
                    response.setToken(newToken);
                    response.setRefreshToken(newRefreshToken);
                    response.setUserType(userType);
                    response.setUsername(username);
                    response.setFullName(fullName);
                    response.setId(userId);
                    return response;
                })
                .orElseThrow(() -> new UnauthorizedException("Refresh token is not in database!"));
    }

    private RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiresAt().isBefore(Instant.now())) {
            refreshTokenRepository.delete(token);
            throw new UnauthorizedException("Refresh token was expired. Please make a new signin request");
        }
        return token;
    }
}
