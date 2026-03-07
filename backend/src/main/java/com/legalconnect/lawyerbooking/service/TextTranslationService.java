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

import java.util.concurrent.TimeUnit;

@Service
public class TextTranslationService {

    private static final Logger logger = LoggerFactory.getLogger(TextTranslationService.class);

    @Value("${app.openai.api.key}")
    private String apiKey;

    private static final String CHAT_COMPLETIONS_URL =
            "https://api.openai.com/v1/chat/completions";

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .build();
    
    private static final int CHUNK_SIZE = 50000; // Characters per chunk
    private static final int OVERLAP_SIZE = 500; // Overlap to avoid splitting in middle of sentences

    private final ObjectMapper mapper = new ObjectMapper();

    private static final String TRANSLATION_PROMPT = """
            You are a professional translator. Translate the following English text to Gujarati.
            
            Important requirements:
            - Preserve all mask tokens exactly as they are (e.g., [NAME_MASKED], [PHONE_MASKED], [EMAIL_MASKED], [ADDRESS_MASKED], [ID_MASKED], [DOB_MASKED], [PII_MASKED])
            - Maintain the same sentence structure and meaning
            - Use natural Gujarati language
            - Do not translate the mask tokens, keep them in English format
            
            Text to translate:
            """;

    /**
     * Translates English masked text to Gujarati
     * @param englishText The English text to translate
     * @return Gujarati translated text with mask tokens preserved
     */
    public String translateToGujarati(String englishText) {
        if (englishText == null || englishText.trim().isEmpty()) {
            return englishText;
        }

        try {
            // For very long texts, process in chunks
            if (englishText.length() > CHUNK_SIZE) {
                logger.info("Text is long ({} chars), translating in chunks...", englishText.length());
                return translateLongText(englishText);
            }

            // For normal length texts, process directly
            return translateTextChunk(englishText);

        } catch (Exception e) {
            logger.error("Error in translateToGujarati", e);
            // Fallback: return original text if translation fails
            return englishText;
        }
    }

    /**
     * Translates a single chunk of text using OpenAI API
     */
    private String translateTextChunk(String text) {
        try {
            // Build the request payload
            String requestBody = buildTranslationRequest(text);

            RequestBody body = RequestBody.create(
                    requestBody,
                    MediaType.parse("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(CHAT_COMPLETIONS_URL)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                String responseBody = response.body() != null
                        ? response.body().string()
                        : "";

                logger.debug("OpenAI Translation Response Status: {}", response.code());

                if (!response.isSuccessful()) {
                    logger.error("OpenAI Translation Error (Status {}): {}", response.code(), responseBody);
                    // Fallback: return original text if API call fails
                    return text;
                }

                JsonNode json = mapper.readTree(responseBody);

                // Check for API errors in response
                if (json.has("error")) {
                    JsonNode error = json.get("error");
                    String errorMsg = error.has("message") ? error.get("message").asText() : "Unknown error";
                    logger.error("OpenAI API Error: {}", errorMsg);
                    return text;
                }

                // Extract the translated text from the response
                if (json.has("choices") && json.get("choices").isArray() && json.get("choices").size() > 0) {
                    JsonNode firstChoice = json.get("choices").get(0);
                    if (firstChoice.has("message") && firstChoice.get("message").has("content")) {
                        String translatedText = firstChoice.get("message").get("content").asText().trim();
                        logger.info("Successfully translated text to Gujarati (length: {})", translatedText.length());
                        return translatedText;
                    }
                }

                logger.error("Invalid OpenAI response structure: {}", responseBody);
                return text;

            } catch (Exception e) {
                logger.error("Error processing OpenAI response", e);
                return text;
            }

        } catch (Exception e) {
            logger.error("Error calling OpenAI API for translation", e);
            // Fallback: return original text if API call fails
            return text;
        }
    }

    /**
     * Processes very long texts by splitting into chunks and translating each chunk
     */
    private String translateLongText(String text) {
        StringBuilder translatedResult = new StringBuilder();
        int totalLength = text.length();
        int processed = 0;

        while (processed < totalLength) {
            int chunkEnd = Math.min(processed + CHUNK_SIZE, totalLength);
            
            // Try to break at sentence boundary if not at end
            if (chunkEnd < totalLength) {
                int lastPeriod = text.lastIndexOf('.', chunkEnd);
                int lastNewline = text.lastIndexOf('\n', chunkEnd);
                int breakPoint = Math.max(lastPeriod, lastNewline);
                
                if (breakPoint > processed + CHUNK_SIZE / 2) {
                    chunkEnd = breakPoint + 1;
                }
            }

            String chunk = text.substring(processed, chunkEnd);
            logger.debug("Translating chunk {} (chars {}-{} of {})", 
                        processed / CHUNK_SIZE + 1, processed, chunkEnd, totalLength);

            String translatedChunk = translateTextChunk(chunk);
            translatedResult.append(translatedChunk);

            // Move forward, with overlap to avoid missing context
            processed = chunkEnd - OVERLAP_SIZE;
            if (processed < 0) processed = chunkEnd;
        }

        return translatedResult.toString();
    }

    /**
     * Builds the JSON request body for OpenAI Chat Completions API
     */
    private String buildTranslationRequest(String text) throws Exception {
        String model = "gpt-4o-mini";
        
        String fullPrompt = TRANSLATION_PROMPT + text;

        // Calculate max_tokens dynamically based on input length
        int estimatedInputTokens = text.length() / 4;
        int maxTokens = Math.max(estimatedInputTokens + 500, 2000);
        maxTokens = Math.min(maxTokens, 16000);

        logger.debug("Building translation request: input length={} chars, estimated tokens={}, max_output_tokens={}", 
                     text.length(), estimatedInputTokens, maxTokens);

        // Build JSON using Jackson ObjectMapper
        ObjectNode requestJson = mapper.createObjectNode();
        requestJson.put("model", model);
        
        ArrayNode messages = mapper.createArrayNode();
        
        // System message
        ObjectNode systemMessage = mapper.createObjectNode();
        systemMessage.put("role", "system");
        systemMessage.put("content", "You are a professional translator that translates English to Gujarati while preserving mask tokens exactly as they are.");
        messages.add(systemMessage);
        
        // User message with the prompt and text
        ObjectNode userMessage = mapper.createObjectNode();
        userMessage.put("role", "user");
        userMessage.put("content", fullPrompt);
        messages.add(userMessage);
        
        requestJson.set("messages", messages);
        requestJson.put("temperature", 0.3); // Low temperature for consistent translation
        requestJson.put("max_tokens", maxTokens);

        return mapper.writeValueAsString(requestJson);
    }
}

