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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/admin/merchants")
@Tag(name = "Administración de API Keys 🔐", description = "Endpoints de uso interno (Back-Office) para emitir y revocar credenciales de comercios.")
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
    @Operation(summary = "Crear nueva API Key", description = "Genera una clave criptográfica de un solo uso para un comercio nuevo.")
    public ResponseEntity<String> createKey(@PathVariable("merchantId") String merchantId){
        String newRawKey = createApiKeyUseCase.execute(merchantId);
        return ResponseEntity.ok(newRawKey);
    }

    @DeleteMapping("/{merchantId}/keys/{keyId}")
    @Operation(summary = "Revocar API Key", description = "Invalida permanentemente una llave para evitar accesos futuros o en caso de filtración.")
    public void deleteKey(@PathVariable("merchantId") String merchantId, @PathVariable("keyId") UUID keyId){
        revokeApiKeyUseCase.execute(merchantId, keyId);
    }

    @PatchMapping("/{merchantId}/keys/{keyId}/rotate")
    @Operation(summary = "Rotar API Key", description = "Emite una llave nueva y programa la caducidad (grace period) de la llave anterior.")
    public ResponseEntity<String> rotateKey(@PathVariable("merchantId") String merchantId, @PathVariable("keyId") UUID keyId){
        String newKey = rotateApiKeyUseCase.execute(merchantId, keyId);
        return ResponseEntity.ok(newKey);
    }
}
