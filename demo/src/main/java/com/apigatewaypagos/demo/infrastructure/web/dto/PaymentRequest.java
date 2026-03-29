package com.apigatewaypagos.demo.infrastructure.web.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record PaymentRequest(
    @NotBlank(message = "El token de tarjeta es obligatorio (ej. pm_card_visa)")
    String paymentMethodToken,
    
    @NotNull(message = "El monto es obligatorio")
    @Positive(message = "El monto debe ser mayor a 0")
    BigDecimal amount,
    
    @NotBlank(message = "La moneda es obligatoria")
    @Size(min = 3, max = 3, message = "La moneda debe tener exactamente 3 caracteres")
    String currency
) {}
