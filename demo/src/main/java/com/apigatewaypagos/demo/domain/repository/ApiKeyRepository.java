package com.apigatewaypagos.demo.domain.repository;

import java.util.Optional;
import java.util.UUID;

import com.apigatewaypagos.demo.domain.model.ApiKey;

public interface ApiKeyRepository {
    
    Optional<ApiKey> findByKeyPrefix(String keyPrefix);
    Optional<ApiKey> findById(UUID id);
    void save(ApiKey apiKey);
}
