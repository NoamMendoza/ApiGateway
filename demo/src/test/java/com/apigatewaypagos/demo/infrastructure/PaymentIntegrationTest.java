package com.apigatewaypagos.demo.infrastructure;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.apigatewaypagos.demo.TestcontainersConfiguration;
import com.apigatewaypagos.demo.domain.model.Money;
import com.apigatewaypagos.demo.domain.model.Payment;
import com.apigatewaypagos.demo.domain.repository.PaymentRepository;

import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.apigatewaypagos.demo.application.port.out.BankGatewayPort;
import com.apigatewaypagos.demo.application.port.out.EventPublisherPort;

import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.RedisTemplate;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class PaymentIntegrationTest {

    @MockitoBean
    private BankGatewayPort bankGatewayPort;

    @MockitoBean
    private EventPublisherPort eventPublisherPort;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private org.springframework.test.web.servlet.MockMvc mockMvc;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @BeforeEach
    void setUp() {
        // Limpiamos Redis antes de cada test para que los contadores no se acumulen
        // La clave ahora usa separador ':': "rate_limit:" + apiKey
        var keys = redisTemplate.keys("rate_limit:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
    @Test
    void shouldSaveAndRetrievePaymentFromDatabase() {
        // 1. Arrange: Crear un pago de dominio puro
        String idempotencyKey = "test-key-123";
        Payment newPayment = new Payment(idempotencyKey, "merch-001", new Money(new BigDecimal("150.50"), "MXN"));
        
        // 2. Act: Guardarlo en la base de datos (PostgreSQL en Testcontainers)
        paymentRepository.save(newPayment);

        // 3. Assert: Recuperarlo por su llave de idempotencia
        var retrievedPayment = paymentRepository.findByIdempotencyKey(idempotencyKey);
        
        assertThat(retrievedPayment).isPresent();
        assertThat(retrievedPayment.get().getId()).isEqualTo(newPayment.getId());
        assertThat(retrievedPayment.get().getMerchantId()).isEqualTo("merch-001");
        assertThat(retrievedPayment.get().getAmount().amount().compareTo(new BigDecimal("150.50"))).isEqualTo(0);
        assertThat(retrievedPayment.get().getAmount().currency()).isEqualTo("MXN");
        assertThat(retrievedPayment.get().getStatus()).isEqualTo(newPayment.getStatus());
    }

    @Test
    void shouldThrowExceptionWhenSavingDuplicateIdempotencyKey() {
        // Arrange
        String idempotencyKey = "duplicate-key-456";
        Payment firstPayment = new Payment(idempotencyKey, "merch-001", new Money(new BigDecimal("100.00"), "MXN"));
        Payment secondPayment = new Payment(idempotencyKey, "merch-002", new Money(new BigDecimal("200.00"), "USD"));
        
        // Act
        paymentRepository.save(firstPayment);

        // Assert: Garantiza que la base de datos PostgreSQL bloquee el registro duplicado usando la restricción @Column(unique = true)
        org.junit.jupiter.api.Assertions.assertThrows(
            org.springframework.dao.DataIntegrityViolationException.class,
            () -> paymentRepository.save(secondPayment),
            "Expected DataIntegrityViolationException to validate the unique constraint on idempotencyKey"
        );
    }

    @Test
    void shouldThrowExceptionWhenProvidingInvalidDomainData() {
        // Assert: Valida que el dominio de Java lance IllegalArgumentException antes de llegar a la base de datos
        org.junit.jupiter.api.Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> new Payment("", "merch-123", new Money(new BigDecimal("100.00"), "MXN")),
            "La llave de idempotencia es obligatoria"
        );
    }

    @Test
    void shouldRejectPaymentWhenApiKeyIsMissingOrInvalid() throws Exception{
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.
        post("/api/payments")
        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
        .content("""
            {
                "merchantId": "merch-123",
                "amount":500.00,
                "currency":"MXN"
            }""")
        .header("Idempotency-Key", "error-test-123"))
        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isUnauthorized());
    }

    @Test
    void shouldBlockPaymentWhenRateLimitIsExceeded() throws Exception {
        String validJson = "{\"merchantId\": \"merch-123\", \"amount\":500.00, \"currency\":\"MXN\"}";
        String validApiKey = "sk_test-123456789"; // Llave configurada en application.properties
        
        // Hacemos 5 peticiones exitosas consecutivas
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.
                post("/api/payments")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(validJson)
                .header("Idempotency-Key", "rate-limit-test-" + i)
                .header("X-API-KEY", validApiKey))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
        }

        // La sexta petición debe ser bloqueada por Redis con HTTP 429 Too Many Requests
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.
            post("/api/payments")
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content(validJson)
            .header("Idempotency-Key", "rate-limit-test-6")
            .header("X-API-KEY", validApiKey))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isTooManyRequests());
    }

    @Test
    void shouldPublishEventWhenPaymentIsSuccessful() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
            .post("/api/payments")
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content("{\"merchantId\": \"merch-123\", \"amount\":500.00, \"currency\":\"MXN\"}")
            .header("Idempotency-Key", "event-test-key-001")
            .header("X-API-KEY", "sk_test-123456789"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());

        // Verifica que el adaptador de eventos fue llamado exactamente 1 vez
        verify(eventPublisherPort, times(1)).publishPaymentCompletedEvent(any());
    }
}
