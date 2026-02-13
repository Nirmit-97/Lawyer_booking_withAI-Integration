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
public class TextMaskingService {

    private static final Logger logger = LoggerFactory.getLogger(TextMaskingService.class);

    @Value("${openai.api.key}")
    private String apiKey;

    private static final String CHAT_COMPLETIONS_URL = "https://api.openai.com/v1/chat/completions";

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .build();

    // Maximum tokens for input (gpt-4o-mini supports ~128k, but we'll use a safe
    // limit)
    private static final int MAX_INPUT_TOKENS = 100000; // Safe limit for context window
    private static final int CHUNK_SIZE = 50000; // Characters per chunk (roughly ~12k tokens)
    private static final int OVERLAP_SIZE = 500; // Overlap to avoid splitting in middle of sentences

    private final ObjectMapper mapper = new ObjectMapper();

    private static final String MASKING_PROMPT = """
            You are a privacy protection assistant. Your task is to mask ONLY personal information in the given legal case text, while preserving ALL case-related information.

            MASK these personal information types:
            - Names of people (first names, last names, full names)
            - Phone numbers (any format)
            - Email addresses
            - Physical addresses (street addresses, cities, villages, locations)
            - Aadhar numbers, PAN numbers, or other government ID numbers
            - Dates of birth
            - Any other personally identifiable information (PII)

            PRESERVE these case-related information:
            - Case details, facts, and circumstances
            - Legal issues and problems described
            - Dates of events (if not personal DOB)
            - Case numbers or reference numbers
            - Legal terminology and descriptions
            - All other information related to the case itself

            Replacement rules:
            - Replace names with [NAME_MASKED]
            - Replace phone numbers with [PHONE_MASKED]
            - Replace emails with [EMAIL_MASKED]
            - Replace addresses with [ADDRESS_MASKED]
            - Replace ID numbers with [ID_MASKED]
            - Replace dates of birth with [DOB_MASKED]
            - For other PII, use [PII_MASKED]

            Important: Maintain the original sentence structure, grammar, and flow. Only replace the personal information with the appropriate mask tokens. Do not change any case-related information, legal facts, or case descriptions.

            Text to mask:
            """;

    /**
     * Uses OpenAI GPT to intelligently mask personal information while preserving
     * case information
     * Handles long texts by chunking if necessary
     * 
     * @param text The original text containing both personal and case information
     * @return Text with personal information masked but case information preserved
     */
    public String maskEnglishPersonalInfo(String text) {
        if (text == null || text.trim().isEmpty()) {
            return text;
        }

        try {
            // For very long texts, process in chunks
            if (text.length() > CHUNK_SIZE) {
                logger.info("Text is long ({} chars), processing in chunks...", text.length());
                return maskLongText(text);
            }

            // For normal length texts, process directly
            return maskTextChunk(text);

        } catch (Exception e) {
            logger.error("Error in maskEnglishPersonalInfo", e);
            // Fallback: return original text if processing fails
            return text;
        }
    }

    /**
     * Masks a single chunk of text using OpenAI API
     */
    private String maskTextChunk(String text) {
        try {
            // Build the request payload
            String requestBody = buildChatRequest(text);

            RequestBody body = RequestBody.create(
                    requestBody,
                    MediaType.parse("application/json; charset=utf-8"));

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

                logger.debug("OpenAI Masking Response Status: {}", response.code());

                if (!response.isSuccessful()) {
                    logger.error("OpenAI Masking Error (Status {}): {}", response.code(), responseBody);
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

                // Extract the masked text from the response
                if (json.has("choices") && json.get("choices").isArray() && json.get("choices").size() > 0) {
                    JsonNode firstChoice = json.get("choices").get(0);
                    if (firstChoice.has("message") && firstChoice.get("message").has("content")) {
                        String maskedText = firstChoice.get("message").get("content").asText().trim();

                        // Check if GPT says no masking is needed
                        if (maskedText.toLowerCase().contains("does not contain any personal information") ||
                                maskedText.toLowerCase().contains("no personal information") ||
                                maskedText.toLowerCase().contains("no changes are necessary")) {
                            logger.info("No PII detected, returning original text");
                            return text;
                        }

                        logger.info("Successfully masked text using OpenAI NLP (length: {})", maskedText.length());
                        return maskedText;
                    }
                }

                logger.error("Invalid OpenAI response structure: {}", responseBody);
                return text;

            } catch (Exception e) {
                logger.error("Error processing OpenAI response", e);
                return text;
            }

        } catch (Exception e) {
            logger.error("Error calling OpenAI API for text masking", e);
            // Fallback: return original text if API call fails
            return text;
        }
    }

    /**
     * Processes very long texts by splitting into chunks and masking each chunk
     */
    private String maskLongText(String text) {
        StringBuilder maskedResult = new StringBuilder();
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
            logger.debug("Processing chunk {} (chars {}-{} of {})",
                    processed / CHUNK_SIZE + 1, processed, chunkEnd, totalLength);

            String maskedChunk = maskTextChunk(chunk);
            maskedResult.append(maskedChunk);

            // Move forward, with overlap to avoid missing context
            processed = chunkEnd - OVERLAP_SIZE;
            if (processed < 0)
                processed = chunkEnd;
        }

        return maskedResult.toString();
    }

    /**
     * Builds the JSON request body for OpenAI Chat Completions API using Jackson
     */
    private String buildChatRequest(String text) throws Exception {
        // Using gpt-4o-mini for cost-effective and fast NLP-based masking
        String model = "gpt-4o-mini";

        String fullPrompt = MASKING_PROMPT + text;

        // Calculate max_tokens dynamically based on input length
        // Rough estimate: 1 token ≈ 4 characters, but we need output tokens
        // Set max_tokens to be at least as long as input (in tokens) + buffer
        int estimatedInputTokens = text.length() / 4;
        int maxTokens = Math.max(estimatedInputTokens + 500, 2000); // At least 2000, or input length + buffer
        // Cap at model's max output tokens (gpt-4o-mini supports up to 16384 output
        // tokens)
        maxTokens = Math.min(maxTokens, 16000);

        logger.debug("Building request: input length={} chars, estimated tokens={}, max_output_tokens={}",
                text.length(), estimatedInputTokens, maxTokens);

        // Build JSON using Jackson ObjectMapper for robustness
        ObjectNode requestJson = mapper.createObjectNode();
        requestJson.put("model", model);

        ArrayNode messages = mapper.createArrayNode();

        // System message
        ObjectNode systemMessage = mapper.createObjectNode();
        systemMessage.put("role", "system");
        systemMessage.put("content",
                "You are a privacy protection assistant that masks personal information while preserving legal case details.");
        messages.add(systemMessage);

        // User message with the prompt and text
        ObjectNode userMessage = mapper.createObjectNode();
        userMessage.put("role", "user");
        userMessage.put("content", fullPrompt);
        messages.add(userMessage);

        requestJson.set("messages", messages);
        requestJson.put("temperature", 0.1); // Low temperature for consistent masking
        requestJson.put("max_tokens", maxTokens); // Dynamic based on input length

        return mapper.writeValueAsString(requestJson);
    }
}
