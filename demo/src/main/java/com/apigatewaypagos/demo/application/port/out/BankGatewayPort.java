package com.apigatewaypagos.demo.application.port.out;

import com.apigatewaypagos.demo.domain.model.Payment;

public interface BankGatewayPort {
    public boolean process(Payment payment);
}
