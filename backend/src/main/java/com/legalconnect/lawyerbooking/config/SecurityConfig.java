package com.legalconnect.lawyerbooking.config;

import com.legalconnect.lawyerbooking.filter.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;


@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

// CORS is globally configured using CorsConfig.java (CorsFilter Bean)

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(org.springframework.security.config.Customizer.withDefaults())
            .csrf(csrf -> csrf.disable())
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setContentType("application/json");
                    response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("{\"success\": false, \"message\": \"Unauthorized: Please login\", \"status\": 401}");
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setContentType("application/json");
                    response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_FORBIDDEN);
                    response.getWriter().write("{\"success\": false, \"message\": \"Forbidden: Insufficient permissions\", \"status\": 403}");
                })
            )
            .authorizeHttpRequests(auth -> auth
                // Public Endpoints
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/admin/login").permitAll()
                .requestMatchers("/api/bookings/lawyers").permitAll()
                .requestMatchers("/api/lawyers/*/profile").permitAll()
                .requestMatchers("/ws/**").permitAll()
                .requestMatchers("/api/payments/webhook").permitAll() // Razorpay webhook
                .requestMatchers("/error").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                
                // Swagger UI
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

                // Secured Endpoints - Role-Based
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/lawyer/**").hasRole("LAWYER") 
                .requestMatchers("/api/users/**").hasRole("USER")
                
                // Any other API request must be authenticated
                .requestMatchers("/api/**").authenticated()

                // Default fallback
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}

