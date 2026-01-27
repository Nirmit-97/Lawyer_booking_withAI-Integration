package com.legalconnect.lawyerbooking.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.concurrent.TimeUnit;

@Service
public class OpenAIWhisperService {

    private static final Logger logger = LoggerFactory.getLogger(OpenAIWhisperService.class);

    @Value("${openai.api.key}")
    private String apiKey;

    private static final String TRANSLATE_URL =
            "https://api.openai.com/v1/audio/translations";

    // Increased timeouts for long audio files (Whisper can take time for long audio)
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(300, TimeUnit.SECONDS) // 5 minutes for long audio processing
            .writeTimeout(120, TimeUnit.SECONDS)
            .build();
    
    private final ObjectMapper mapper = new ObjectMapper();
    
    @jakarta.annotation.PostConstruct
    public void init() {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            logger.error("CRITICAL: OpenAI API Key is NULL or EMPTY in OpenAIWhisperService!");
        } else {
            String masked = apiKey.length() > 10 ? apiKey.substring(0, 10) + "..." : "***";
            logger.info("OpenAIWhisperService initialized with key: {}", masked);
        }
    }

    // ================= Gujarati Audio → English Text =================
    public String translateToEnglish(MultipartFile file) throws Exception {
        
        long fileSize = file.getSize();
        logger.info("Starting Whisper translation for file: {} (size: {} bytes)", 
                   file.getOriginalFilename(), fileSize);

        MediaType mediaType = MediaType.parse(
                file.getContentType() != null ? file.getContentType() : "audio/wav"
        );

        RequestBody body = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                        "file",
                        file.getOriginalFilename(),
                        RequestBody.create(file.getBytes(), mediaType)
                )
                // 🔥 STABLE MODEL
                .addFormDataPart("model", "whisper-1")
                .build();

        Request request = new Request.Builder()
                .url(TRANSLATE_URL)
                .addHeader("Authorization", "Bearer " + apiKey)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {

            String responseBody = response.body() != null
                    ? response.body().string()
                    : "";

            logger.debug("Whisper Translation Response Status: {}", response.code());

            if (!response.isSuccessful()) {
                logger.error("Whisper API Error (Status {}): {}", response.code(), responseBody);
                
                // Try to parse error message from response
                try {
                    JsonNode errorJson = mapper.readTree(responseBody);
                    if (errorJson.has("error")) {
                        JsonNode error = errorJson.get("error");
                        String errorMsg = error.has("message") ? error.get("message").asText() : responseBody;
                        throw new RuntimeException("OpenAI Whisper API error: " + errorMsg);
                    }
                } catch (Exception e) {
                    // If JSON parsing fails, use raw response
                }
                
                throw new RuntimeException("OpenAI Whisper API error (Status " + response.code() + "): " + responseBody);
            }

            JsonNode json = mapper.readTree(responseBody);

            // Check for error in response
            if (json.has("error")) {
                JsonNode error = json.get("error");
                String errorMsg = error.has("message") ? error.get("message").asText() : "Unknown error";
                throw new RuntimeException("OpenAI Whisper API error: " + errorMsg);
            }

            if (!json.has("text")) {
                throw new RuntimeException("Invalid Whisper response: missing 'text' field. Response: " + responseBody);
            }

            String translatedText = json.get("text").asText();
            logger.info("Whisper translation completed. Text length: {} characters", translatedText.length());
            
            return translatedText;
        } catch (java.net.SocketTimeoutException e) {
            logger.error("Whisper API request timed out", e);
            throw new RuntimeException("Whisper API request timed out. The audio file might be too long. " +
                                     "Please try with a shorter audio file or check your network connection.", e);
        } catch (Exception e) {
            logger.error("Error in Whisper translation: {}", e.getMessage(), e);
            throw e;
        }
    }
}
