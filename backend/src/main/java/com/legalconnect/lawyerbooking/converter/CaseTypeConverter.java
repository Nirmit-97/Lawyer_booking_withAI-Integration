package com.legalconnect.lawyerbooking.converter;

import com.legalconnect.lawyerbooking.enums.CaseType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Converter(autoApply = true)
public class CaseTypeConverter implements AttributeConverter<CaseType, String> {

    private static final Logger logger = LoggerFactory.getLogger(CaseTypeConverter.class);

    @Override
    public String convertToDatabaseColumn(CaseType attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.name();
    }

    @Override
    public CaseType convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return null;
        }
        try {
            return CaseType.valueOf(dbData.trim().toUpperCase().replace(" ", "_"));
        } catch (IllegalArgumentException e) {
            logger.warn("Unknown CaseType value in database: '{}'. Defaulting to OTHER.", dbData);
            return CaseType.OTHER;
        }
    }
}
