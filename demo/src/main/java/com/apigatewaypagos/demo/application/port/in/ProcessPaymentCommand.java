package com.apigatewaypagos.demo.application.port.in;

import java.math.BigDecimal;

public record ProcessPaymentCommand(
    String idempotencyKey,
    String merchantId,
    BigDecimal amount,
    String currency,
    String paymentMethodToken
) {}
