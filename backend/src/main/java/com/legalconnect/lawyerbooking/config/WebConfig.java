package com.legalconnect.lawyerbooking.config;

import com.legalconnect.lawyerbooking.converter.CaseTypeRequestConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private CaseTypeRequestConverter caseTypeRequestConverter;

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(caseTypeRequestConverter);
    }
}
