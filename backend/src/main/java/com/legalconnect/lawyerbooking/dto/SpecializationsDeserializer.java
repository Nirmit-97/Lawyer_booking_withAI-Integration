package com.legalconnect.lawyerbooking.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.legalconnect.lawyerbooking.enums.CaseType;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class SpecializationsDeserializer extends JsonDeserializer<Set<CaseType>> {
    
    @Override
    public Set<CaseType> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        Set<CaseType> result = new HashSet<>();
        JsonNode node = p.getCodec().readTree(p);
        
        if (node.isTextual()) {
            // Handle comma-separated string from frontend
            String text = node.asText();
            if (text != null && !text.trim().isEmpty()) {
                String[] parts = text.split(",");
                for (String part : parts) {
                    try {
                        String normalized = part.trim().toUpperCase().replace(" ", "_");
                        result.add(CaseType.valueOf(normalized));
                    } catch (IllegalArgumentException e) {
                        // Skip invalid values
                    }
                }
            }
        } else if (node.isArray()) {
            // Handle JSON array format
            for (JsonNode element : node) {
                try {
                    String normalized = element.asText().trim().toUpperCase().replace(" ", "_");
                    result.add(CaseType.valueOf(normalized));
                } catch (IllegalArgumentException e) {
                    // Skip invalid values
                }
            }
        }
        
        return result;
    }
}
