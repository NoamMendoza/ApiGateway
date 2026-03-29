package com.apigatewaypagos.demo.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.apigatewaypagos.demo.infrastructure.persistence.entity.ApiKeyEntity;

public interface SpringDataApiKeyRepository extends JpaRepository<ApiKeyEntity, UUID>{
    Optional<ApiKeyEntity> findByKeyPrefix(String keyPrefix);
}
