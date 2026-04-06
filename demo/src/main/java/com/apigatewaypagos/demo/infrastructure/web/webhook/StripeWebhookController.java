package com.apigatewaypagos.demo.infrastructure.web.webhook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/webhooks")
@Tag(name = "Webhooks Entrantes 📨", description = "Endpoints para recibir notificaciones asíncronas de Stripe sobre cambios en el estado de pagos.")
public class StripeWebhookController {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookController.class);

    private final StripeWebhookService stripeWebhookService;
    private final String webhookSecret;

    public StripeWebhookController(StripeWebhookService stripeWebhookService,
                                   @Value("${stripe.webhook.secret}") String webhookSecret) {
        this.stripeWebhookService = stripeWebhookService;
        this.webhookSecret = webhookSecret;
    }

    @PostMapping("/stripe")
    @Operation(
        summary = "Receptor de eventos de Stripe",
        description = "Recibe y valida eventos firmados (HMAC-SHA256) de Stripe: disputes, payment failures, etc."
    )
    public ResponseEntity<String> handleStripeEvent(
            @RequestBody byte[] payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        Event event;
        try {
            event = Webhook.constructEvent(new String(payload), sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.warn("Stripe Webhook: Firma inválida — puede ser un intento de spoofing. Motivo: {}", e.getMessage());
            return ResponseEntity.status(400).body("Firma inválida");
        }

        log.info("Evento Stripe recibido — type={}, id={}", event.getType(), event.getId());

        stripeWebhookService.handle(event);

        return ResponseEntity.ok("Evento recibido");
    }
}
