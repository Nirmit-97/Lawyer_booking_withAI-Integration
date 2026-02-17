package com.legalconnect.lawyerbooking.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Service for converting audio files to WAV format using FFmpeg.
 * This enables gender detection to work with any audio format from browsers.
 */
@Service
public class AudioConversionService {

    private static final Logger logger = LoggerFactory.getLogger(AudioConversionService.class);

    /**
     * Converts an audio file to WAV format using FFmpeg.
     * 
     * @param audioFile The input audio file (any format)
     * @return A new MultipartFile containing the WAV audio
     * @throws Exception if conversion fails
     */
    public MultipartFile convertToWav(MultipartFile audioFile) throws Exception {
        logger.info("Converting audio to WAV format for file: {}", audioFile.getOriginalFilename());

        // Create temporary files
        Path inputPath = null;
        Path outputPath = null;

        try {
            // Create temp input file
            String originalFilename = audioFile.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : ".tmp";

            inputPath = Files.createTempFile("audio_input_", extension);
            outputPath = Files.createTempFile("audio_output_", ".wav");

            // Write uploaded file to temp input
            Files.write(inputPath, audioFile.getBytes());

            // Build FFmpeg command
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "ffmpeg",
                    "-i", inputPath.toString(),
                    "-acodec", "pcm_s16le", // PCM 16-bit signed little-endian
                    "-ar", "16000", // 16kHz sample rate (good for voice)
                    "-ac", "1", // Mono channel
                    "-y", // Overwrite output file
                    outputPath.toString());

            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            // Capture output for debugging
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();

            if (exitCode != 0) {
                logger.error("FFmpeg conversion failed with exit code: {}", exitCode);
                logger.error("FFmpeg output: {}", output.toString());
                throw new RuntimeException("FFmpeg conversion failed. Exit code: " + exitCode);
            }

            // Read converted WAV file
            byte[] wavBytes = Files.readAllBytes(outputPath);
            logger.info("✓ Audio converted successfully: {} bytes → {} bytes (WAV)",
                    audioFile.getSize(), wavBytes.length);

            // Create new MultipartFile with WAV content
            String wavFilename = originalFilename != null
                    ? originalFilename.replaceAll("\\.[^.]+$", ".wav")
                    : "converted.wav";

            return new ByteArrayMultipartFile(wavBytes, wavFilename, "audio/wav");

        } catch (IOException e) {
            logger.error("IO error during audio conversion: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to convert audio file", e);
        } catch (InterruptedException e) {
            logger.error("FFmpeg process was interrupted: {}", e.getMessage(), e);
            Thread.currentThread().interrupt();
            throw new RuntimeException("Audio conversion was interrupted", e);
        } finally {
            // Clean up temporary files
            if (inputPath != null) {
                try {
                    Files.deleteIfExists(inputPath);
                } catch (IOException e) {
                    logger.warn("Failed to delete temp input file: {}", e.getMessage());
                }
            }
            if (outputPath != null) {
                try {
                    Files.deleteIfExists(outputPath);
                } catch (IOException e) {
                    logger.warn("Failed to delete temp output file: {}", e.getMessage());
                }
            }
        }
    }

    /**
     * Checks if FFmpeg is available on the system.
     * 
     * @return true if FFmpeg is available, false otherwise
     */
    public boolean isFFmpegAvailable() {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("ffmpeg", "-version");
            Process process = processBuilder.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            logger.warn("FFmpeg not available: {}", e.getMessage());
            return false;
        }
    }

    @jakarta.annotation.PostConstruct
    public void init() {
        if (isFFmpegAvailable()) {
            logger.info("✓ FFmpeg is available for audio conversion");
        } else {
            logger.warn("⚠ FFmpeg is NOT available. Gender detection may fail for non-WAV audio formats.");
            logger.warn("Install FFmpeg: brew install ffmpeg (macOS) or apt-get install ffmpeg (Linux)");
        }
    }

    /**
     * Simple MultipartFile implementation backed by a byte array.
     */
    private static class ByteArrayMultipartFile implements MultipartFile {
        private final byte[] content;
        private final String name;
        private final String contentType;

        public ByteArrayMultipartFile(byte[] content, String name, String contentType) {
            this.content = content;
            this.name = name;
            this.contentType = contentType;
        }

        @Override
        public String getName() {
            return "file";
        }

        @Override
        public String getOriginalFilename() {
            return name;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public boolean isEmpty() {
            return content == null || content.length == 0;
        }

        @Override
        public long getSize() {
            return content.length;
        }

        @Override
        public byte[] getBytes() {
            return content;
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(content);
        }

        @Override
        public void transferTo(File dest) throws IOException, IllegalStateException {
            Files.write(dest.toPath(), content);
        }
    }
}
