package com.apigatewaypagos.demo.infrastructure.web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.apigatewaypagos.demo.application.dto.PaymentResponseDTO;
import com.apigatewaypagos.demo.application.port.in.ProcessPaymentCommand;
import com.apigatewaypagos.demo.application.port.in.ProcessPaymentUseCase;
import com.apigatewaypagos.demo.application.usecase.GetPaymentUseCase;
import com.apigatewaypagos.demo.infrastructure.web.dto.PaymentRequest;

import jakarta.validation.Valid;
import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/payments")
@Tag(name = "Pagos 💵", description = "Endpoints para procesar transacciones bancarias (B2B).")
public class PaymentController {
    
    private final ProcessPaymentUseCase processPaymentUseCase;
    private final GetPaymentUseCase getPaymentUseCase;

    public PaymentController(ProcessPaymentUseCase processPaymentUseCase, GetPaymentUseCase getPaymentUseCase) {
        this.processPaymentUseCase = processPaymentUseCase;
        this.getPaymentUseCase = getPaymentUseCase;
    }

    @PostMapping
    @Operation(
        summary = "Procesar pago de tarjeta",
        description = "Envía la petición del comercio hacia Stripe de forma asíncrona validando la integridad con un UUID de idempotencia.",
        security = @SecurityRequirement(name = "ApiKey")
    )
    public ResponseEntity<String> processPayment(
            @RequestHeader("Idempotency-key") String idempotencyKey, 
            @Valid @RequestBody PaymentRequest request) {
       String merchantId = SecurityContextHolder.getContext().getAuthentication().getName();
        ProcessPaymentCommand command = new ProcessPaymentCommand(
            idempotencyKey,
            merchantId,
            request.amount(),
            request.currency(),
            request.paymentMethodToken()
        );
        processPaymentUseCase.execute(command);
        return ResponseEntity.ok("Pago Procesado");
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Consultar el estado de un pago",
        description = "Obtén el resumen financiero de una transacción mediante su identificador UUID único.",
        security = @SecurityRequirement(name = "ApiKey")
    )
    public ResponseEntity<PaymentResponseDTO> getPaymentStatus(@PathVariable("id") UUID id) {
        PaymentResponseDTO response = getPaymentUseCase.execute(id);
        return ResponseEntity.ok(response);
    }
}
