-- ============================================================
-- V2: Creación de la tabla api_keys
--
-- Esta migration refleja la estructura definida en PaymentEntity.java
-- A partir de aquí, Flyway gestiona el esquema — Hibernate solo lo valida.
--
-- Convención de nombres Flyway:
--   V{numero}__{descripcion}.sql
--   El doble guion bajo es obligatorio.
-- ============================================================


CREATE TABLE IF NOT EXISTS api_keys(
    id              VARCHAR(36)     PRIMARY KEY,

    merchant_id     VARCHAR(250) NOT NULL,

    key_prefix      VARCHAR(15) NOT NULL UNIQUE,

    hashed_key      VARCHAR(255) NOT NULL,

    is_active       BOOLEAN DEFAULT TRUE,

    created_at      TIMESTAMP,
    
    expires_at      TIMESTAMP
);
