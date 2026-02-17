package com.legalconnect.lawyerbooking.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for detecting gender from audio using a custom implementation of the
 * YIN Algorithm.
 * 
 * YIN is a robust pitch detection algorithm for speech and music.
 * It is much more accurate than Zero-Crossing Rate (ZCR) and less susceptible
 * to noise.
 * This implementation is dependency-free (no TarsosDSP required).
 */
@Service
public class GenderDetectionService {

    private static final Logger logger = LoggerFactory.getLogger(GenderDetectionService.class);

    private final AudioConversionService audioConversionService;

    public GenderDetectionService(AudioConversionService audioConversionService) {
        this.audioConversionService = audioConversionService;
    }

    // Pitch thresholds
    // Male: 85-180 Hz
    // Female: 165-255 Hz
    // Threshold: 175 Hz
    private static final double GENDER_THRESHOLD_HZ = 175.0;

    public String detectGender(MultipartFile audioFile) {
        try {
            logger.info("=== Starting Robust Gender Detection (YIN Loop) for file: {} ===",
                    audioFile.getOriginalFilename());

            // 1. Convert to WAV using FFmpeg (Standardizes format)
            MultipartFile wavFile = audioConversionService.convertToWav(audioFile);

            // 2. Load Audio
            try (AudioInputStream audioInputStream = AudioSystem
                    .getAudioInputStream(new ByteArrayInputStream(wavFile.getBytes()))) {
                AudioFormat format = audioInputStream.getFormat();
                float sampleRate = format.getSampleRate();

                // Read all bytes
                byte[] audioBytes = wavFile.getBytes();
                float[] samples = convertBytesToSamples(audioBytes, format);

                logger.info("Loaded {} samples. Rate: {}", samples.length, sampleRate);

                if (samples.length == 0) {
                    logger.warn("No samples decoded. Defaulting to NEUTRAL.");
                    return "NEUTRAL";
                }

                // 3. Run YIN Algorithm on chunks
                // YIN works best on short frames (e.g. 1024-2048 samples)
                // Tuning: 1024 samples @ 16kHz = 64ms (Good for speech pitch)
                int bufferSize = 1024;
                int overlap = 512;

                List<Double> pitches = new ArrayList<>();
                Yin yin = new Yin(sampleRate, bufferSize);

                for (int i = 0; i < samples.length - bufferSize; i += (bufferSize - overlap)) {
                    // Extract frame
                    float[] buffer = new float[bufferSize];
                    System.arraycopy(samples, i, buffer, 0, bufferSize);

                    // Get pitch
                    double pitch = yin.getPitch(buffer);

                    // Filter valid human voice range (60Hz - 300Hz)
                    // Male voices go down to ~85Hz, Female up to ~255Hz
                    if (pitch != -1 && pitch > 60 && pitch < 350) {
                        pitches.add(pitch);
                    }
                }

                if (pitches.isEmpty()) {
                    logger.warn("No valid pitch detected by YIN. Defaulting to NEUTRAL.");
                    return "NEUTRAL";
                }

                // Calculate average pitch
                double averagePitch = pitches.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                logger.info("YIN Analysis: Average Pitch: {:.2f} Hz (based on {} frames)", averagePitch,
                        pitches.size());

                return classifyGenderByFrequency(averagePitch);
            }

        } catch (Exception e) {
            logger.error("Error in gender detection", e);
            return "NEUTRAL";
        }
    }

    private float[] convertBytesToSamples(byte[] bytes, AudioFormat format) {
        int bytesPerSample = format.getSampleSizeInBits() / 8;
        int offset = 44; // Skip wav header
        if (bytes.length <= offset)
            return new float[0];

        int length = (bytes.length - offset) / bytesPerSample;
        float[] samples = new float[length];

        for (int i = 0; i < length; i++) {
            int index = offset + i * bytesPerSample;
            if (bytesPerSample == 2) {
                int val = (bytes[index] & 0xFF) | (bytes[index + 1] << 8);
                samples[i] = val / 32768.0f;
            } else {
                samples[i] = 0;
            }
        }
        return samples;
    }

    private String classifyGenderByFrequency(double frequency) {
        logger.info("Classifying: {:.2f} Hz vs Threshold {:.2f} Hz", frequency, GENDER_THRESHOLD_HZ);

        if (frequency < GENDER_THRESHOLD_HZ) {
            logger.info("→ MALE DETECTED");
            return "MALE";
        } else {
            logger.info("→ FEMALE DETECTED");
            return "FEMALE";
        }
    }

    /**
     * Inner class implementing the YIN Pitch Detection Algorithm.
     * Based on the paper: "YIN, a fundamental frequency estimator for speech and
     * music"
     * by A. de Cheveigné and H. Kawahara.
     */
    public static class Yin {
        private final double threshold = 0.20; // Increased threshold to robustly catch fundamental frequency
        private final float sampleRate;
        private final int bufferSize;
        private final double[] yinBuffer;

        public Yin(float sampleRate, int bufferSize) {
            this.sampleRate = sampleRate;
            this.bufferSize = bufferSize;
            this.yinBuffer = new double[bufferSize / 2];
        }

        public double getPitch(float[] audioBuffer) {
            int tauEstimate;
            double pitchInHertz;

            // Step 1: Calculate Difference Function
            difference(audioBuffer);

            // Step 2: Cumulative Mean Normalized Difference Function
            cumulativeMeanNormalizedDifference();

            // Step 3: Absolute Threshold
            tauEstimate = absoluteThreshold();

            // Step 4: Parabolic Interpolation
            if (tauEstimate != -1) {
                double betterTau = parabolicInterpolation(tauEstimate);

                // Step 5: Best Local Estimate (omitted for simplicity, Step 4 usually enough)

                pitchInHertz = sampleRate / betterTau;
            } else {
                pitchInHertz = -1; // No pitch found
            }

            return pitchInHertz;
        }

        /**
         * Step 1: Calculates the difference function.
         */
        private void difference(float[] audioBuffer) {
            int index, tau;
            double delta;
            for (tau = 0; tau < yinBuffer.length; tau++) {
                yinBuffer[tau] = 0;
            }
            for (tau = 1; tau < yinBuffer.length; tau++) {
                for (index = 0; index < listSize(); index++) {
                    delta = audioBuffer[index] - audioBuffer[index + tau];
                    yinBuffer[tau] += delta * delta;
                }
            }
        }

        private int listSize() {
            return bufferSize / 2;
        }

        /**
         * Step 2: Calculates the cumulative mean normalized difference function.
         */
        private void cumulativeMeanNormalizedDifference() {
            int tau;
            yinBuffer[0] = 1;
            double runningSum = 0;
            for (tau = 1; tau < yinBuffer.length; tau++) {
                runningSum += yinBuffer[tau];
                yinBuffer[tau] *= tau / runningSum;
            }
        }

        /**
         * Step 3: Implements the absolute threshold.
         */
        private int absoluteThreshold() {
            int tau;
            // First two positions in yinBuffer are always 1
            // Search for the first valley below threshold
            for (tau = 2; tau < yinBuffer.length; tau++) {
                if (yinBuffer[tau] < threshold) {
                    while (tau + 1 < yinBuffer.length && yinBuffer[tau + 1] < yinBuffer[tau]) {
                        tau++;
                    }
                    // Found absolute minimum below threshold
                    return tau;
                }
            }

            // If no pitch found, try to find global minimum
            // (Optional: can return -1 here for strictness)
            // For robustness, let's find the global minimum if nothing below threshold
            int bestTau = -1;
            double minVal = 1000;
            for (tau = 2; tau < yinBuffer.length; tau++) {
                if (yinBuffer[tau] < minVal) {
                    minVal = yinBuffer[tau];
                    bestTau = tau;
                }
            }
            // Only return if the minimum is somewhat reasonable (e.g. < 0.4)
            // otherwise it's likely noise
            if (minVal < 0.4)
                return bestTau;

            return -1; // No pitch
        }

        /**
         * Step 4: Interpolate the peak using parabolic interpolation.
         */
        private double parabolicInterpolation(int tauEstimate) {
            double betterTau;
            int x0, x2;

            if (tauEstimate < 1) {
                x0 = tauEstimate;
            } else {
                x0 = tauEstimate - 1;
            }
            if (tauEstimate + 1 < yinBuffer.length) {
                x2 = tauEstimate + 1;
            } else {
                x2 = tauEstimate;
            }

            if (x0 == tauEstimate) {
                if (yinBuffer[tauEstimate] <= yinBuffer[x2]) {
                    betterTau = tauEstimate;
                } else {
                    betterTau = x2;
                }
            } else if (x2 == tauEstimate) {
                if (yinBuffer[tauEstimate] <= yinBuffer[x0]) {
                    betterTau = tauEstimate;
                } else {
                    betterTau = x0;
                }
            } else {
                double s0, s1, s2;
                s0 = yinBuffer[x0];
                s1 = yinBuffer[tauEstimate];
                s2 = yinBuffer[x2];
                // Parabolic interpolation formula
                betterTau = tauEstimate + (s2 - s0) / (2 * (2 * s1 - s2 - s0));
            }
            return betterTau;
        }
    }
}
