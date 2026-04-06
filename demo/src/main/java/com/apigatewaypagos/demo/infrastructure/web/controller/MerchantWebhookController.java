package com.apigatewaypagos.demo.infrastructure.web.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.apigatewaypagos.demo.application.usecase.SetWebhookUrlUseCase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/admin/merchants/{merchantId}/webhook")
@Tag(name = "Merchants Admin", description = "Operaciones administrativas para clientes y comercios")
public class MerchantWebhookController {

    private final SetWebhookUrlUseCase setWebhookUrlUseCase;

    public MerchantWebhookController(SetWebhookUrlUseCase setWebhookUrlUseCase) {
        this.setWebhookUrlUseCase = setWebhookUrlUseCase;
    }

    @PutMapping
    @Operation(summary = "Configurar recepción de notificaciones", description = "Establece la URL a donde la pasarela dirigirá los cobros como PUSH events.")
    public ResponseEntity<Map<String, String>> setWebhookUrl(
        @PathVariable String merchantId, 
        @RequestBody Map<String, String> request
    ) {
        String url = request.get("webhookUrl");
        if (url == null || url.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "webhookUrl es requerido en el body JSON."));
        }

        try {
            setWebhookUrlUseCase.execute(merchantId, url);
            return ResponseEntity.ok(Map.of(
                "merchantId", merchantId,
                "webhookUrl", url,
                "status", "Configurado exitosamente"
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
