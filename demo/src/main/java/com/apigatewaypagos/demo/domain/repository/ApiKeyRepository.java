package com.apigatewaypagos.demo.domain.repository;

import java.util.Optional;

import com.apigatewaypagos.demo.domain.model.ApiKey;

public interface ApiKeyRepository {
    
    Optional<ApiKey> findByKeyPrefix(String keyPrefix);
    void save(ApiKey apiKey);
}
