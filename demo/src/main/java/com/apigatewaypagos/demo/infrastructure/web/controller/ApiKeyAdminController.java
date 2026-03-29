package com.apigatewaypagos.demo.infrastructure.web.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.apigatewaypagos.demo.application.usecase.CreateApiKeyUseCase;
import com.apigatewaypagos.demo.application.usecase.RevokeApiKeyUseCase;
import com.apigatewaypagos.demo.application.usecase.RotateApiKeyUseCase;

@RestController
@RequestMapping("/api/admin/merchants")
public class ApiKeyAdminController {

    private final CreateApiKeyUseCase createApiKeyUseCase;
    private final RevokeApiKeyUseCase revokeApiKeyUseCase;
    private final RotateApiKeyUseCase rotateApiKeyUseCase;

    public ApiKeyAdminController(CreateApiKeyUseCase createApiKeyUseCase, RevokeApiKeyUseCase revokeApiKeyUseCase, RotateApiKeyUseCase rotateApiKeyUseCase){
        this.createApiKeyUseCase = createApiKeyUseCase;
        this.revokeApiKeyUseCase = revokeApiKeyUseCase;
        this.rotateApiKeyUseCase = rotateApiKeyUseCase;
    }

    @PostMapping("/{merchantId}/keys")
    public ResponseEntity<String> createKey(@PathVariable String merchantId){
        String newRawKey = createApiKeyUseCase.execute(merchantId);
        return ResponseEntity.ok(newRawKey);
    }

    @DeleteMapping("/{merchantId}/keys/{keyId}")
    public void deleteKey(@PathVariable String merchantId, @PathVariable UUID keyId){
        revokeApiKeyUseCase.execute(merchantId, keyId);
    }

    @PatchMapping("/{merchantId}/keys/{keyId}/rotate")
    public ResponseEntity<String> rotateKey(@PathVariable String merchantId, @PathVariable UUID keyId){
        String newKey = rotateApiKeyUseCase.execute(merchantId, keyId);
        return ResponseEntity.ok(newKey);
    }
}
