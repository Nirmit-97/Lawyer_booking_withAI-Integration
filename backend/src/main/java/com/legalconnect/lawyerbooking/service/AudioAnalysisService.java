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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

/**
 * Service to analyze audio files and detect gender using OpenAI's Multimodal API.
 * Uses gpt-4o-audio-preview to listen and classify the speaker.
 */
@Service
public class AudioAnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(AudioAnalysisService.class);

    @Value("${openai.api.key}")
    private String apiKey;

    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
    
    // Using gpt-4o-audio-preview for audio analysis capabilities
    private static final String MODEL = "gpt-4o-audio-preview";

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .build();

    private final ObjectMapper mapper = new ObjectMapper();

    public String detectGender(File audioFile) {
        if (audioFile == null || !audioFile.exists()) {
            logger.warn("Audio file not found for gender detection, defaulting to MALE");
            return "MALE";
        }

        try {
            logger.info("Analyzing audio for gender detection using OpenAI (Size: {} bytes)", audioFile.length());
            
            // 1. Encode Audio to Base64
            // Note: Large files might hit token limits, but speech clips are usually short
            byte[] fileContent = Files.readAllBytes(audioFile.toPath());
            String base64Audio = Base64.getEncoder().encodeToString(fileContent);

            // 2. Build Request
            String requestBody = buildAnalysisRequest(base64Audio);

            Request request = new Request.Builder()
                    .url(OPENAI_API_URL)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
                    .build();

            // 3. Execute Call
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "No error body";
                    logger.error("OpenAI Analysis Error (Status {}): {}", response.code(), errorBody);
                    // Fallback to MALE if API fails/quota exceeded
                    return "MALE";
                }

                if (response.body() != null) {
                    String responseString = response.body().string();
                    return parseGenderResponse(responseString);
                }
            }

        } catch (Exception e) {
            logger.error("Failed to detect gender via OpenAI", e);
        }
        
        return "MALE"; // Safe default
    }

    private String buildAnalysisRequest(String base64Audio) throws Exception {
        ObjectNode root = mapper.createObjectNode();
        root.put("model", MODEL);
        // We only want a text response (classification)
        root.putArray("modalities").add("text");

        ArrayNode messages = root.putArray("messages");
        ObjectNode userMessage = messages.addObject();
        userMessage.put("role", "user");
        
        ArrayNode content = userMessage.putArray("content");
        
        // Text Prompt
        ObjectNode textPart = content.addObject();
        textPart.put("type", "text");
        textPart.put("text", "Listen to this audio. Is the speaker MALE or FEMALE? Reply with ONLY one word: 'MALE' or 'FEMALE'.");

        // Audio Input
        ObjectNode audioPart = content.addObject();
        audioPart.put("type", "input_audio");
        ObjectNode inputAudio = audioPart.putObject("input_audio");
        inputAudio.put("data", base64Audio);
        inputAudio.put("format", "wav"); // Assuming WAV from AudioProcessingService temp file

        return mapper.writeValueAsString(root);
    }

    private String parseGenderResponse(String jsonResponse) {
        try {
            JsonNode root = mapper.readTree(jsonResponse);
            JsonNode choices = root.get("choices");
            if (choices != null && choices.isArray() && choices.size() > 0) {
                JsonNode message = choices.get(0).get("message");
                if (message != null && message.has("content")) {
                    String content = message.get("content").asText().trim().toUpperCase();
                    logger.info("OpenAI Gender Classification: {}", content);
                    
                    if (content.contains("FEMALE")) return "FEMALE";
                    if (content.contains("MALE")) return "MALE";
                }
            }
        } catch (Exception e) {
            logger.error("Error parsing OpenAI response", e);
        }
        return "MALE";
    }
}
