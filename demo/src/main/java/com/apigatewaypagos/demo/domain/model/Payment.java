package com.apigatewaypagos.demo.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

import com.apigatewaypagos.demo.domain.exception.InvalidPaymentStateException;

public class Payment {
    private final UUID id;
    private final String merchantId;
    private final Money amount;
    private PaymentStatus status;
    private final LocalDateTime createdAt;
    private LocalDateTime processedAt;
    private final String idempotencyKey;
    private final String paymentMethodToken;

    public Payment(String idempotencyKey, String merchantId, Money amount, String paymentMethodToken){
        this.id = UUID.randomUUID();
        this.merchantId = merchantId;
        this.amount = amount;
        this.status = PaymentStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        if(idempotencyKey == null || idempotencyKey.isBlank()){
            throw new IllegalArgumentException("La llave de idempotencia es obligatoria");
        }
        this.idempotencyKey = idempotencyKey;
        this.paymentMethodToken = paymentMethodToken;
    }

    private Payment(UUID id, String idempotencyKey, String merchantId, Money amount, PaymentStatus status, LocalDateTime createdAt, LocalDateTime processedAt, String paymentMethodToken){
        this.id = id;
        this.idempotencyKey = idempotencyKey;
        this.merchantId = merchantId;
        this.amount = amount;
        this.status = status;
        this.createdAt = createdAt;
        this.processedAt = processedAt;
        this.paymentMethodToken = paymentMethodToken;
    }

    public static Payment restore(UUID id, String idempotencyKey, String merchantId, Money amount, PaymentStatus status, LocalDateTime createdAt, LocalDateTime processedAt, String paymentMethodToken){
        return new Payment(id, idempotencyKey, merchantId, amount, status, createdAt, processedAt, paymentMethodToken);
    }

    public void authorize(){
        if(this.status != PaymentStatus.PENDING){
            throw new InvalidPaymentStateException("Solo pagos pendientes pueden ser autorizados");
        }
        this.status = PaymentStatus.AUTHORIZED;
        this.processedAt = LocalDateTime.now();
    }

    public void capture(){
        if(this.status != PaymentStatus.AUTHORIZED){
            throw new InvalidPaymentStateException("Solo pagos autorizados pueden ser capturados");
        }
        this.status = PaymentStatus.CAPTURED;
        this.processedAt = LocalDateTime.now();
    }

    public void decline(){
        if(this.status == PaymentStatus.CAPTURED){
            throw new InvalidPaymentStateException("No puedes declinar un pago que fue aceptado");
        }
        this.status = PaymentStatus.DECLINED;
        this.processedAt = LocalDateTime.now();
    }
    

    public UUID getId(){return id;}
    public String getMerchantId(){return merchantId;}
    public Money getAmount(){return amount;}
    public PaymentStatus getStatus(){return status;}
    public String getIdempotencyKey(){return idempotencyKey;}
    public LocalDateTime getCreatedAt(){return createdAt;}
    public LocalDateTime getProcessedAt(){return processedAt;}
    public String getPaymentMethodToken(){return paymentMethodToken;}
    
}