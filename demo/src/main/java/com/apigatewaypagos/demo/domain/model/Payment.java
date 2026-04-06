package com.apigatewaypagos.demo.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

import com.apigatewaypagos.demo.domain.exception.InvalidPaymentStateException;

public class Payment {
    private UUID id;
    private String merchantId;
    private Money amount;
    private PaymentStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
    private String idempotencyKey;
    private String paymentMethodToken;
    private String externalTransactionId;

    protected Payment() {}

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

    private Payment(UUID id, String idempotencyKey, String merchantId, Money amount, PaymentStatus status, LocalDateTime createdAt, LocalDateTime processedAt, String paymentMethodToken, String externalTransactionId){
        this.id = id;
        this.idempotencyKey = idempotencyKey;
        this.merchantId = merchantId;
        this.amount = amount;
        this.status = status;
        this.createdAt = createdAt;
        this.processedAt = processedAt;
        this.paymentMethodToken = paymentMethodToken;
        this.externalTransactionId = externalTransactionId;
    }

    public static Payment restore(UUID id, String idempotencyKey, String merchantId, Money amount, PaymentStatus status, LocalDateTime createdAt, LocalDateTime processedAt, String paymentMethodToken, String externalTransactionId){
        return new Payment(id, idempotencyKey, merchantId, amount, status, createdAt, processedAt, paymentMethodToken, externalTransactionId);
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
    

    public void refund(){
        if(this.status != PaymentStatus.CAPTURED){
            throw new InvalidPaymentStateException("Solo pagos previamente capturados (exitosos) pueden ser reembolsados");
        }
        this.status = PaymentStatus.REFUNDED;
        this.processedAt = LocalDateTime.now();
    }
    
    public void setExternalTransactionId(String externalId) {
        this.externalTransactionId = externalId;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public UUID getId(){return id;}
    public String getMerchantId(){return merchantId;}
    public Money getAmount(){return amount;}
    public PaymentStatus getStatus(){return status;}
    public String getIdempotencyKey(){return idempotencyKey;}
    public LocalDateTime getCreatedAt(){return createdAt;}
    public LocalDateTime getProcessedAt(){return processedAt;}
    public String getPaymentMethodToken(){return paymentMethodToken;}
    public String getExternalTransactionId(){return externalTransactionId;}
    
}