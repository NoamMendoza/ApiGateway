package com.apigatewaypagos.demo.application.usecase;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RotateApiKeyUseCase {
    private static final Logger log = LoggerFactory.getLogger(RotateApiKeyUseCase.class);

    private final CreateApiKeyUseCase createApiKeyUseCase;
    private final RevokeApiKeyUseCase revokeApiKeyUseCase;

    public RotateApiKeyUseCase(CreateApiKeyUseCase createApiKeyUseCase, RevokeApiKeyUseCase revokeApiKeyUseCase){
        this.createApiKeyUseCase = createApiKeyUseCase;
        this.revokeApiKeyUseCase = revokeApiKeyUseCase;
    }

    public String execute(String merchantId, UUID oldKeyId){
        revokeApiKeyUseCase.execute(merchantId, oldKeyId);
        log.info("Api Key revocada");
        return createApiKeyUseCase.execute(merchantId);
    }

}
