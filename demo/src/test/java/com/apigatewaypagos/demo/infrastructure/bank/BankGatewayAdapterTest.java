package com.apigatewaypagos.demo.infrastructure.bank;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.assertThat;

import com.apigatewaypagos.demo.TestcontainersConfiguration;
import com.apigatewaypagos.demo.domain.model.Money;
import com.apigatewaypagos.demo.domain.model.Payment;
import com.apigatewaypagos.demo.application.port.out.EventPublisherPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@SpringBootTest
public class BankGatewayAdapterTest {

    @MockitoBean
    private EventPublisherPort eventPublisherPort;

    @Autowired
    private BankGatewayAdapter bankGatewayAdapter;

    @Test
    void shouldProcessPaymentSuccessfully() {
        Payment dummyPayment = new Payment("dummy-key-2", "merch-123", new Money(new BigDecimal("100"), "MXN"), "pm_card_visa");
        
        System.out.println("=========================================");
        System.out.println("💳 PRUEBA DEL ADAPTADOR BANCARIO");
        System.out.println("=========================================");
        
        // El adaptador ahora simula una llamada con retardo y retorna true
        boolean result = bankGatewayAdapter.process(dummyPayment);

        assertThat(result).isTrue();
        System.out.println("=========================================");
    }
}
