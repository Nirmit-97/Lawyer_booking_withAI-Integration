
package com.legalconnect.lawyerbooking.filter;

import com.legalconnect.lawyerbooking.util.JwtUtil;
import com.legalconnect.lawyerbooking.repository.UserRepository;
import com.legalconnect.lawyerbooking.repository.LawyerRepository;
import com.legalconnect.lawyerbooking.repository.AdminRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LawyerRepository lawyerRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        final String requestTokenHeader = request.getHeader("Authorization");

        // HEARTBEAT LOG: Keep this for debugging persistent 401s
        if (logger.isInfoEnabled()) {
            logger.info("FILTER HEARTBEAT: Processing request " + request.getMethod() + " " + request.getRequestURI());
        }

        String username = null;
        String jwtToken = null;
        String role = null;
        Long userId = null;

        if (requestTokenHeader != null && requestTokenHeader.startsWith("Bearer ")) {
            jwtToken = requestTokenHeader.substring(7).trim(); // Added .trim() for robustness
            try {
                username = jwtUtil.extractUsername(jwtToken);
                role = jwtUtil.extractRole(jwtToken);
                userId = jwtUtil.extractUserId(jwtToken);
            } catch (Exception e) {
                logger.warn("JWT Token parsing failed: " + e.getMessage());
            }
        }

        // Overwrite AnonymousAuthenticationToken if a valid username is extracted
        org.springframework.security.core.Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAnonymous = currentAuth instanceof org.springframework.security.authentication.AnonymousAuthenticationToken;

        if (username != null && (currentAuth == null || isAnonymous)) {
            if (logger.isDebugEnabled()) {
                logger.debug("Validating token for user: " + username + " with role: " + role);
            }
            
            if (jwtUtil.validateToken(jwtToken, username)) {
                
                // CRITICAL: Database check to ensure user still exists and isn't deleted/banned
                boolean userExists = false;
                
                try {
                    if ("USER".equals(role)) {
                        userExists = userRepository.existsById(userId);
                    } else if ("LAWYER".equals(role)) {
                        userExists = lawyerRepository.existsById(userId);
                    } else if ("ADMIN".equals(role)) {
                        userExists = adminRepository.existsById(userId);
                    } else {
                        logger.warn("Unknown role: " + role + " for user: " + username);
                    }
                } catch (Exception e) {
                    logger.error("Database check failed for user: " + username + " (ID: " + userId + "). Error: " + e.getMessage());
                }
                
                if (userExists) {
                    com.legalconnect.lawyerbooking.security.UserPrincipal principal = 
                        new com.legalconnect.lawyerbooking.security.UserPrincipal(userId, username, role);
                    
                    SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role);
                    
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            principal, null, Collections.singletonList(authority));
                    
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    
                    request.setAttribute("userId", userId);
                    request.setAttribute("role", role);
                    
                    logger.info("Successfully authenticated user: " + username + " [Role: " + role + "]");
                } else {
                    logger.warn("Access Denied: Token valid but user record not found in DB [ID: " + userId + ", Role: " + role + ", Username: " + username + "]");
                }
            } else {
                logger.warn("Token validation failed for user: " + username);
            }
        }
        
        chain.doFilter(request, response);
    }
}

