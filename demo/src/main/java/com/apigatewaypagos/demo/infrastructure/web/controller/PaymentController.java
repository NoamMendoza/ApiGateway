package com.apigatewaypagos.demo.infrastructure.web.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.apigatewaypagos.demo.application.dto.PaymentResponseDTO;
import com.apigatewaypagos.demo.application.port.in.ProcessPaymentCommand;
import com.apigatewaypagos.demo.application.port.in.ProcessPaymentUseCase;
import com.apigatewaypagos.demo.application.usecase.GetPaymentUseCase;
import com.apigatewaypagos.demo.application.usecase.RefundPaymentInteractor;
import com.apigatewaypagos.demo.domain.repository.PaymentRepository;
import com.apigatewaypagos.demo.infrastructure.web.dto.PaymentRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/payments")
@Tag(name = "Pagos 💵", description = "Endpoints para procesar transacciones bancarias (B2B).")
public class PaymentController {
    
    private final ProcessPaymentUseCase processPaymentUseCase;
    private final GetPaymentUseCase getPaymentUseCase;
    private final RefundPaymentInteractor refundPaymentInteractor;
    private final PaymentRepository paymentRepository;

    public PaymentController(ProcessPaymentUseCase processPaymentUseCase, GetPaymentUseCase getPaymentUseCase,
                             RefundPaymentInteractor refundPaymentInteractor, PaymentRepository paymentRepository) {
        this.processPaymentUseCase = processPaymentUseCase;
        this.getPaymentUseCase = getPaymentUseCase;
        this.refundPaymentInteractor = refundPaymentInteractor;
        this.paymentRepository = paymentRepository;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_CHARGE')")
    @Operation(
        summary = "Procesar pago de tarjeta",
        description = "Envía la petición del comercio hacia Stripe de forma asíncrona validando la integridad con un UUID de idempotencia.",
        security = @SecurityRequirement(name = "ApiKey")
    )
    public ResponseEntity<PaymentResponseDTO> processPayment(
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
        com.apigatewaypagos.demo.domain.model.Payment payment = processPaymentUseCase.execute(command);
        return ResponseEntity.ok(PaymentResponseDTO.fromDomain(payment));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_CHARGE')")
    @Operation(
        summary = "Listar pagos del comercio",
        description = "Retorna la lista paginada de transacciones del comercio autenticado, ordenadas de más reciente a más antigua.",
        security = @SecurityRequirement(name = "ApiKey")
    )
    @org.springframework.cache.annotation.Cacheable(value = "payments_search", key = "#merchantId + '_' + #paymentId + '_' + #status + '_' + #startDate + '_' + #endDate + '_' + #minAmount + '_' + #maxAmount + '_' + #page + '_' + #size")
    public List<PaymentResponseDTO> listPayments(
            @RequestParam(required = false) String paymentId,
            @RequestParam(required = false) com.apigatewaypagos.demo.domain.model.PaymentStatus status,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime startDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime endDate,
            @RequestParam(required = false) java.math.BigDecimal minAmount,
            @RequestParam(required = false) java.math.BigDecimal maxAmount,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
            
        int safeSize = Math.min(size, 50);
        
        String merchantId = SecurityContextHolder.getContext().getAuthentication().getName();
        return paymentRepository.findByMerchantId(
                merchantId, paymentId, status, startDate, endDate, minAmount, maxAmount, page, safeSize)
                .stream()
                .map(PaymentResponseDTO::fromDomain)
                .toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_CHARGE') or hasAuthority('SCOPE_READ')")
    @Operation(
        summary = "Consultar el estado de un pago",
        description = "Obtén el resumen financiero de una transacción mediante su identificador UUID único.",
        security = @SecurityRequirement(name = "ApiKey")
    )
    public ResponseEntity<PaymentResponseDTO> getPaymentStatus(@PathVariable("id") UUID id) {
        String merchantId = SecurityContextHolder.getContext().getAuthentication().getName();
        PaymentResponseDTO response = getPaymentUseCase.execute(id, merchantId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/refund")
    @PreAuthorize("hasAuthority('SCOPE_REFUND')")
    @Operation(
        summary = "Reembolsar un pago",
        description = "Devuelve los fondos de un pago previamente capturado al método de pago original.",
        security = @SecurityRequirement(name = "ApiKey")
    )
    public ResponseEntity<String> refundPayment(@PathVariable("id") UUID id) {
        try {
            String merchantId = SecurityContextHolder.getContext().getAuthentication().getName();
            refundPaymentInteractor.execute(id, merchantId);
            return ResponseEntity.ok("Reembolso Procesado Exitosamente");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage()); 
        }
    }
}
