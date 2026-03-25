package com.apigatewaypagos.demo.infrastructure.bank;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.apigatewaypagos.demo.application.port.out.BankGatewayPort;
import com.apigatewaypagos.demo.domain.model.Payment;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

@Component
public class BankGatewayAdapter implements BankGatewayPort {

    private static final Logger log = LoggerFactory.getLogger(BankGatewayAdapter.class);

    @Override
    @CircuitBreaker(name = "bankGateway", fallbackMethod = "bankFallback")
    @Retry(name = "bankGateway")
    public boolean process(Payment payment) {
        log.info("Enviando pago al banco — paymentId={}, amount={} {}",
                payment.getId(), payment.getAmount().amount(), payment.getAmount().currency());
        try {
            // Aquí irá la llamada real al banco (Stripe, Conekta, etc.)
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log.info("Banco respondió exitosamente — paymentId={}", payment.getId());
        return true;
    }

    /**
     * Fallback del Circuit Breaker.
     * Se invoca cuando el banco falla repetidamente o el circuit breaker está abierto.
     */
    public boolean bankFallback(Payment payment, Exception ex) {
        log.error("Banco no disponible — paymentId={}, motivo={}", payment.getId(), ex.getMessage());
        return false;
    }
}
