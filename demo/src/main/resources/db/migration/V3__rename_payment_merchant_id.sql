-- ============================================================
-- V3: Renombrar columna merchat_id a merchant_id en tabla payments
--
-- Corrige un typo en el nombre de la columna para alinearla con 
-- PaymentEntity.java y nombramiento estándar.
-- ============================================================

ALTER TABLE payments RENAME COLUMN merchat_id TO merchant_id;
