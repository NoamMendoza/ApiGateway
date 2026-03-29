package com.apigatewaypagos.demo.application.usecase;

import java.time.LocalDateTime;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.apigatewaypagos.demo.domain.model.ApiKey;
import com.apigatewaypagos.demo.domain.repository.ApiKeyRepository;

@Service
public class CreateApiKeyUseCase {

    private static final Logger log = LoggerFactory.getLogger(CreateApiKeyUseCase.class);

    private final ApiKeyRepository apiKeyRepository;

    public CreateApiKeyUseCase(ApiKeyRepository apiKeyRepository){
        this.apiKeyRepository = apiKeyRepository;
    }

    public String execute(String merchantId){
        String secret = UUID.randomUUID().toString().replace("-", "");
        String rawKey = "sk_live_" + secret;
        String prefix = rawKey.substring(0, 8);
        String hashedKey = org.apache.commons.codec.digest.DigestUtils.sha256Hex(rawKey);

        ApiKey newkey = new ApiKey(
            UUID.randomUUID(),
            merchantId,
            prefix,
            hashedKey,
            true,
            LocalDateTime.now(),
            LocalDateTime.now().plusYears(1)
        );

        apiKeyRepository.save(newkey);

        log.info("Nueva API Key generada para el merchantId: {}", merchantId);

        return rawKey;
    }

}
