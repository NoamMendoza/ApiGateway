package com.apigatewaypagos.demo.infrastructure.persistence.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "merchant_configs")
@Getter
@Setter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class MerchantConfigEntity {
    
    @Id
    @Column(name = "merchant_id", nullable = false)
    private String merchantId;

    @Column(name = "webhook_url", nullable = false, length = 1000)
    private String webhookUrl;

    @Column(name = "webhook_secret", length = 64)
    private String webhookSecret;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static MerchantConfigEntity fromDomain(com.apigatewaypagos.demo.domain.model.MerchantConfig config) {
        MerchantConfigEntity entity = new MerchantConfigEntity();
        entity.setMerchantId(config.getMerchantId());
        entity.setWebhookUrl(config.getWebhookUrl());
        entity.setWebhookSecret(config.getWebhookSecret());
        entity.setCreatedAt(config.getCreatedAt());
        entity.setUpdatedAt(config.getUpdatedAt());
        return entity;
    }

    public com.apigatewaypagos.demo.domain.model.MerchantConfig toDomain() {
        return com.apigatewaypagos.demo.domain.model.MerchantConfig.restore(
            this.merchantId,
            this.webhookUrl,
            this.webhookSecret,
            this.createdAt,
            this.updatedAt
        );
    }
}
