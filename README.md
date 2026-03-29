# 🚀 B2B Payment Gateway Core

Un núcleo transaccional avanzado y robusto (Payment Gateway) diseñado para procesar y enrutar pagos con tarjetas hacia **Stripe** de forma segura. Construido con arquitectura de grado empresarial y principios de **Clean Architecture** (Ports & Adapters) para separar la lógica de negocio de la infraestructura tecnológica externa.

Perfecto para integrarse como el backend de pagos de cualquier plataforma de SaaS, E-Commerce o aplicación móvil B2B.

---

## ✨ Características Principales (Enterprise-Ready)

- 🛡️ **Seguridad Criptográfica**: Autenticación B2B mediante API Keys cifradas asimétricamente con SHA-256 manejadas por un filtro de seguridad de Spring (Sin exposición de contraseñas en texto plano).
- 🔄 **Alta Resiliencia**: Patrones de Circuit Breaker integrados con `resilience4j` para proteger el sistema contra demoras y caídas de los servidores mundiales del banco adquirente (Stripe).
- 🚦 **Rate Limiting Distribuido**: Algoritmo de mitigación de ataques estructurado sobre **Redis** en memoria, diseñado para evitar denegación de servicios (DDoS).
- 📩 **Arquitectura Orientada a Eventos (EDA)**: Las validaciones de pagos emiten la transacción asíncronamente hacia **RabbitMQ**, posibilitando que otros servicios de inventario, analítica o facturación consuman estas auditorías sin afectar la latencia.
- 🐘 **Integridad de Datos**: Transacciones seguras e idempotentes (Idempotency-Keys) almacenadas en **PostgreSQL**, con control de versiones manejado por **Flyway**.
- 📖 **Documentación Automática**: Endpoints documentados en tiempo real listos al instante gracias al uso de Swagger (OpenAPI 3.0).

---

## 🛠️ Stack Tecnológico

- **Lenguaje**: Java 21
- **Framework Core**: Spring Boot 3.4.x (WebMVC, Data JPA, Security)
- **Base de Datos**: PostgreSQL
- **Caché y Limitador**: Redis
- **Message Broker**: RabbitMQ
- **Integraciones**: Stripe SDK
- **DevOps**: Docker & Docker Compose
- **Documentación API**: springdoc-openapi (Swagger)

---

## ⚙️ Arquitectura

Este sistema cumple de manera estricta los principios de **Arquitectura Hexagonal**.

- `domain`: Las reglas de negocio primordiales (Ej. cómo validar un dinero, estados de pago). No tiene dependencias externas, asegurando portabilidad absoluta.
- `application`: Casos de uso e Interfaces (Ports) de entrada y salida (Interactor y API Key Management).
- `infrastructure`: Adaptadores del exterior (Web Controllers, Repositorios de Data JPA, Filtros REST API, Client calls a mensajería de RabbitMQ y consumo web a Stripe).

---

## 🚀 Requisitos y Configuración Local

1. Asegúrate de tener instalado **Docker** en tu sistema.
2. Clona el repositorio.
3. El proyecto incluye un archivo `compose.yaml` para levantar toda la infraestructura sin configuraciones manuales:

```bash
docker compose up -d
```
Verifica tener los siguientes puertos libres: `5432` (Postgres), `6379` (Redis), y `5672` (RabbitMQ).

4. **Variables de Entorno**. En el entorno de desarrollo, la API Key de integración corre en modo de prueba (`sk_test...`). Sin embargo, es buena práctica registrarte gratis en [Stripe Developers](https://dashboard.stripe.com/) y obtener la tuya.

Exponla en tu terminal (Ejemplo para PowerShell):
```powershell
$env:STRIPE_API_KEY="sk_test_1234..."
```

5. Compila y ejecuta el Servidor:
```bash
mvn clean install
mvn spring-boot:run
```

---

## 🏢 Adaptación para tu propio Negocio (Variables de Entorno)

Si deseas clonar este proyecto y configurarlo para tu propia startup o empresa de pagos, el sistema está diseñado para leer **Variables de Entorno** sin necesidad de modificar el código fuente (`application-dev.properties` y `application.properties`).

Para salir del modo de prueba local, debes inyectar las siguientes variables en tu servidor (AWS, Railway, Render, etc.) o dentro de tu archivo `docker-compose`:

| Variable de Entorno | Valor por defecto en Dev | Descripción y Uso en Producción |
| :--- | :--- | :--- |
| `STRIPE_API_KEY` | `sk_test_51...` | **CRÍTICO.** Sustitúyela por tu *Live Key* de Stripe (`sk_live_...`) para empezar a capturar transacciones con dinero real. |
| `DB_USER` | `postgres` | Usuario de seguridad de PostgreSQL. Cámbialo por un rol con permisos restrigidos. |
| `DB_PASSWORD` | `postgres` | Contraseña de tu motor de base de datos relacional. |
| `RABBITMQ_USER` | `guest` | Usuario para el broker de mensajería (Eventos de finalización de pago). |
| `RABBITMQ_PASS` | `guest` | Contraseña del broker. |
| `SSL_KEY_PASSWORD` | `changeit` | Contraseña de tu almacén de certificados PKCS12 (`.p12`) obligatorio para arrancar tu API en puerto seguro HTTPS/TLS. |

---

## 🎮 Guía de Inicio Rápido

La pasarela de pago correrá en `http://localhost:8080`. Puedes acceder instantáneamente a todos los endpoints listados dentro de **Swagger UI**:

🔗 **Ver Documentación:** `http://localhost:8080/swagger-ui.html`

### 1️⃣ Emitir una API Key para un Comercio (Admin)
Antes de cobrar, debes dar de alta un comercio emitiéndole una credencial.
**[POST]** `/api/admin/merchants/micasa_mx/keys`
La respuesta será un texto plano con la Secret Key (No la pierdas, ¡no se vuelve a mostrar!).

### 2️⃣ Cargar un Cobro Bancario (B2B)
**[POST]** `/api/payments`
Añade los siguientes Headers para validación obligatoria:
* `X-API-KEY`: *La Secret Key generada en el paso anterior*
* `Idempotency-key`: `test-compra-1234` *(UUID o cadena única para evitar cobros dobles)*

**Cuerpo de la Petición (JSON):**
```json
{
  "paymentMethodToken": "pm_card_visa",
  "amount": 250.75,
  "currency": "MXN"
}
```

La transacción iniciará, se enrutará asíncronamente y dejará una estela de auditoría en la BD antes de emitirse como evento final 💳.
