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
     * Converts text to speech using OpenAI TTS API (English, neutral voice)
     * 
     * @param text The text to convert to speech
     * @return Byte array containing the audio data (MP3 format)
     */
    public byte[] textToSpeech(String text) {
        return textToSpeech(text, "en", "NEUTRAL");
    }

    /**
     * Converts text to speech using OpenAI TTS API with specified language (neutral
     * voice)
     * 
     * @param text         The text to convert to speech
     * @param languageCode Language code (e.g., "en" for English, "gu" for Gujarati)
     * @return Byte array containing the audio data (MP3 format)
     */
    public byte[] textToSpeech(String text, String languageCode) {
        return textToSpeech(text, languageCode, "NEUTRAL");
    }

    /**
     * Converts text to speech using OpenAI TTS API with specified language and
     * gender
     * 
     * @param text         The text to convert to speech
     * @param languageCode Language code (e.g., "en" for English, "gu" for Gujarati)
     * @param gender       Gender for voice selection ("MALE", "FEMALE", or
     *                     "NEUTRAL")
     * @return Byte array containing the audio data (MP3 format)
     */
    public byte[] textToSpeech(String text, String languageCode, String gender) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Text cannot be null or empty");
        }

        try {
            // Build the JSON request body with gender
            String requestBody = buildTTSRequest(text, languageCode, gender);

            RequestBody body = RequestBody.create(
                    requestBody,
                    MediaType.parse("application/json; charset=utf-8"));

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
                    logger.info("Successfully generated audio: {} bytes (gender: {}, language: {})",
                            audioBytes.length, gender, languageCode);
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
     * 
     * @param text         The text to convert to speech
     * @param languageCode Language code (e.g., "en" for English, "gu" for Gujarati)
     * @param gender       Gender of the speaker ("MALE", "FEMALE", or "NEUTRAL")
     * @return JSON string for the request
     */
    private String buildTTSRequest(String text, String languageCode, String gender) throws Exception {
        // Using tts-1 model (high quality) - you can also use tts-1-hd for even better
        // quality
        ObjectNode requestJson = mapper.createObjectNode();
        requestJson.put("model", "tts-1");
        requestJson.put("input", text);

        // Select voice based on gender and language
        String voice = selectVoice(languageCode, gender);
        requestJson.put("voice", voice);
        requestJson.put("response_format", "mp3");

        return mapper.writeValueAsString(requestJson);
    }

    /**
     * Selects appropriate OpenAI voice based on language and gender
     * 
     * @param languageCode Language code
     * @param gender       Detected gender ("MALE", "FEMALE", or "NEUTRAL")
     * @return Voice name for OpenAI TTS
     */
    private String selectVoice(String languageCode, String gender) {
        // For Gujarati, use voices that work well with Indian languages
        if ("gu".equals(languageCode)) {
            if ("MALE".equalsIgnoreCase(gender)) {
                return "onyx"; // Deep male voice
            } else if ("FEMALE".equalsIgnoreCase(gender)) {
                return "shimmer"; // Clear female voice
            } else {
                return "nova"; // Neutral, works well for Gujarati
            }
        }

        // For English
        if ("MALE".equalsIgnoreCase(gender)) {
            return "onyx"; // Deep male voice
        } else if ("FEMALE".equalsIgnoreCase(gender)) {
            return "shimmer"; // Clear female voice
        } else {
            return "alloy"; // Neutral default
        }
    }
}
