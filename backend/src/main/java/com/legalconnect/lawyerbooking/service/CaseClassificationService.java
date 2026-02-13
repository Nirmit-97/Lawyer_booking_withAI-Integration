package com.legalconnect.lawyerbooking.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class CaseClassificationService {

    private static final Logger logger = LoggerFactory.getLogger(CaseClassificationService.class);

    @Value("${openai.api.key}")
    private String apiKey;

    private static final String CHAT_COMPLETIONS_URL = "https://api.openai.com/v1/chat/completions";

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();

    private final ObjectMapper mapper = new ObjectMapper();
    
    @jakarta.annotation.PostConstruct
    public void init() {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            logger.error("CRITICAL: OpenAI API Key is NULL or EMPTY in CaseClassificationService!");
        } else {
            String masked = apiKey.length() > 10 ? apiKey.substring(0, 10) + "..." : "***";
            logger.info("CaseClassificationService initialized with key: {}", masked);
        }
    }

    private static final Map<String, String[]> KEYWORD_MAP = new HashMap<>();

    static {
        KEYWORD_MAP.put("Family", new String[]{"divorce", "custody", "alimony", "marriage", "child", "spouse", "parent"});
        KEYWORD_MAP.put("Criminal", new String[]{"theft", "assault", "fraud", "arrest", "police", "fir", "jail", "crime", "murder"});
        KEYWORD_MAP.put("Property", new String[]{"land", "rent", "deed", "house", "tenant", "landlord", "eviction", "mortgage"});
        KEYWORD_MAP.put("Corporate", new String[]{"business", "contract", "merger", "startup", "company", "shares", "partnership"});
        KEYWORD_MAP.put("Civil", new String[]{"dispute", "lawsuit", "compensation", "defamation", "negligence"});
        KEYWORD_MAP.put("Cyber Crime", new String[]{"hacking", "online", "internet", "phishing", "scam", "cyber", "data"});
        KEYWORD_MAP.put("Labour", new String[]{"employee", "employer", "workforce", "salary", "wage", "termination", "bonus", "union"});
    }

    private static final String CLASSIFICATION_PROMPT = """
            Act as a legal classification engine.
            
            From the list below, choose ONE category that best matches the case description:
            
            Criminal | Family | Civil | Corporate | Property | Cyber Crime | Labour
            
            If the description does not clearly match any category, choose "Civil".
            
            Case Description:
            """;

    private static final String TITLE_GENERATION_PROMPT = """
            Act as a legal secretary.
            
            Generate a concise, professional title (3-5 words) for the following legal case description.
            Examples: "Property Dispute in Mumbai", "Corporate Breach of Contract", "Family Custody Battle".
            
            Do not use quotes in the output.
            
            Case Description:
            """;

    public String generateTitle(String maskedText) {
        if (maskedText == null || maskedText.trim().isEmpty()) {
            return "Legal Case Record";
        }

        try {
            String title = callOpenAI(maskedText, TITLE_GENERATION_PROMPT);
            System.out.println("DEBUG: AI Generated Title: " + title);
            if (title != null) {
                return title.replace("\"", "").trim();
            }
        } catch (Exception e) {
            System.out.println("DEBUG: AI Title Generation error: " + e.getMessage());
            logger.warn("AI Title Generation failed: {}", e.getMessage());
        }
        
        return null;
    }

    public String classifyCase(String maskedText) {
        if (maskedText == null || maskedText.trim().isEmpty()) {
            return "Civil";
        }

        // 1. Try AI Classification
        try {
            String aiResult = callOpenAI(maskedText);
            System.out.println("DEBUG: AI Classification raw result: " + aiResult);
            if (aiResult != null && isValidCategory(aiResult)) {
                String normalized = normalizeCategory(aiResult);
                System.out.println("DEBUG: AI Categorized as: " + normalized);
                return normalized;
            }
        } catch (Exception e) {
            System.out.println("DEBUG: AI Classification error: " + e.getMessage());
            System.err.println("DEBUG: AI Classification error (full stack): ");
            e.printStackTrace();
            logger.warn("AI Classification failed, falling back to keywords. Error: {}", e.getMessage(), e);
        }

        // 2. Keyword Fallback
        return classifyWithKeywords(maskedText);
    }

    private boolean isValidCategory(String category) {
        String trimmed = category.trim();
        return trimmed.equalsIgnoreCase("Criminal") ||
               trimmed.equalsIgnoreCase("Family") ||
               trimmed.equalsIgnoreCase("Civil") ||
               trimmed.equalsIgnoreCase("Corporate") ||
               trimmed.equalsIgnoreCase("Property") ||
               trimmed.equalsIgnoreCase("Cyber Crime") ||
               trimmed.equalsIgnoreCase("Labour");
    }

    private String normalizeCategory(String category) {
        String trimmed = category.trim().toLowerCase();
        if (trimmed.contains("cyber")) return "Cyber Crime";
        if (trimmed.contains("criminal")) return "Criminal";
        if (trimmed.contains("family")) return "Family";
        if (trimmed.contains("civil")) return "Civil";
        if (trimmed.contains("corporate")) return "Corporate";
        if (trimmed.contains("property")) return "Property";
        if (trimmed.contains("labour")) return "Labour";
        return "Civil";
    }

    private String callOpenAI(String text) throws Exception {
        return callOpenAI(text, CLASSIFICATION_PROMPT);
    }

    private String callOpenAI(String text, String promptSystem) throws Exception {
        ObjectNode requestJson = mapper.createObjectNode();
        requestJson.put("model", "gpt-3.5-turbo"); // Fallback to widely available model
        
        ArrayNode messages = mapper.createArrayNode();
        ObjectNode systemMessage = mapper.createObjectNode();
        systemMessage.put("role", "system");
        systemMessage.put("content", "You are a helpful legal assistant.");
        messages.add(systemMessage);
        
        ObjectNode userMessage = mapper.createObjectNode();
        userMessage.put("role", "user");
        userMessage.put("content", promptSystem + text);
        messages.add(userMessage);
        
        requestJson.set("messages", messages);
        requestJson.put("temperature", 0.3);

        RequestBody body = RequestBody.create(
                mapper.writeValueAsString(requestJson),
                MediaType.parse("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(CHAT_COMPLETIONS_URL)
                .addHeader("Authorization", "Bearer " + apiKey)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "No body";
                logger.error("OpenAI API Failed. Status: {}, Body: {}", response.code(), errorBody);
                return null;
            }
            JsonNode json = mapper.readTree(response.body().string());
            if (json.has("choices") && json.get("choices").size() > 0) {
                return json.get("choices").get(0).get("message").get("content").asText().trim();
            }
            return null;
        } catch (Exception e) {
            logger.error("Error making OpenAI request: {}", e.getMessage(), e);
            throw e;
        }
    }

    private String classifyWithKeywords(String text) {
        String lowerText = text.toLowerCase();
        for (Map.Entry<String, String[]> entry : KEYWORD_MAP.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (lowerText.contains(keyword)) {
                    logger.info("Classified via keyword '{}' as {}", keyword, entry.getKey());
                    return entry.getKey();
                }
            }
        }
        return "Civil";
    }
}
