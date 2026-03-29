package com.apigatewaypagos.demo.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class ApiKey {
    private final UUID id;
    private final String merchantId;
    private final String keyPrefix;
    private final String hashedKey;
    private final Boolean isActive;
    private final LocalDateTime createdAt;
    private final LocalDateTime expiresAt;

    public ApiKey(UUID id, String merchantId, String keyPrefix, String hashedKey, Boolean isActive, LocalDateTime createdAt, LocalDateTime expiresAt){
        this.id = id;
        this.merchantId = merchantId;
        this.keyPrefix = keyPrefix;
        this.hashedKey = hashedKey;
        this.isActive = isActive;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public UUID getId(){return id;}
    public String getMerchantId(){return merchantId;}
    public String getKeyPrefix(){return keyPrefix;}
    public String getHashedKey(){return hashedKey;}
    public Boolean getIsActive(){return isActive;}
    public LocalDateTime getCreatedAt(){return createdAt;}
    public LocalDateTime getExpiresAt(){return expiresAt;}

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }

    public static ApiKey restore(UUID id, String merchantId, String keyPrefix, String hashedKey, LocalDateTime createdAt, Boolean isActive, LocalDateTime expiresAt) {
        return new ApiKey(id, merchantId, keyPrefix, hashedKey, isActive, createdAt, expiresAt);
    }
}
