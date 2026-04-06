package com.apigatewaypagos.demo.application.usecase;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.apigatewaypagos.demo.application.port.out.BankGatewayPort;
import com.apigatewaypagos.demo.application.port.out.EventPublisherPort;
import com.apigatewaypagos.demo.application.port.out.PaymentGatewayResult;
import com.apigatewaypagos.demo.domain.exception.InvalidPaymentStateException;
import com.apigatewaypagos.demo.domain.model.Money;
import com.apigatewaypagos.demo.domain.model.Payment;
import com.apigatewaypagos.demo.domain.model.PaymentStatus;
import com.apigatewaypagos.demo.domain.repository.PaymentRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("RefundPaymentInteractor — Casos de uso del reembolso")
class RefundPaymentInteractorTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private BankGatewayPort bankGatewayPort;
    @Mock private EventPublisherPort eventPublisherPort;

    @InjectMocks
    private RefundPaymentInteractor interactor;

    private UUID paymentId;
    private Payment capturedPayment;

    @BeforeEach
    void setUp() {
        paymentId = UUID.randomUUID();
        capturedPayment = new Payment("idempotency-test-1", "merchant_test",
                new Money(new BigDecimal("150.00"), "MXN"), "pm_card_visa");
        capturedPayment.authorize();
        capturedPayment.capture();
        capturedPayment.setExternalTransactionId("pi_external_123");
        paymentId = capturedPayment.getId();
    }

    @Test
    @DisplayName("Reembolso exitoso — debe marcar REFUNDED, guardar y emitir evento")
    void execute_WhenPaymentCaptured_ShouldRefundSuccessfully() {
        // Given
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(capturedPayment));
        when(bankGatewayPort.refund(capturedPayment)).thenReturn(PaymentGatewayResult.success("re_stripe_123"));

        // When
        Payment result = interactor.execute(paymentId);

        // Then
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        verify(paymentRepository).save(capturedPayment);
        verify(eventPublisherPort).publishPaymentCompletedEvent(capturedPayment);
    }

    @Test
    @DisplayName("Pago no encontrado — debe lanzar IllegalArgumentException (→ 404)")
    void execute_WhenPaymentNotFound_ShouldThrowIllegalArgumentException() {
        // Given
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> interactor.execute(paymentId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining(paymentId.toString());

        verifyNoInteractions(bankGatewayPort, eventPublisherPort);
    }

    @Test
    @DisplayName("Banco rechaza reembolso — no debe guardar ni emitir evento")
    void execute_WhenBankDeclinesRefund_ShouldThrowAndNotPersist() {
        // Given
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(capturedPayment));
        when(bankGatewayPort.refund(capturedPayment))
            .thenReturn(PaymentGatewayResult.failure("Fondos insuficientes en Stripe"));

        // When / Then
        assertThatThrownBy(() -> interactor.execute(paymentId))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Reembolso fallido");

        verify(paymentRepository, never()).save(any());
        verifyNoInteractions(eventPublisherPort);
    }

    @Test
    @DisplayName("Reembolsar pago en estado PENDING — el dominio debe rechazarlo")
    void execute_WhenPaymentNotCaptured_ShouldThrowInvalidStateException() {
        // Given — pago en estado PENDING (nunca pasó por authorize/capture)
        Payment pendingPayment = new Payment("idempotency-2", "merchant_test",
                new Money(new BigDecimal("50.00"), "MXN"), "pm_card_visa");
        when(paymentRepository.findById(pendingPayment.getId())).thenReturn(Optional.of(pendingPayment));

        // When / Then
        assertThatThrownBy(() -> interactor.execute(pendingPayment.getId()))
            .isInstanceOf(InvalidPaymentStateException.class)
            .hasMessageContaining("capturados");

        verifyNoInteractions(bankGatewayPort);
        verify(paymentRepository, never()).save(any());
    }
}
