package com.srijan.portfolio.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.srijan.portfolio.entity.ResumeAiCacheEntry;
import com.srijan.portfolio.repository.ResumeAiCacheRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ResumeAiCacheService {

    private final ResumeAiCacheRepository resumeAiCacheRepository;
    private final ObjectMapper objectMapper;

    public String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((input == null ? "" : input).getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte value : hash) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    public <T> Optional<T> read(String cacheType, String cacheHash, Class<T> targetType) {
        return resumeAiCacheRepository.findByCacheKey(buildCacheKey(cacheType, cacheHash))
                .map(ResumeAiCacheEntry::getResponseJson)
                .flatMap(json -> deserialize(json, targetType));
    }

    public void write(String cacheType, String cacheHash, Object payload) {
        try {
            String cacheKey = buildCacheKey(cacheType, cacheHash);
            ResumeAiCacheEntry entry = resumeAiCacheRepository.findByCacheKey(cacheKey)
                    .orElse(ResumeAiCacheEntry.builder()
                            .cacheKey(cacheKey)
                            .cacheType(cacheType)
                            .build());
            entry.setResponseJson(objectMapper.writeValueAsString(payload));
            resumeAiCacheRepository.save(entry);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize resume AI cache payload", exception);
        }
    }

    public String hashObject(Object value) {
        try {
            return sha256(objectMapper.writeValueAsString(value));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to hash cache payload", exception);
        }
    }

    private String buildCacheKey(String cacheType, String cacheHash) {
        return cacheType + ":" + cacheHash;
    }

    private <T> Optional<T> deserialize(String json, Class<T> targetType) {
        try {
            return Optional.of(objectMapper.readValue(json, targetType));
        } catch (JsonProcessingException exception) {
            return Optional.empty();
        }
    }
}
