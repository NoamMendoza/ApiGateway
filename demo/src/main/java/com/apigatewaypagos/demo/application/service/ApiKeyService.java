package com.apigatewaypagos.demo.application.service;

import java.util.Optional;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.apigatewaypagos.demo.domain.repository.ApiKeyRepository;

@Service
public class ApiKeyService {
    private ApiKeyRepository apiKeyRepository;

    public ApiKeyService(ApiKeyRepository apiKeyRepository){
        this.apiKeyRepository = apiKeyRepository;
    }

    @Cacheable(value ="api_keys", key = "#rawApiKey")
    public Optional<String> validateAndGetMerchantId(String rawApiKey) {
        if (rawApiKey == null || rawApiKey.length() < 8) {
            return Optional.empty();
        }
        String prefix = rawApiKey.substring(0, 8);
        
        return apiKeyRepository.findByKeyPrefix(prefix).flatMap(apiKey -> {
            String hashApiKey = org.apache.commons.codec.digest.DigestUtils.sha256Hex(rawApiKey);
            
            if (hashApiKey.equals(apiKey.getHashedKey()) && apiKey.getIsActive() && !apiKey.isExpired()) {
                return Optional.of(apiKey.getMerchantId());
            }
            
            return Optional.empty();
        });
    }
}
