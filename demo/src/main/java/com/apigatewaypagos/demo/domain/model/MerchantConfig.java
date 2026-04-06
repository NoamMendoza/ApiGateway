package com.apigatewaypagos.demo.domain.model;

import java.time.LocalDateTime;

public class MerchantConfig {
    private final String merchantId;
    private final String webhookUrl;
    private final String webhookSecret;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public MerchantConfig(String merchantId, String webhookUrl, String webhookSecret, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.merchantId = merchantId;
        this.webhookUrl = webhookUrl;
        this.webhookSecret = webhookSecret;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getMerchantId() { return merchantId; }
    public String getWebhookUrl() { return webhookUrl; }
    public String getWebhookSecret() { return webhookSecret; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public static MerchantConfig restore(String merchantId, String webhookUrl, String webhookSecret, LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new MerchantConfig(merchantId, webhookUrl, webhookSecret, createdAt, updatedAt);
    }
}
