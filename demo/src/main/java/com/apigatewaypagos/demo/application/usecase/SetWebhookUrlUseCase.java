package com.apigatewaypagos.demo.application.usecase;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.apigatewaypagos.demo.domain.model.MerchantConfig;
import com.apigatewaypagos.demo.domain.repository.MerchantConfigRepository;

@Service
public class SetWebhookUrlUseCase {

    private static final Logger log = LoggerFactory.getLogger(SetWebhookUrlUseCase.class);

    private final MerchantConfigRepository merchantConfigRepository;

    public SetWebhookUrlUseCase(MerchantConfigRepository merchantConfigRepository) {
        this.merchantConfigRepository = merchantConfigRepository;
    }

    public void execute(String merchantId, String webhookUrl) {
        if (!isValidUrl(webhookUrl)) {
            throw new IllegalArgumentException("La URL provista no es un Webhook Endpoint válido (debe ser HTTP/HTTPS).");
        }

        MerchantConfig config = merchantConfigRepository.findByMerchantId(merchantId)
            .map(existing -> new MerchantConfig(
                merchantId, 
                webhookUrl, 
                existing.getWebhookSecret() != null ? existing.getWebhookSecret() : generateSecret(),
                existing.getCreatedAt(), 
                LocalDateTime.now()
            ))
            .orElseGet(() -> new MerchantConfig(
                merchantId, 
                webhookUrl, 
                generateSecret(),
                LocalDateTime.now(), 
                LocalDateTime.now()
            ));

        merchantConfigRepository.save(config);
        log.info("Webhook configurado para merchantId '{}'. URL: {} | Secret: *** ", merchantId, webhookUrl);
    }

    private String generateSecret() {
        return java.util.UUID.randomUUID().toString().replace("-", "") + 
               java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private boolean isValidUrl(String url) {
        try {
            URI uri = new URI(url);
            return uri.getScheme() != null && 
                   (uri.getScheme().equalsIgnoreCase("http") || uri.getScheme().equalsIgnoreCase("https")) &&
                   uri.getHost() != null;
        } catch (URISyntaxException e) {
            return false;
        }
    }
}
