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
        return textToSpeech(text, languageCode, "MALE");
    }

    /**
     * Converts text to speech using OpenAI TTS API with specified language and gender
     * @param text The text to convert to speech
     * @param languageCode Language code (e.g., "en" for English, "gu" for Gujarati)
     * @param gender Detected gender ("MALE" or "FEMALE")
     * @return Byte array containing the audio data (MP3 format)
     */
    public byte[] textToSpeech(String text, String languageCode, String gender) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Text cannot be null or empty");
        }

        try {
            // Determine voice based on gender
            String voice = "alloy"; // default
            if ("MALE".equalsIgnoreCase(gender)) {
                voice = "onyx";
            } else if ("FEMALE".equalsIgnoreCase(gender)) {
                voice = "nova";
            }

            // Build the JSON request body
            String requestBody = buildTTSRequest(text, voice);

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
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "No error body";
                    logger.error("OpenAI TTS Error (Status {}): {}", response.code(), errorBody);
                    throw new RuntimeException("OpenAI TTS API call failed: " + response.code() + " - " + errorBody);
                }

                if (response.body() != null) {
                    return response.body().bytes();
                } else {
                    throw new RuntimeException("OpenAI TTS API returned empty response body");
                }
            } catch (IOException e) {
                logger.error("Error processing OpenAI TTS response", e);
                throw new RuntimeException("Failed to process TTS response", e);
            }

        } catch (Exception e) {
            logger.error("Error calling OpenAI TTS API", e);
            throw new RuntimeException("Text-to-speech conversion failed", e);
        }
    }

    private String buildTTSRequest(String text, String voice) throws Exception {
        ObjectNode requestJson = mapper.createObjectNode();
        requestJson.put("model", "tts-1");
        requestJson.put("input", text);
        requestJson.put("voice", voice); // Use the selected voice
        requestJson.put("response_format", "mp3");
        return mapper.writeValueAsString(requestJson);
    }
}

