package com.apigatewaypagos.demo.application.port.in;

public interface ProcessPaymentUseCase {
    void execute(ProcessPaymentCommand command);
}
