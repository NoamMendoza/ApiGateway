package com.apigatewaypagos.demo.application.port.out;

import com.apigatewaypagos.demo.domain.model.Payment;

public interface EventPublisherPort {
    void publishPaymentCompletedEvent(Payment payment);
}
