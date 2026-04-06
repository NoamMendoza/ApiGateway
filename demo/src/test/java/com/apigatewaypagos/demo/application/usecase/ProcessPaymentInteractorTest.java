package com.apigatewaypagos.demo.application.usecase;

import java.math.BigDecimal;
import java.util.Optional;

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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.apigatewaypagos.demo.application.port.in.ProcessPaymentCommand;
import com.apigatewaypagos.demo.application.port.out.BankGatewayPort;
import com.apigatewaypagos.demo.application.port.out.EventPublisherPort;
import com.apigatewaypagos.demo.application.port.out.PaymentGatewayResult;
import com.apigatewaypagos.demo.domain.exception.InvalidPaymentStateException;
import com.apigatewaypagos.demo.domain.model.Money;
import com.apigatewaypagos.demo.domain.model.Payment;
import com.apigatewaypagos.demo.domain.model.PaymentStatus;
import com.apigatewaypagos.demo.domain.repository.PaymentRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProcessPaymentInteractor — Idempotencia y Procesamiento")
class ProcessPaymentInteractorTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private BankGatewayPort bankGatewayPort;
    @Mock private EventPublisherPort eventPublisherPort;

    @InjectMocks
    private ProcessPaymentInteractor interactor;

    private ProcessPaymentCommand command;
    private String merchantId = "merchant_123";
    private String idempotencyKey = "key_abc";

    @BeforeEach
    void setUp() {
        command = new ProcessPaymentCommand(
            idempotencyKey,
            merchantId,
            new BigDecimal("100.00"),
            "USD",
            "pm_card_visa"
        );
    }

    @Test
    @DisplayName("Pago nuevo — debe procesar exitosamente con el banco")
    void execute_WhenNewPayment_ShouldProcessSuccessfully() {
        // Given
        when(paymentRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(bankGatewayPort.process(any(Payment.class))).thenReturn(PaymentGatewayResult.success("ext_999"));

        // When
        Payment result = interactor.execute(command);

        // Then
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.CAPTURED);
        verify(paymentRepository, times(2)).save(any(Payment.class)); // 1 al crear, 1 al capturar
        verify(eventPublisherPort).publishPaymentCompletedEvent(any(Payment.class));
    }

    @Test
    @DisplayName("🔄 Smart Idempotency — Reintento idéntico devuelve mismo resultado sin llamar al banco")
    void execute_WhenDuplicateIdenticalRequest_ShouldReturnExisting() {
        // Given
        Payment existingPayment = new Payment(idempotencyKey, merchantId, new Money(new BigDecimal("100.00"), "USD"), "pm_card_visa");
        existingPayment.authorize();
        existingPayment.capture();
        
        when(paymentRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.of(existingPayment));

        // When
        Payment result = interactor.execute(command);

        // Then
        assertThat(result).isSameAs(existingPayment);
        verify(bankGatewayPort, never()).process(any());
        verify(eventPublisherPort, never()).publishPaymentCompletedEvent(any());
    }

    @Test
    @DisplayName("❌ Conflicto de Idempotencia — Misma llave pero parámetros distintos")
    void execute_WhenDuplicateRequestWithDifferentAmount_ShouldThrowException() {
        // Given
        Payment existingPayment = new Payment(idempotencyKey, merchantId, new Money(new BigDecimal("200.00"), "USD"), "pm_card_visa");
        when(paymentRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.of(existingPayment));

        // When / Then
        assertThatThrownBy(() -> interactor.execute(command))
            .isInstanceOf(InvalidPaymentStateException.class)
            .hasMessageContaining("diferente");

        verify(bankGatewayPort, never()).process(any());
    }
}
