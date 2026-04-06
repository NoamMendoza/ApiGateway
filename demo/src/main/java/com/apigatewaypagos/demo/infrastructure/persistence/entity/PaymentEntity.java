package com.apigatewaypagos.demo.infrastructure.persistence.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.apigatewaypagos.demo.domain.model.PaymentStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)

public class PaymentEntity {
    @Id
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String idempotencyKey;

    @Column(name = "merchant_id")
    private String merchantId;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;

    private BigDecimal amount;
    private String currency;
    @Enumerated(EnumType.STRING) private PaymentStatus status;
    private String paymentMethodToken;
    
    @Column(name = "external_transaction_id")
    private String externalTransactionId;

    public static PaymentEntity fromDomain(com.apigatewaypagos.demo.domain.model.Payment payment){
        PaymentEntity entity = new PaymentEntity();
        entity.setId(payment.getId());
        entity.setMerchantId(payment.getMerchantId());
        if (payment.getAmount() != null) {
            entity.setAmount(payment.getAmount().amount());
            entity.setCurrency(payment.getAmount().currency());
        }
        entity.setStatus(payment.getStatus());
        entity.setIdempotencyKey(payment.getIdempotencyKey());
        entity.setPaymentMethodToken(payment.getPaymentMethodToken());
        entity.setExternalTransactionId(payment.getExternalTransactionId());
        entity.setCreatedAt(payment.getCreatedAt());
        entity.setProcessedAt(payment.getProcessedAt());
        return entity;
    }

    public com.apigatewaypagos.demo.domain.model.Payment toDomain(){
        return com.apigatewaypagos.demo.domain.model.Payment.restore(
            this.id,
            this.idempotencyKey,
            this.merchantId,
            new com.apigatewaypagos.demo.domain.model.Money(this.amount, this.currency),
            this.status,
            this.createdAt,
            this.processedAt,
            this.paymentMethodToken,
            this.externalTransactionId
        );
    }

}

