package com.apigatewaypagos.demo.application.usecase;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.apigatewaypagos.demo.application.port.out.BankGatewayPort;
import com.apigatewaypagos.demo.application.port.out.EventPublisherPort;
import com.apigatewaypagos.demo.application.port.out.PaymentGatewayResult;
import com.apigatewaypagos.demo.domain.model.Payment;
import com.apigatewaypagos.demo.domain.repository.PaymentRepository;

@Service
public class RefundPaymentInteractor {

    private static final Logger log = LoggerFactory.getLogger(RefundPaymentInteractor.class);

    private final PaymentRepository paymentRepository;
    private final BankGatewayPort bankGatewayPort;
    private final EventPublisherPort eventPublisherPort;

    public RefundPaymentInteractor(PaymentRepository paymentRepository, BankGatewayPort bankGatewayPort, EventPublisherPort eventPublisherPort) {
        this.paymentRepository = paymentRepository;
        this.bankGatewayPort = bankGatewayPort;
        this.eventPublisherPort = eventPublisherPort;
    }

    public Payment execute(UUID paymentId, String merchantId) {
        log.info("Iniciando solicitud de reembolso para paymentId={}, merchantId={}", paymentId, merchantId);

        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado con ID: " + paymentId));

        if (!payment.getMerchantId().equals(merchantId)) {
            log.error("¡ALERTA DE SEGURIDAD! El comercio {} intentó reembolsar un pago del comercio {}", merchantId, payment.getMerchantId());
            throw new IllegalArgumentException("No tiene permisos para reembolsar este pago.");
        }

        payment.refund(); 

        log.debug("Pago válido para reembolso. Enviando a la pasarela bancaria...");
        PaymentGatewayResult result = bankGatewayPort.refund(payment);

        if (result.isSuccess()) {
            log.info("Reembolso autorizado por el banco. Actualizando BD. paymentId={}", payment.getId());
            paymentRepository.save(payment);
            
            eventPublisherPort.publishPaymentCompletedEvent(payment); 
            log.info("Evento asincrono de reembolso despachado a la red.");
            
            return payment;
        } else {
            log.error("El banco declinó procesar el reembolso — paymentId={}, motivo={}", payment.getId(), result.errorMessage());
            throw new RuntimeException("Reembolso fallido: " + result.errorMessage());
        }
    }
}
