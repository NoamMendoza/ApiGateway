package com.apigatewaypagos.demo.infrastructure.web.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.apigatewaypagos.demo.TestcontainersConfiguration;
import com.apigatewaypagos.demo.application.dto.PaymentResponseDTO;
import com.apigatewaypagos.demo.application.port.in.ProcessPaymentUseCase;
import com.apigatewaypagos.demo.application.usecase.GetPaymentUseCase;
import com.apigatewaypagos.demo.application.usecase.RefundPaymentInteractor;
import com.apigatewaypagos.demo.application.service.ApiKeyService;
import com.apigatewaypagos.demo.domain.exception.PaymentNotFoundException;
import com.apigatewaypagos.demo.domain.model.PaymentStatus;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@DisplayName("PaymentSecurityIntegrationTest — Aislamiento de Comercios")
class PaymentSecurityIntegrationTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private ProcessPaymentUseCase processPaymentUseCase;
    @MockitoBean private GetPaymentUseCase getPaymentUseCase;
    @MockitoBean private RefundPaymentInteractor refundPaymentInteractor;
    @MockitoBean private ApiKeyService apiKeyService;

    private UUID paymentId = UUID.randomUUID();

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        // Configuramos la API Key mock para que devuelva un mercante válido y los scopes necesarios
        // Esto permite que el ApiKeyAuthFilter no bloquee la petición y pueble el contexto
        when(apiKeyService.validateAndGetApiKeyDetails("mock-key"))
            .thenReturn(java.util.Optional.of(new ApiKeyService.ApiKeyResult("merchant_A", "CHARGE,READ,REFUND")));
    }

    @Test
    @WithMockUser(username = "merchant_A", authorities = {"SCOPE_READ"})
    @DisplayName("BOLA Check — Comercio A no puede ver pagos si el UseCase lanza NotFound")
    void getPaymentStatus_WhenOwnedByOther_ShouldReturn404() throws Exception {
        // Given: El interactor lanza NotFoundException si merchantId no coincide (IDOR Protected)
        when(getPaymentUseCase.execute(eq(paymentId), eq("merchant_A")))
            .thenThrow(new PaymentNotFoundException("No encontrado"));

        // When / Then
        mockMvc.perform(get("/api/payments/" + paymentId)
                .header("X-API-KEY", "mock-key"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "merchant_A", authorities = {"SCOPE_CHARGE"})
    @DisplayName("Acceso Correcto — Comercio A ve su propio pago")
    void getPaymentStatus_WhenOwnedBySelf_ShouldReturn200() throws Exception {
        // Given
        PaymentResponseDTO response = new PaymentResponseDTO(
            paymentId, 
            "merchant_A", 
            new BigDecimal("100.00"), 
            "USD", 
            PaymentStatus.CAPTURED.name(), 
            LocalDateTime.now(), 
            LocalDateTime.now()
        );
        when(getPaymentUseCase.execute(eq(paymentId), eq("merchant_A"))).thenReturn(response);

        // When / Then
        mockMvc.perform(get("/api/payments/" + paymentId)
                .header("X-API-KEY", "mock-key"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "merchant_A", authorities = {"SCOPE_REFUND"})
    @DisplayName("BOLA Check — Intento de Reembolso Cruzado")
    void refundPayment_WhenUnauthorized_ShouldReturn404() throws Exception {
        // Given
        when(refundPaymentInteractor.execute(eq(paymentId), eq("merchant_A")))
            .thenThrow(new IllegalArgumentException("No tiene permisos"));

        // When / Then
        mockMvc.perform(post("/api/payments/" + paymentId + "/refund")
                .header("X-API-KEY", "mock-key"))
                .andExpect(status().isNotFound());
    }
}
