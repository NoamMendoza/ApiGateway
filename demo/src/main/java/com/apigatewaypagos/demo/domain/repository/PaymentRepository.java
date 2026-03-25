package com.apigatewaypagos.demo.domain.repository;

import java.util.Optional;
import java.util.UUID;

import com.apigatewaypagos.demo.domain.model.Payment;

public interface PaymentRepository {
    void save(Payment payment);

    Optional<Payment> findById(UUID id);
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
}
