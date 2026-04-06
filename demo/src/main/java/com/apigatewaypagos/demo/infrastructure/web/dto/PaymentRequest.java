package com.apigatewaypagos.demo.infrastructure.web.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Modelo de datos que el comercio envía para procesar un pago con Stripe.")
public record PaymentRequest(
    @Schema(description = "Token de tarjeta seguro extraído desde Stripe.js", example = "pm_card_visa")
    @NotBlank(message = "El token de tarjeta es obligatorio (ej. pm_card_visa)")
    String paymentMethodToken,
    
    @Schema(description = "Monto a cobrar con precisión bidimensional", example = "150.50")
    @NotNull(message = "El monto es obligatorio")
    @Positive(message = "El monto debe ser mayor a 0")
    BigDecimal amount,
    
    @Schema(description = "Abreviatura ISO 4217 de la moneda", example = "MXN")
    @NotBlank(message = "La moneda es obligatoria")
    @Size(min = 3, max = 3, message = "La moneda debe tener exactamente 3 caracteres")
    String currency
) {
    @Override
    public String toString() {
        return "PaymentRequest[" +
                "paymentMethodToken=***MASKED***" +
                ", amount=" + amount +
                ", currency='" + currency + '\'' +
                ']';
    }
}
