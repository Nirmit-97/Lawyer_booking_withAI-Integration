package com.legalconnect.lawyerbooking.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;

@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // Allow credentials (like cookies or Authorization headers)
        config.setAllowCredentials(true);
        
        // Define exact allowed origins properly instead of using wildcard "*"
        config.setAllowedOrigins(Arrays.asList(
                "https://het-full--project.d11nz5qtychasv.amplifyapp.com",
                "http://localhost:3000"
        ));
        
        // Allow all required HTTP headers
        config.setAllowedHeaders(Collections.singletonList("*"));
        
        // Allow all HTTP methods
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        
        // Cache preflight requests for 1 hour
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Register the configuration for all API endpoints globally
        source.registerCorsConfiguration("/**", config);
        
        return source;
    }
}
