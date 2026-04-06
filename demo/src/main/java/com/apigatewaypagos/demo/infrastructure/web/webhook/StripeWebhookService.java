package com.apigatewaypagos.demo.infrastructure.web.webhook;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.apigatewaypagos.demo.application.port.out.EventPublisherPort;
import com.apigatewaypagos.demo.domain.model.Payment;
import com.apigatewaypagos.demo.domain.model.PaymentStatus;
import com.apigatewaypagos.demo.domain.repository.PaymentRepository;
import com.stripe.model.Dispute;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;

@Service
public class StripeWebhookService {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookService.class);

    private final PaymentRepository paymentRepository;
    private final EventPublisherPort eventPublisherPort;

    public StripeWebhookService(PaymentRepository paymentRepository, EventPublisherPort eventPublisherPort) {
        this.paymentRepository = paymentRepository;
        this.eventPublisherPort = eventPublisherPort;
    }

    @Async
    public void handle(Event event) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        Optional<StripeObject> stripeObjectOpt = deserializer.getObject();

        switch (event.getType()) {

            case "charge.dispute.created" -> {
                if (stripeObjectOpt.isPresent() && stripeObjectOpt.get() instanceof Dispute dispute) {
                    log.warn("DISPUTE RECIBIDO OBJETO RECIENTE: {}", dispute.toJson());
                    String intentId = dispute.getPaymentIntent();
                    String chargeId = dispute.getCharge();
                    log.warn("CONTRACARGO DETECTADO — intentId={}, chargeId={}, motivo={}, monto={}",
                            intentId, chargeId, dispute.getReason(), dispute.getAmount());

                    findPaymentByExternalId(intentId).ifPresentOrElse(
                        payment -> {
                            payment.setStatus(PaymentStatus.DISPUTED);
                            paymentRepository.save(payment);
                            log.warn("Pago marcado como DISPUTED — paymentId={}", payment.getId());

                            eventPublisherPort.publishPaymentCompletedEvent(payment);
                            log.warn("Notificación DISPUTED enviada al comercio — merchantId={}", payment.getMerchantId());
                        },
                        () -> log.warn("Pago con intentId={} no encontrado en BD para marcar como DISPUTED", intentId)
                    );
                }
            }

            case "payment_intent.payment_failed" -> {
                if (stripeObjectOpt.isPresent() && stripeObjectOpt.get() instanceof PaymentIntent intent) {
                    String failureMsg = intent.getLastPaymentError() != null
                            ? intent.getLastPaymentError().getMessage()
                            : "Error desconocido";
                    log.error("Pago fallido asíncrono — intentId={}, motivo={}", intent.getId(), failureMsg);

                    findPaymentByExternalId(intent.getId()).ifPresent(payment -> {
                        payment.setStatus(PaymentStatus.FAILED);
                        paymentRepository.save(payment);
                        log.error("Pago marcado como FAILED — paymentId={}", payment.getId());

                        eventPublisherPort.publishPaymentCompletedEvent(payment);
                        log.error("Notificación FAILED enviada al comercio — merchantId={}", payment.getMerchantId());
                    });
                }
            }

            case "payment_intent.succeeded" -> {
                if (stripeObjectOpt.isPresent() && stripeObjectOpt.get() instanceof PaymentIntent intent) {
                    log.info("Pago asíncrono confirmado por Stripe — intentId={}", intent.getId());
                }
            }

            default -> log.debug("Evento de Stripe no manejado — type={}", event.getType());
        }
    }

    private Optional<Payment> findPaymentByExternalId(String externalId) {
        int maxRetries = 4;
        int delayMs = 500;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            Optional<Payment> result = paymentRepository.findByExternalTransactionId(externalId);
            if (result.isPresent()) {
                return result;
            }
            if (attempt < maxRetries) {
                log.debug("Pago con intentId={} aún no en BD, reintento {}/{} en {}ms...",
                        externalId, attempt, maxRetries, delayMs);
                try { Thread.sleep(delayMs); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                delayMs *= 2;
            }
        }
        return Optional.empty();
    }
}
