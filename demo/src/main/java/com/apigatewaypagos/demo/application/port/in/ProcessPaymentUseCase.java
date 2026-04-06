package com.apigatewaypagos.demo.application.port.in;

import com.apigatewaypagos.demo.domain.model.Payment;

public interface ProcessPaymentUseCase {
    Payment execute(ProcessPaymentCommand command);
}
