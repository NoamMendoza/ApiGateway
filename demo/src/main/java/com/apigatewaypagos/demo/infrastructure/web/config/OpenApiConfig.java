package com.apigatewaypagos.demo.infrastructure.web.config;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "🚀 Payment Gateway API B2B",
        version = "v1.0.0",
        description = "Core bancario para procesamiento seguro de pagos con tarjetas de crédito/débito y enrutamiento a Stripe. Diseñado con Clean Architecture y Resiliencia.",
        contact = @Contact(
            name = "Soporte Técnico de Pagos",
            email = "soporte@tuempresa.com"
        )
    )
)
@SecurityScheme(
    name = "ApiKey", // Identificador interno en Swagger
    type = SecuritySchemeType.APIKEY,
    in = SecuritySchemeIn.HEADER,
    paramName = "X-API-KEY",
    description = "Introduce tu API Key en la forma: sk_live_..."
)
public class OpenApiConfig {
}
