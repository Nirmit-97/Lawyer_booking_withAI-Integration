package com.legalconnect.lawyerbooking.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Service
public class OpenAITextToSpeechService {

    private static final Logger logger = LoggerFactory.getLogger(OpenAITextToSpeechService.class);

    @Value("${openai.api.key}")
    private String apiKey;

    private static final String TTS_URL = "https://api.openai.com/v1/audio/speech";
    
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .build();

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Converts text to speech using OpenAI TTS API (English)
     * @param text The text to convert to speech
     * @return Byte array containing the audio data (MP3 format)
     */
    public byte[] textToSpeech(String text) {
        return textToSpeech(text, "en");
    }

    /**
     * Converts text to speech using OpenAI TTS API with specified language
     * @param text The text to convert to speech
     * @param languageCode Language code (e.g., "en" for English, "gu" for Gujarati)
     * @return Byte array containing the audio data (MP3 format)
     */
    public byte[] textToSpeech(String text, String languageCode) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Text cannot be null or empty");
        }

        try {
            // Build the JSON request body
            String requestBody = buildTTSRequest(text, languageCode);

            RequestBody body = RequestBody.create(
                    requestBody,
                    MediaType.parse("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(TTS_URL)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                logger.debug("OpenAI TTS Response Status: {}", response.code());

                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "No error body";
                    logger.error("OpenAI TTS Error (Status {}): {}", response.code(), errorBody);
                    throw new RuntimeException("OpenAI TTS API call failed: " + response.code() + " - " + errorBody);
                }

                // TTS API returns audio bytes directly
                if (response.body() != null) {
                    byte[] audioBytes = response.body().bytes();
                    logger.info("Successfully generated audio: {} bytes", audioBytes.length);
                    return audioBytes;
                } else {
                    logger.error("OpenAI TTS API returned empty response body");
                    throw new RuntimeException("OpenAI TTS API returned empty response body");
                }

            } catch (IOException e) {
                logger.error("Error processing OpenAI TTS response", e);
                throw new RuntimeException("Failed to process TTS response: " + e.getMessage(), e);
            }

        } catch (Exception e) {
            logger.error("Error calling OpenAI TTS API", e);
            throw new RuntimeException("Text-to-speech conversion failed: " + e.getMessage(), e);
        }
    }

    /**
     * Builds the JSON request body for OpenAI TTS API
     * @param text The text to convert to speech
     * @param languageCode Language code (e.g., "en" for English, "gu" for Gujarati)
     * @return JSON string for the request
     */
    private String buildTTSRequest(String text, String languageCode) throws Exception {
        // Using tts-1 model (high quality) - you can also use tts-1-hd for even better quality
        // Voice options: alloy, echo, fable, onyx, nova, shimmer
        ObjectNode requestJson = mapper.createObjectNode();
        requestJson.put("model", "tts-1");
        requestJson.put("input", text);
        
        // Select voice based on language - using "nova" for Gujarati as it works well with Indian languages
        // For English, keep "alloy" as default
        String voice = "gu".equals(languageCode) ? "nova" : "alloy";
        requestJson.put("voice", voice);
        requestJson.put("response_format", "mp3");
        
        return mapper.writeValueAsString(requestJson);
    }
}

