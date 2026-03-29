package com.apigatewaypagos.demo.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.apigatewaypagos.demo.domain.model.Payment;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Representación detallada de una transacción bancaria finalizada o en proceso")
public record PaymentResponseDTO(
    @Schema(description = "Identificador único universal del pago", example = "550e8400-e29b-41d4-a716-446655440000")
    UUID id,
    
    @Schema(description = "Comercio asociado a la transacción", example = "micasa_mx")
    String merchantId,
    
    @Schema(description = "Monto cobrado", example = "150.50")
    BigDecimal amount,
    
    @Schema(description = "Moneda del cobro", example = "MXN")
    String currency,
    
    @Schema(description = "Estado actual del cobro en Stripe", example = "CAPTURED")
    String status,
    
    @Schema(description = "Fecha de creación del intento de pago")
    LocalDateTime createdAt,
    
    @Schema(description = "Fecha exacta en que el banco emisor resolvió el cobro")
    LocalDateTime processedAt
) {
    public static PaymentResponseDTO fromDomain(Payment payment) {
        return new PaymentResponseDTO(
            payment.getId(),
            payment.getMerchantId(),
            payment.getAmount() != null ? payment.getAmount().amount() : null,
            payment.getAmount() != null ? payment.getAmount().currency() : null,
            payment.getStatus().name(),
            payment.getCreatedAt(),
            payment.getProcessedAt()
        );
    }
}
