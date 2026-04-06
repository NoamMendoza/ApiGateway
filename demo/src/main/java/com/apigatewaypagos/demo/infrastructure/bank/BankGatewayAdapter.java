package com.apigatewaypagos.demo.infrastructure.bank;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.apigatewaypagos.demo.application.port.out.BankGatewayPort;
import com.apigatewaypagos.demo.domain.model.Payment;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import com.apigatewaypagos.demo.application.port.out.PaymentGatewayResult;

import java.math.BigDecimal;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

@Component
public class BankGatewayAdapter implements BankGatewayPort {

    private static final Logger log = LoggerFactory.getLogger(BankGatewayAdapter.class);

    public BankGatewayAdapter(@Value("${stripe.api.key}") String stripeApiKey){
        Stripe.apiKey = stripeApiKey;
    }

    @Override
    @CircuitBreaker(name = "bankGateway", fallbackMethod = "bankFallback")
    @Retry(name = "bankGateway")
    public PaymentGatewayResult process(Payment payment){
        log.info("Enviando pago a STRIPE — paymentId={}, amount={} {}, token={}",
                payment.getId(), payment.getAmount().amount(), payment.getAmount().currency(), payment.getPaymentMethodToken());

        try {
            long amountInCents = payment.getAmount().amount().multiply(new BigDecimal("100")).longValue();

             PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amountInCents)
                .setCurrency(payment.getAmount().currency().toLowerCase())
                .setPaymentMethod(payment.getPaymentMethodToken())
                .setConfirm(true)
                .setReturnUrl("https://localhost:8443/api/payments/callback")
                .build();

            PaymentIntent intent = PaymentIntent.create(params);
            log.info("Stripe respondió exitosamente — intentId={}, status={}", intent.getId(), intent.getStatus());

            if ("succeeded".equals(intent.getStatus()) || "requires_capture".equals(intent.getStatus())) {
                return PaymentGatewayResult.success(intent.getId());
            } else {
                log.warn("El pago quedó en un estado extraño: {}", intent.getStatus());
                return PaymentGatewayResult.failure("Status ambiguo en Stripe: " + intent.getStatus());
            }
        } catch (StripeException  e) {
            log.error("Stripe declinó el pago o hubo un error — motivo={}", e.getMessage());
            return PaymentGatewayResult.failure(e.getMessage());
        }
    }
    
    @Override
    @CircuitBreaker(name = "bankGateway", fallbackMethod = "bankFallback")
    @Retry(name = "bankGateway")
    public PaymentGatewayResult refund(Payment payment){
        log.info("Solicitando reembolso a STRIPE — paymentId={}, intentId={}", payment.getId(), payment.getExternalTransactionId());

        if (payment.getExternalTransactionId() == null) {
            return PaymentGatewayResult.failure("El pago no cuenta con un ID de transacción externo para reembolsar.");
        }

        try {
            RefundCreateParams params = RefundCreateParams.builder()
                .setPaymentIntent(payment.getExternalTransactionId())
                .build();

            Refund refund = Refund.create(params);
            log.info("Stripe procesó reembolso — refundId={}, status={}", refund.getId(), refund.getStatus());

            if ("succeeded".equals(refund.getStatus())) {
                return PaymentGatewayResult.success(refund.getId());
            } else {
                log.warn("El reembolso quedó en un estado extraño: {}", refund.getStatus());
                return PaymentGatewayResult.failure("Status de Reembolso ambiguo: " + refund.getStatus());
            }
        } catch (StripeException e) {
            log.error("Stripe declinó el reembolso — motivo={}", e.getMessage());
            return PaymentGatewayResult.failure(e.getMessage());
        }
    }

    public PaymentGatewayResult bankFallback(Payment payment, Exception ex) {
        log.error("Stripe no disponible (Fallback Activado) — paymentId={}, motivo={}", payment.getId(), ex.getMessage());
        return PaymentGatewayResult.failure("Servicio bancario no disponible localmente.");
    }
}
