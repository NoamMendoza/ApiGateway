package com.apigatewaypagos.demo.infrastructure.messaging;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.apigatewaypagos.demo.domain.model.MerchantConfig;
import com.apigatewaypagos.demo.domain.model.Payment;
import com.apigatewaypagos.demo.domain.repository.MerchantConfigRepository;

@Component
public class WebhookEmissionWorker {

    private static final Logger log = LoggerFactory.getLogger(WebhookEmissionWorker.class);

    private final MerchantConfigRepository merchantConfigRepository;
    private final RestClient restClient;

    public WebhookEmissionWorker(MerchantConfigRepository merchantConfigRepository) {
        this.merchantConfigRepository = merchantConfigRepository;
        this.restClient = RestClient.builder().build();
    }

    @RabbitListener(queues = RabbitMqConfig.QUEUE_COMPLETED)
    @Retryable(
        retryFor = {Exception.class}, 
        maxAttempts = 3, 
        backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public void handlePaymentCompleted(Payment payment) {
        log.info("📡 Recibiendo PaymentCompleted en RabbitMQ [paymentId={}] para Webhook...", payment.getId());

        Optional<MerchantConfig> configOpt = merchantConfigRepository.findByMerchantId(payment.getMerchantId());

        if (configOpt.isEmpty()) {
            log.debug("El comercio {} no tiene Webhook configurado. Ignorando silenciosamente.", payment.getMerchantId());
            return;
        }

        String targetUrl = configOpt.get().getWebhookUrl();
        String secret = configOpt.get().getWebhookSecret();

        WebhookPayload payload = new WebhookPayload(
            payment.getId().toString(),
            payment.getMerchantId(),
            payment.getStatus().name(),
            payment.getAmount().amount(),
            payment.getAmount().currency()
        );

        String jsonPayload = serializeToJson(payload);
        String signature = calculateHmacSha256(jsonPayload, secret);

        log.info("Transmitiendo Webhook POST a: {} (Firma: {})", targetUrl, signature.substring(0, 8) + "...");

        try {
            restClient.post()
                .uri(targetUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-PayGateway-Signature", signature)
                .body(jsonPayload)
                .retrieve()
                .toBodilessEntity();
                
            log.info("✅ Webhook entregado exitosamente para paymentId={}", payment.getId());
        } catch (Exception e) {
            log.error("❌ Falló la entrega del Webhook: {} - Se reintentará por defecto vía Spring Retry", e.getMessage());
            throw e;
        }
    }

    private String serializeToJson(WebhookPayload payload) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(payload);
        } catch (Exception e) {
            throw new RuntimeException("Error serializando payload de webhook", e);
        }
    }

    private String calculateHmacSha256(String data, String secret) {
        try {
            if (secret == null) return "unsigned";
            javax.crypto.spec.SecretKeySpec secretKey = new javax.crypto.spec.SecretKeySpec(secret.getBytes(), "HmacSHA256");
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(secretKey);
            byte[] hmacData = mac.doFinal(data.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hmacData) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error calculando HMAC SHA256", e);
        }
    }

    public record WebhookPayload(String paymentId, String merchantId, String status, java.math.BigDecimal amount, String currency) {}
}
