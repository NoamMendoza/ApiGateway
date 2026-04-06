# 🏦 B2B Payment Gateway Core

Un núcleo transaccional de grado empresarial que implementa una pasarela de pagos B2B completa sobre **Stripe**, con soporte de reembolsos, detección de contracargos y notificaciones asíncronas en tiempo real a los comercios. Construido con **Arquitectura Hexagonal** (Ports & Adapters) sobre Spring Boot.

> **Diseñado para**: plataformas SaaS, E-Commerce B2B y aplicaciones que necesiten cobrar con tarjeta sin construir su propia integración bancaria.

---

## ✨ Características Enterprise

| Categoría | Feature |
|-----------|---------|
| 🔐 **Seguridad** | Anti-Timing Attacks (MessageDigest), Protección BOLA/IDOR (Segregación por Merchant), Anti-XSS (Sanitización DOM) |
| 💳 **Pagos** | Cobros síncronos vía Stripe, **Idempotencia Inteligente** (reintentos sin doble cobro) |
| 🔄 **Reembolsos** | Endpoint dedicado con validación de propiedad (IDOR Protected), persistencia y notificación |
| 📨 **Webhooks Salientes** | Firmas **HMAC-SHA256** (`X-PayGateway-Signature`), reintentos automáticos y secretos únicos |
| 📈 **Dashboard** | Pantalla de métricas **Responsiva** (Mobile/TV), KPIs privados por comercio y gráficas en tiempo real |
| 🏗️ **Infraestructura** | Redis Caching por usuario, PostgreSQL + Flyway, RabbitMQ, Docker Compose |

---

## 🛠️ Stack Tecnológico

| Capa | Tecnología |
|------|-----------|
| **Lenguaje** | Java 21 |
| **Framework** | Spring Boot 4.x (WebMVC, Data JPA, Security, AMQP) |
| **Base de Datos** | PostgreSQL 18 + Flyway (migraciones versionadas) |
| **Caché / Rate Limit** | Redis |
| **Message Broker** | RabbitMQ |
| **Pasarela Bancaria** | Stripe SDK |
| **Resiliencia** | Resilience4j (Circuit Breaker), Spring Retry |
| **DevOps** | Docker & Docker Compose |
| **Documentación** | springdoc-openapi (Swagger UI) |

---

## ⚙️ Arquitectura Hexagonal

```
┌─────────────────────────────────────────────────────────┐
│                     INFRASTRUCTURE                       │
│  ┌──────────────┐  ┌──────────────┐  ┌───────────────┐  │
│  │ Web          │  │ Persistence  │  │ Messaging     │  │
│  │ Controllers  │  │ JPA/Flyway   │  │ RabbitMQ      │  │
│  │ Filters      │  │ Redis        │  │ Stripe Client │  │
│  └──────┬───────┘  └──────┬───────┘  └───────┬───────┘  │
│         │                 │                  │           │
│  ┌──────▼─────────────────▼──────────────────▼───────┐   │
│  │              APPLICATION (Use Cases)               │   │
│  │  ProcessPaymentInteractor  RefundPaymentInteractor │   │
│  │  CreateApiKeyUseCase       StripeWebhookService    │   │
│  └──────────────────────┬────────────────────────────┘   │
│                         │                                 │
│  ┌──────────────────────▼────────────────────────────┐   │
│  │                    DOMAIN                          │   │
│  │   Payment  Money  PaymentStatus  ApiKey            │   │
│  │   (Sin dependencias externas — portable)           │   │
│  └───────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
```

---

## 🚀 Configuración Local

### Pre-requisitos
- **Docker Desktop** instalado y corriendo
- **Java 21**
- **Maven 3.9+**
- Opcional: **Stripe CLI** para probar webhooks localmente

### 1. Clonar e iniciar infraestructura

```bash
git clone <repo-url>
cd gateway_pagos/demo
docker compose up -d   # Levanta Postgres, Redis y RabbitMQ
```

### 2. Configurar Stripe (opcional en dev)

El proyecto incluye una API Key de test preconfigurada. Para usar la tuya:

```powershell
# PowerShell
$env:STRIPE_API_KEY="sk_test_TU_CLAVE_AQUI"
$env:STRIPE_WEBHOOK_SECRET="whsec_TU_SECRETO_AQUI"  # obtenido con stripe listen
```

### 3. Arrancar el servidor

```bash
mvn spring-boot:run
```

El servidor estará disponible en `http://localhost:8080`.

📖 **Swagger UI:** `http://localhost:8080/swagger-ui.html`

---

## 🎮 Guía de Uso Rápido

### Paso 1 — Generar API Key para el comercio

```http
POST /api/admin/merchants/{merchantId}/keys
```
> Guarda la `sk_live_...` que devuelve. No se vuelve a mostrar.

### Paso 2 — Configurar URL de Webhook del comercio

```http
PUT /api/admin/merchants/{merchantId}/webhook
Content-Type: application/json

{ "webhookUrl": "https://tu-sistema.com/webhooks" }
```

### Paso 3 — Realizar un cobro

```http
POST /api/payments
X-API-KEY: sk_live_tu_clave
Idempotency-Key: compra-uuid-unico

{
  "paymentMethodToken": "pm_card_visa",
  "amount": 250.75,
  "currency": "MXN"
}
```

**Respuesta exitosa:**
```json
{
  "paymentId": "uuid",
  "merchantId": "micasa_mx",
  "status": "CAPTURED",
  "amount": 250.75,
  "currency": "MXN"
}
```

### Paso 4 — Reembolsar un pago

```http
POST /api/payments/{paymentId}/refund
X-API-KEY: sk_live_tu_clave_con_scope_REFUND
```

### Paso 5 — Webhook que recibirá tu sistema

Cuando el pago se captura, reembolsa o disputa, enviaremos un `POST` a tu `webhookUrl` con:

```json
{
  "paymentId": "uuid",
  "merchantId": "micasa_mx",
  "status": "CAPTURED | REFUNDED | DISPUTED | FAILED",
  "amount": 250.75,
  "currency": "MXN"
}
```

---

## 🃏 Tarjetas de Prueba (Stripe Test Mode)

| Token | Comportamiento |
|-------|---------------|
| `pm_card_visa` | Pago exitoso ✅ |
| `pm_card_mastercard` | Pago exitoso ✅ |
| `pm_card_visa_debit` | Débito exitoso ✅ |
| `pm_card_declined` | Tarjeta declinada ❌ |
| `pm_card_createDispute` | Pago exitoso + Disputa automática 🚨 |

---

## 🔐 Seguridad

### Protección IDOR / BOLA
- **Segregación Estricta**: Cada recurso (`Payment`, `Metrics`) está filtrado por el `merchantId` autenticado. Un comercio no puede acceder a transacciones ajenas ni siquiera conociendo el UUID.
- **Validación en Application Layer**: Los Use Cases verifican la propiedad antes de procesar reembolsos o lecturas.

### Hardening de API
- **Anti-Timing Attacks**: Validación de API Keys con `MessageDigest.isEqual` para evitar ataques de tiempo basados en comparaciones iterativas de strings.
- **Anti-XSS**: Sanitización proactiva de datos en el Dashboard usando `escapeHTML` para prevenir ataques de inyección de scripts vía nombres o campos de metadatos.
- **Rate Limiting Distribuido**: 5 peticiones por minuto por API Key con ventana deslizante sobre Redis.

### Webhooks Seguros (Egress)
- **Firma de Mensajes**: Cada webhook enviado incluye el header `X-PayGateway-Signature`.
- **Criptografía**: Firma HMAC-SHA256 generada con un secreto único de 40 caracteres por comercio.
- **Infraestructura**: Despacho asíncrono vía RabbitMQ con 3 reintentos automáticos y backoff exponencial.

---

## 🌐 Variables de Entorno

| Variable | Default (dev) | Producción |
|----------|---------------|-----------|
| `STRIPE_API_KEY` | `sk_test_51...` | Tu Live Key de Stripe |
| `STRIPE_WEBHOOK_SECRET` | `whsec_...` | Secreto del endpoint en Stripe Dashboard |
| `DB_USER` | `postgres` | Usuario restringido |
| `DB_PASSWORD` | `postgres` | Contraseña segura |
| `RABBITMQ_USER` | `guest` | Usuario de producción |
| `RABBITMQ_PASS` | `guest` | Contraseña segura |

---

## 🧪 Probar Webhooks Localmente (Stripe CLI)

```bash
# Instalar Stripe CLI (Windows)
winget install Stripe.StripeCLI

# Autenticarse
stripe login

# Reenviar eventos a tu servidor local
stripe listen --forward-to localhost:8080/api/webhooks/stripe

# Simular un contracargo
stripe trigger charge.dispute.created
```

---

## 📊 Dashboard de Control (Merchant Hub)

El proyecto incluye un dashboard administrativo moderno alojado en `/dashboard.html` que ofrece:
- **Responsive Design**: Optimizado para Mobile, Tablet, Desktop y 4K (TV).
- **KPIs en Vivo**: Ingresos del día, Tasas de éxito y Volumen transaccional.
- **Filtrado Avanzado**: Búsqueda por ID, Estado, Rango de Fechas y Montos.
- **Seguridad**: Autenticación vía API Key almacenada localmente.

---

## 🔁 Idempotencia Inteligente (Double Charge Prevention)

A diferencia de implementaciones básicas, este Gateway implementa lógica "Smart":
1. **Detección**: Si se recibe el mismo `Idempotency-Key`, el sistema pausa el procesamiento.
2. **Validación**: Compara los parámetros (`amount`, `currency`, `merchantId`).
3. **Respuesta**: Si los parámetros son idénticos, devuelve el resultado original de forma instantánea. Si difieren, bloquea la transacción para evitar fraudes de colisión de llaves.

---

## 📈 Ciclo de Vida de un Pago

## 🧪 Pruebas y QA (Test Suite)

El proyecto incluye una suite de pruebas robusta que garantiza la seguridad y la resiliencia del gateway.

### Categorías de Pruebas

1.  **Unitarias (Domain & Application)**:
    -   **Seguridad IDOR/BOLA**: Verificación de aislamiento entre comercios en interactores (`RefundPaymentInteractorTest`).
    -   **Idempotencia**: Prevención de cargos dobles mediante validación de llaves únicas (`ProcessPaymentInteractorTest`).
    -   **Webhooks**: Validación de lógica de reintentos y firmas HMAC (`WebhookEmissionWorkerTest`).
2.  **Integración (Infrastructure & Containers)**:
    -   Utiliza **Testcontainers** para levantar instancias reales de PostgreSQL, Redis y RabbitMQ durante la ejecución de los tests.
    -   **Aislamiento de Comercio (Zero Trust)**: `PaymentSecurityIntegrationTest` valida que los controladores rechacen accesos cruzados mediante MockMvc y una base de datos real.

### Cómo ejecutar los tests

Asegúrate de tener **Docker Desktop** iniciado y ejecuta desde la carpeta `demo`:

```bash
mvn test
```

---

## 📁 Estructura del Proyecto

```
src/main/java/com/apigatewaypagos/demo/
├── domain/
│   ├── model/          # Payment, Money, PaymentStatus, ApiKey
│   ├── repository/     # Interfaces de repositorio (Ports)
│   └── exception/      # Excepciones de dominio
├── application/
│   ├── usecase/        # ProcessPaymentInteractor, RefundPaymentInteractor
│   ├── service/        # ApiKeyService
│   └── port/out/       # BankGatewayPort, EventPublisherPort, PaymentGatewayResult
└── infrastructure/
    ├── bank/           # BankGatewayAdapter (Stripe)
    ├── messaging/      # RabbitMqEventPublisherAdapter, WebhookEmissionWorker
    ├── persistence/    # PaymentRepositoryAdapter, JPA Entities
    └── web/
        ├── controller/ # PaymentController, ApiKeyAdminController
        ├── webhook/    # StripeWebhookController, StripeWebhookService
        └── security/   # ApiKeyAuthFilter, RateLimitInterceptor
```
