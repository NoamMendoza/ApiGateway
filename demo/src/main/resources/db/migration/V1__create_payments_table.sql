-- ============================================================
-- V1: Creación de la tabla payments
--
-- Esta migration refleja la estructura definida en PaymentEntity.java
-- A partir de aquí, Flyway gestiona el esquema — Hibernate solo lo valida.
--
-- Convención de nombres Flyway:
--   V{numero}__{descripcion}.sql
--   El doble guion bajo es obligatorio.
-- ============================================================

CREATE TABLE IF NOT EXISTS payments (
    -- UUID como VARCHAR(36) para compatibilidad con @JdbcTypeCode(SqlTypes.VARCHAR)
    id              VARCHAR(36)    NOT NULL PRIMARY KEY,

    -- La clave de idempotencia garantiza que el mismo pago no se procese dos veces
    idempotency_key VARCHAR(255)   NOT NULL UNIQUE,

    -- Nombre exacto que usa Hibernate para la columna (del campo merchatId en la entidad)
    merchat_id      VARCHAR(255),

    -- Monto y moneda almacenados por separado
    amount          NUMERIC(19, 4),
    currency        VARCHAR(10),

    -- Estado del ciclo de vida: PENDING, AUTHORIZED, CAPTURED, DECLINED
    status          VARCHAR(50),

    -- Timestamps del ciclo de vida del pago
    created_at      TIMESTAMP,
    processed_at    TIMESTAMP
);
