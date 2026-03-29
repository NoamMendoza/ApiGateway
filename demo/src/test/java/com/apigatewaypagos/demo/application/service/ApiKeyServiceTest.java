package com.apigatewaypagos.demo.application.service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.codec.digest.DigestUtils;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.apigatewaypagos.demo.domain.model.ApiKey;
import com.apigatewaypagos.demo.domain.repository.ApiKeyRepository;

@ExtendWith(MockitoExtension.class)
public class ApiKeyServiceTest {

    @Mock
    private ApiKeyRepository apiKeyRepository;

    private ApiKeyService apiKeyService;

    private final String rawSecret = "sk_live_abc123456789";
    private final String prefix = "sk_live_";
    private final String hashedSecret = DigestUtils.sha256Hex(rawSecret);

    @BeforeEach
    void setUp() {
        apiKeyService = new ApiKeyService(apiKeyRepository);
    }

    @Test
    void validate_keyValida_ReturnsMerchantId() {
        ApiKey validKey = new ApiKey(UUID.randomUUID(), "micasa_mx", prefix, hashedSecret, true, LocalDateTime.now(), LocalDateTime.now().plusDays(30));

        when(apiKeyRepository.findByKeyPrefix(prefix)).thenReturn(Optional.of(validKey));

        // Act
        Optional<String> merchantId = apiKeyService.validateAndGetMerchantId(rawSecret);

        // Assert
        assertTrue(merchantId.isPresent(), "La llave debería ser válida y retornar un merchantId");
        assertEquals("micasa_mx", merchantId.get());
        verify(apiKeyRepository, times(1)).findByKeyPrefix(prefix);
    }

    @Test
    void validate_KeyInactiva_ReturnsEmpty() {
        // isActive = false
        ApiKey revokedKey = new ApiKey(UUID.randomUUID(), "micasa_mx", prefix, hashedSecret, false, LocalDateTime.now(), LocalDateTime.now().plusDays(30));
        when(apiKeyRepository.findByKeyPrefix(prefix)).thenReturn(Optional.of(revokedKey));
        
        // Act
        Optional<String> merchantId = apiKeyService.validateAndGetMerchantId(rawSecret);
        
        // Assert
        assertFalse(merchantId.isPresent(), "La llave no debería ser válida porque está inactiva (revocada)");
    }

    @Test
    void validate_KeyExpirada_ReturnsEmpty() {
        // expiresAt = ayer
        ApiKey expiredKey = new ApiKey(UUID.randomUUID(), "micasa_mx", prefix, hashedSecret, true, LocalDateTime.now().minusDays(2), LocalDateTime.now().minusDays(1));
        when(apiKeyRepository.findByKeyPrefix(prefix)).thenReturn(Optional.of(expiredKey));
        
        // Act
        Optional<String> merchantId = apiKeyService.validateAndGetMerchantId(rawSecret);
        
        // Assert
        assertFalse(merchantId.isPresent(), "La llave no debería ser válida porque ya expiró");
    }

    @Test
    void validate_KeyNoExiste_ReturnsEmpty() {
        // Arrange
        when(apiKeyRepository.findByKeyPrefix(prefix)).thenReturn(Optional.empty());
        
        // Act
        Optional<String> merchantId = apiKeyService.validateAndGetMerchantId(rawSecret);
        
        // Assert
        assertFalse(merchantId.isPresent(), "La llave no debería ser válida porque no existe en la BD");
    }
}