package com.apigatewaypagos.demo.application.port.in;


import java.math.BigDecimal;

public record ProcessPaymentCommand (String idempotencyKey, String merchantId, BigDecimal amount, String currency){
    public ProcessPaymentCommand{
        if(idempotencyKey == null || idempotencyKey.isBlank()){
            throw new IllegalArgumentException("La cabecera Idempotency-Key es obligatoria");
        }
        if(merchantId == null || merchantId.isBlank()){
            throw new IllegalArgumentException("El merchantId no puede estar vacío");
        }

        if(amount == null || amount.compareTo(BigDecimal.ZERO) <= 0){
            throw new IllegalArgumentException("El monto a cobrar debe ser mayor a 0");
        }

        if(currency == null || currency.isBlank() || currency.length() != 3){
            throw new IllegalArgumentException("La moneda debe ser un código válido de 3 letras (ej. MXN, USD)");
        }
    }
    
}
