package com.apigatewaypagos.demo.application.usecase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.apigatewaypagos.demo.application.port.in.ProcessPaymentCommand;
import com.apigatewaypagos.demo.application.port.in.ProcessPaymentUseCase;
import com.apigatewaypagos.demo.application.port.out.BankGatewayPort;
import com.apigatewaypagos.demo.application.port.out.EventPublisherPort;
import com.apigatewaypagos.demo.domain.exception.InvalidPaymentStateException;
import com.apigatewaypagos.demo.domain.model.Money;
import com.apigatewaypagos.demo.domain.model.Payment;
import com.apigatewaypagos.demo.domain.repository.PaymentRepository;

@Service
public class ProcessPaymentInteractor implements ProcessPaymentUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProcessPaymentInteractor.class);

    private final PaymentRepository paymentRepository;
    private final BankGatewayPort bankGatewayPort;
    private final EventPublisherPort eventPublisherPort;

    public ProcessPaymentInteractor(PaymentRepository paymentRepository,
                                    BankGatewayPort bankGatewayPort,
                                    EventPublisherPort eventPublisherPort) {
        this.paymentRepository = paymentRepository;
        this.bankGatewayPort = bankGatewayPort;
        this.eventPublisherPort = eventPublisherPort;
    }

    @Override
    public void execute(ProcessPaymentCommand command) {
        log.info("Iniciando procesamiento de pago — merchant={}, idempotencyKey={}",
                command.merchantId(), command.idempotencyKey());

        var existingPayment = paymentRepository.findByIdempotencyKey(command.idempotencyKey());
        if (existingPayment.isPresent()) {
            log.warn("Pago duplicado detectado — idempotencyKey={}", command.idempotencyKey());
            throw new InvalidPaymentStateException("Transaccion ya fue procesada anteriormente");
        }

        Money money = new Money(command.amount(), command.currency());
        Payment payment = new Payment(command.idempotencyKey(), command.merchantId(), money, command.paymentMethodToken());
        paymentRepository.save(payment);
        payment.authorize();
        log.debug("Pago creado y autorizado — paymentId={}", payment.getId());

        boolean isSuccess = bankGatewayPort.process(payment);

        if (isSuccess) {
            payment.capture();
            log.info("Pago capturado exitosamente — paymentId={}, amount={} {}",
                    payment.getId(), payment.getAmount().amount(), payment.getAmount().currency());
        } else {
            payment.decline();
            log.warn("Pago declinado por el banco — paymentId={}", payment.getId());
        }

        paymentRepository.save(payment);
        eventPublisherPort.publishPaymentCompletedEvent(payment);
        log.info("Evento publicado a RabbitMQ — paymentId={}, status={}", payment.getId(), payment.getStatus());
    }
}
