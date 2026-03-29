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
import com.stripe.param.PaymentIntentCreateParams;

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
    public boolean process(Payment payment){
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
                return true;
            } else {
                log.warn("El pago quedó en un estado extraño: {}", intent.getStatus());
                return false;
            }
        } catch (StripeException  e) {
            log.error("Stripe declinó el pago o hubo un error — motivo={}", e.getMessage());
            return false;
        }
    }
    public boolean bankFallback(Payment payment, Exception ex) {
        log.error("Stripe no disponible (Fallback Activado) — paymentId={}, motivo={}", payment.getId(), ex.getMessage());
        return false;
    }
}
