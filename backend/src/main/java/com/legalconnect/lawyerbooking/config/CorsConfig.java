package com.legalconnect.lawyerbooking.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        // Allow credentials (like cookies or Authorization headers)
        config.setAllowCredentials(true);
        
        // Define exact allowed origins properly instead of using wildcard "*"
        config.setAllowedOrigins(Arrays.asList(
                "http://localhost:3000",
                "http://localhost:3001",
                "http://127.0.0.1:3000",
                "https://lawyer-booking-with-ai-integration.vercel.app"
        ));
        
        // Allow all required HTTP headers
        config.setAllowedHeaders(Arrays.asList(
                "Origin", 
                "Content-Type", 
                "Accept", 
                "Authorization", 
                "X-Admin-Id", 
                "X-Requested-With", 
                "Access-Control-Request-Method", 
                "Access-Control-Request-Headers"
        ));
        
        // Allow necessary HTTP methods
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        
        // Cache preflight requests for 1 hour
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Register the configuration for all API endpoints globally
        source.registerCorsConfiguration("/**", config);
        
        return new CorsFilter(source);
    }
}
