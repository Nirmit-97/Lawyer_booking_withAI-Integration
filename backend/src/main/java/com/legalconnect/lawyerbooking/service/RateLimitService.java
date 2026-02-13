package com.legalconnect.lawyerbooking.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {

    // Separate buckets for different users/keys and endpoint types
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    // Standard API bucket: 100 requests per minute per user
    private Bucket getStandardBucket(String key) {
        return buckets.computeIfAbsent(key + ":standard", k -> Bucket.builder()
                .addLimit(Bandwidth.classic(100, Refill.intervally(100, Duration.ofMinutes(1))))
                .build());
    }

    // Costly AI bucket: 5 requests per minute per user
    public Bucket getAiBucket(String key) {
        return buckets.computeIfAbsent(key + ":ai", k -> Bucket.builder()
                .addLimit(Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(1))))
                .build());
    }

    public boolean tryConsumeAi(String key) {
        return getAiBucket(key).tryConsume(1);
    }

    public boolean tryConsumeStandard(String key) {
        return getStandardBucket(key).tryConsume(1);
    }

    // For backwards compatibility or global limits if needed
    public boolean tryConsumeAi() {
        return tryConsumeAi("global");
    }

    public boolean tryConsumeStandard() {
        return tryConsumeStandard("global");
    }
}
