package com.legalconnect.lawyerbooking.converter;

import com.legalconnect.lawyerbooking.enums.CaseStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Converter(autoApply = true)
public class CaseStatusConverter implements AttributeConverter<CaseStatus, String> {

    private static final Logger logger = LoggerFactory.getLogger(CaseStatusConverter.class);

    @Override
    public String convertToDatabaseColumn(CaseStatus attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.name();
    }

    @Override
    public CaseStatus convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return null;
        }
        try {
            return CaseStatus.valueOf(dbData.trim().toUpperCase().replace(" ", "_"));
        } catch (IllegalArgumentException e) {
            logger.warn("Unknown CaseStatus value in database: '{}'. Defaulting to OPEN.", dbData);
            return CaseStatus.OPEN;
        }
    }
}
