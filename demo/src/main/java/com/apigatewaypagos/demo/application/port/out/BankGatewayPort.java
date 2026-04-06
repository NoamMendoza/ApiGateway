package com.apigatewaypagos.demo.application.port.out;

import com.apigatewaypagos.demo.domain.model.Payment;

public interface BankGatewayPort {
    public PaymentGatewayResult process(Payment payment);
    public PaymentGatewayResult refund(Payment payment);
}
