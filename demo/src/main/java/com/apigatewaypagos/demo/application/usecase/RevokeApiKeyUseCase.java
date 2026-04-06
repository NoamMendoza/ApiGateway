package com.apigatewaypagos.demo.application.usecase;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.apigatewaypagos.demo.domain.model.ApiKey;
import com.apigatewaypagos.demo.domain.repository.ApiKeyRepository;

@Service
public class RevokeApiKeyUseCase {
    private static final Logger log = LoggerFactory.getLogger(RevokeApiKeyUseCase.class);

    private final ApiKeyRepository apiKeyRepository;

    public RevokeApiKeyUseCase(ApiKeyRepository apiKeyRepository){
        this.apiKeyRepository = apiKeyRepository;
    }

    public void execute(String merchantId, UUID keyId){
        ApiKey apiKey = apiKeyRepository.findById(keyId).orElseThrow(() -> new IllegalArgumentException("API Key no encontrada"));
        if(!apiKey.getMerchantId().equals(merchantId)){
            throw new SecurityException("Esta API Key no te pertenece");
        }
        ApiKey revokedKey = ApiKey.restore(
            apiKey.getId(),
            apiKey.getMerchantId(),
            apiKey.getKeyPrefix(),
            apiKey.getHashedKey(),
            apiKey.getCreatedAt(),
            false,
            apiKey.getExpiresAt(),
            apiKey.getScopes()
        );

        apiKeyRepository.save(revokedKey);
        log.info("API Key {} del merchant {} revocada con éxito", apiKey.getId(), merchantId);
    }
}
