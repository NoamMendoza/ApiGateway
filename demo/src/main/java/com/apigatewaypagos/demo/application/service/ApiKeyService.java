package com.apigatewaypagos.demo.application.service;

import java.util.Optional;
import java.io.Serializable;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.apigatewaypagos.demo.domain.repository.ApiKeyRepository;

@Service
public class ApiKeyService {
    private ApiKeyRepository apiKeyRepository;

    public ApiKeyService(ApiKeyRepository apiKeyRepository){
        this.apiKeyRepository = apiKeyRepository;
    }

    public record ApiKeyResult(String merchantId, String scopes) implements Serializable {}

    @Cacheable(value ="api_keys", key = "#p0")
    public Optional<ApiKeyResult> validateAndGetApiKeyDetails(String rawApiKey) {
        if (rawApiKey == null || rawApiKey.length() < 16) {
            return Optional.empty();
        }
        String prefix = rawApiKey.substring(0, 16);
        
        return apiKeyRepository.findByKeyPrefix(prefix).flatMap(apiKey -> {
            String hashApiKey = org.apache.commons.codec.digest.DigestUtils.sha256Hex(rawApiKey);
            if (java.security.MessageDigest.isEqual(hashApiKey.getBytes(), apiKey.getHashedKey().getBytes()) 
                && apiKey.getIsActive() 
                && !apiKey.isExpired()) {
                return Optional.of(new ApiKeyResult(apiKey.getMerchantId(), apiKey.getScopes()));
            }
            
            return Optional.empty();
        });
    }
}
