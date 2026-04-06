package com.apigatewaypagos.demo.infrastructure.persistence.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.apigatewaypagos.demo.domain.model.ApiKey;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "api_keys")
@Getter
@Setter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class ApiKeyEntity {
    @Id
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID id;

    @Column(name = "merchant_id", nullable = false)
    private String merchantId;

    @Column(unique = true, nullable = false)
    private String keyPrefix;

    @Column(nullable = false)
    private String hashedKey;
    
    //Default TRUE
    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "scopes")
    private String scopes;

    public static ApiKeyEntity fromDomain(com.apigatewaypagos.demo.domain.model.ApiKey apiKey){
        ApiKeyEntity entity = new ApiKeyEntity();
        entity.setId(apiKey.getId());
        entity.setMerchantId(apiKey.getMerchantId());
        entity.setKeyPrefix(apiKey.getKeyPrefix());
        entity.setHashedKey(apiKey.getHashedKey());
        entity.setIsActive(apiKey.getIsActive());
        entity.setCreatedAt(apiKey.getCreatedAt());
        entity.setExpiresAt(apiKey.getExpiresAt());
        entity.setScopes(apiKey.getScopes());
        return entity;
    }

    public com.apigatewaypagos.demo.domain.model.ApiKey toDomain(){
        return com.apigatewaypagos.demo.domain.model.ApiKey.restore(
            this.id,
            this.merchantId,
            this.keyPrefix,
            this.hashedKey,
            this.createdAt,
            this.isActive,
            this.expiresAt,
            this.scopes
        );
    };
}
