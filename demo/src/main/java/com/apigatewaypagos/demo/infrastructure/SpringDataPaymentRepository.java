package com.apigatewaypagos.demo.infrastructure;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.apigatewaypagos.demo.infrastructure.persistence.entity.PaymentEntity;

public interface SpringDataPaymentRepository extends JpaRepository<PaymentEntity, UUID> {
    java.util.Optional<PaymentEntity> findByIdempotencyKey(String idempotencyKey);
}
