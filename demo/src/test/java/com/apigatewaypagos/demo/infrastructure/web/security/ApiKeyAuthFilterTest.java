package com.apigatewaypagos.demo.infrastructure.web.security;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.apigatewaypagos.demo.application.service.ApiKeyService;

import jakarta.servlet.ServletException;


@ExtendWith(MockitoExtension.class)
public class ApiKeyAuthFilterTest {
    @Mock
    private ApiKeyService apiKeyService;
    private ApiKeyAuthFilter apiKeyAuthFilter;
    @BeforeEach
    void setUp() {
        apiKeyAuthFilter = new ApiKeyAuthFilter(apiKeyService);
    }
    @Test
    void doFilterInternal_ConKeyValida_DejaPasarLaPeticion() throws ServletException, IOException {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/payments");
        request.addHeader("X-API-KEY", "sk_live_valida123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();
        // Simulamos que el servicio reconoce la llave y devuelve el comerciante
        when(apiKeyService.validateAndGetMerchantId("sk_live_valida123")).thenReturn(Optional.of("micasa_mx"));
        // Act
        apiKeyAuthFilter.doFilterInternal(request, response, filterChain);
        // Assert
        // Si dejó pasar la petición, el código de estado HTTP no debería ser el 401 que lanza manualmente el filtro
        assertEquals(HttpStatus.OK.value(), response.getStatus(), "La petición debería continuar sin ser bloqueada");
    }
    @Test
    void doFilterInternal_ConKeyInvalida_Devuelve401() throws ServletException, IOException {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/payments");
        request.addHeader("X-API-KEY", "sk_live_mala123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();
        // Simulamos que la llave es inválida, expirada o revocada
        when(apiKeyService.validateAndGetMerchantId("sk_live_mala123")).thenReturn(Optional.empty());
        // Act
        apiKeyAuthFilter.doFilterInternal(request, response, filterChain);
        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatus(), "Debería bloquear la petición con 401 si la llave es inválida");
    }
    @Test
    void doFilterInternal_RutaAdmin_NoRequiereKey() throws ServletException, IOException {
        // Arrange
        // Simulamos un acceso a la zona de administración
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/admin/merchants/micasa_mx/keys");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();
        // Act
        apiKeyAuthFilter.doFilterInternal(request, response, filterChain);
        // Assert
        // El filtro tiene una regla: if(!path.startsWith("/api/payments")) -> dejar pasar.
        assertEquals(HttpStatus.OK.value(), response.getStatus(), "Rutas ajenas a /api/payments no requieren revisión de llave");
        verify(apiKeyService, never()).validateAndGetMerchantId(anyString()); // Verifica que el servicio de autenticación ni siquiera es llamado
    }
}
