package com.apigatewaypagos.demo.infrastructure.web.controller;

import org.springframework.http.ResponseEntity;
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

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    
    private final ProcessPaymentUseCase processPaymentUseCase;
    private final GetPaymentUseCase getPaymentUseCase;

    public PaymentController(ProcessPaymentUseCase processPaymentUseCase, GetPaymentUseCase getPaymentUseCase) {
        this.processPaymentUseCase = processPaymentUseCase;
        this.getPaymentUseCase = getPaymentUseCase;
    }

    @PostMapping
    public ResponseEntity<String> processPayment(@RequestHeader("Idempotency-key") String idempotencyKey, @Valid @RequestBody PaymentRequest request) {
        ProcessPaymentCommand command = new ProcessPaymentCommand(idempotencyKey, request.merchantId(), request.amount(), request.currency());
        processPaymentUseCase.execute(command);
        return ResponseEntity.ok("Pago Procesado");
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponseDTO> getPaymentStatus(@PathVariable UUID id) {
        PaymentResponseDTO response = getPaymentUseCase.execute(id);
        return ResponseEntity.ok(response);
    }
}
