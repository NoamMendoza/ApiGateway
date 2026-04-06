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
    public Payment execute(ProcessPaymentCommand command) {
        log.info("Iniciando procesamiento de pago — merchant={}, idempotencyKey={}",
                command.merchantId(), command.idempotencyKey());

        var existingPaymentOpt = paymentRepository.findByIdempotencyKey(command.idempotencyKey());
        if (existingPaymentOpt.isPresent()) {
            Payment existing = existingPaymentOpt.get();
            if (isSameRequest(existing, command)) {
                log.info("Reintento de pago detectado (Smart Idempotency) — devolviendo resultado previo.");
                return existing;
            } else {
                log.warn("Conflicto de Idempotencia — La llave ya existe con diferentes parámetros.");
                throw new InvalidPaymentStateException("La llave de idempotencia ya ha sido utilizada para una transacción diferente.");
            }
        }

        Money money = new Money(command.amount(), command.currency());
        Payment payment = new Payment(command.idempotencyKey(), command.merchantId(), money, command.paymentMethodToken());
        paymentRepository.save(payment);
        payment.authorize();
        log.debug("Pago creado y autorizado — paymentId={}", payment.getId());

        com.apigatewaypagos.demo.application.port.out.PaymentGatewayResult result = bankGatewayPort.process(payment);

        if (result.isSuccess()) {
            payment.capture();
            payment.setExternalTransactionId(result.externalId());
            log.info("Pago capturado exitosamente — paymentId={}, amount={} {}, externalId={}",
                    payment.getId(), payment.getAmount().amount(), payment.getAmount().currency(), payment.getExternalTransactionId());
        } else {
            payment.decline();
            log.warn("Pago declinado por el banco — paymentId={}, error={}", payment.getId(), result.errorMessage());
        }

        paymentRepository.save(payment);
        eventPublisherPort.publishPaymentCompletedEvent(payment);
        log.info("Evento publicado a RabbitMQ — paymentId={}, status={}", payment.getId(), payment.getStatus());
        
        return payment;
    }

    private boolean isSameRequest(Payment payment, ProcessPaymentCommand command) {
        return payment.getMerchantId().equals(command.merchantId()) &&
               payment.getAmount().amount().compareTo(command.amount()) == 0 &&
               payment.getAmount().currency().equalsIgnoreCase(command.currency());
    }
}
