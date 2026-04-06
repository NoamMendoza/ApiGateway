package com.apigatewaypagos.demo.domain.repository;

import java.util.Optional;
import java.util.UUID;

import com.apigatewaypagos.demo.domain.model.Payment;

public interface PaymentRepository {
    void save(Payment payment);

    Optional<Payment> findById(UUID id);
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
    Optional<Payment> findByExternalTransactionId(String externalTransactionId);
    java.util.List<Payment> findByMerchantId(
        String merchantId, 
        String paymentId,
        com.apigatewaypagos.demo.domain.model.PaymentStatus status,
        java.time.LocalDateTime startDate,
        java.time.LocalDateTime endDate,
        java.math.BigDecimal minAmount,
        java.math.BigDecimal maxAmount,
        int page, 
        int size
    );
}
