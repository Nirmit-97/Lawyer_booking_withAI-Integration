package com.legalconnect.lawyerbooking.converter;

import com.legalconnect.lawyerbooking.enums.CaseType;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class CaseTypeRequestConverter implements Converter<String, CaseType> {

    @Override
    public CaseType convert(String source) {
        if (source == null || source.trim().isEmpty()) {
            return null;
        }

        // 1. Normalize (trim, upper case, spaces to underscores)
        String normalized = source.trim().toUpperCase().replace(" ", "_");
        
        // 2. Try exact match
        try {
            return CaseType.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            // 3. Try stripping "_LAW" or "LAW" suffix
            String stripped = normalized;
            if (normalized.endsWith("_LAW")) {
                stripped = normalized.substring(0, normalized.length() - 4);
            } else if (normalized.endsWith("LAW")) {
                stripped = normalized.substring(0, normalized.length() - 3);
            }
            
            if (!stripped.equals(normalized)) {
                try {
                    return CaseType.valueOf(stripped);
                } catch (IllegalArgumentException e2) {
                    // Still not found
                }
            }

            // 4. Default to OTHER instead of throwing error
            return CaseType.OTHER;
        }
    }
}
