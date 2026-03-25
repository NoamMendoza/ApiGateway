package com.apigatewaypagos.demo.infrastructure.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.apigatewaypagos.demo.application.port.out.EventPublisherPort;
import com.apigatewaypagos.demo.domain.model.Payment;

@Component
public class RabbitMqEventPublisherAdapter implements EventPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(RabbitMqEventPublisherAdapter.class);

    private final RabbitTemplate rabbitTemplate;

    public RabbitMqEventPublisherAdapter(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publishPaymentCompletedEvent(Payment payment) {
        log.info("Publicando evento PaymentCompleted — paymentId={}, status={}",
                payment.getId(), payment.getStatus());

        rabbitTemplate.convertAndSend(
            RabbitMqConfig.EXCHANGE_NAME,
            "payment.completed.success",
            payment
        );

        log.debug("Evento enviado exitosamente a exchange={}, routingKey=payment.completed.success",
                RabbitMqConfig.EXCHANGE_NAME);
    }
}
