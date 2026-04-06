package com.apigatewaypagos.demo.infrastructure;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.apigatewaypagos.demo.infrastructure.persistence.entity.PaymentEntity;


import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SpringDataPaymentRepository extends JpaRepository<PaymentEntity, UUID>, JpaSpecificationExecutor<PaymentEntity> {
    java.util.Optional<PaymentEntity> findByIdempotencyKey(String idempotencyKey);
    java.util.Optional<PaymentEntity> findByExternalTransactionId(String externalTransactionId);
    Page<PaymentEntity> findByMerchantId(String merchantId, Pageable pageable);

    @Query("SELECT p.status, COUNT(p) FROM PaymentEntity p WHERE p.merchantId = :merchantId GROUP BY p.status")
    List<Object[]> countByStatus(@Param("merchantId") String merchantId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM PaymentEntity p WHERE p.merchantId = :merchantId AND p.status = 'CAPTURED' AND p.processedAt >= :startOfDay")
    BigDecimal sumCapturedAmountSince(@Param("merchantId") String merchantId, @Param("startOfDay") LocalDateTime startOfDay);

    @Query("SELECT COUNT(p) FROM PaymentEntity p WHERE p.merchantId = :merchantId AND p.createdAt >= :startOfDay")
    long countCreatedSince(@Param("merchantId") String merchantId, @Param("startOfDay") LocalDateTime startOfDay);

}
