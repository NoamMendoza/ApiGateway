package com.apigatewaypagos.demo.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.apigatewaypagos.demo.domain.model.Payment;

public record PaymentResponseDTO(
    UUID id,
    String merchantId,
    BigDecimal amount,
    String currency,
    String status,
    LocalDateTime createdAt,
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
