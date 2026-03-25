package com.apigatewaypagos.demo.domain.model;

import java.math.BigDecimal;

public record Money(BigDecimal amount, String currency) {
    public Money{
        if(amount == null || amount.compareTo(BigDecimal.ZERO) < 0){
            throw new IllegalArgumentException("El monto no puede ser negativo o nulo");
        }
        if(currency == null || currency.isBlank() || currency.length() != 3){
            throw new IllegalArgumentException("La moneda debe ser un código válido de 3 letras (ej. MXN, USD)");
        }
    }
        
}
