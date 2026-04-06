-- ============================================================
-- V8: Actualizar el CHECK constraint de la columna 'status'
--
-- Hibernate 6 genera CHECK constraints automáticos para enums.
-- Al añadir DISPUTED al enum PaymentStatus, debemos actualizar
-- el constraint para que PostgreSQL permita el nuevo valor.
-- ============================================================

-- Eliminamos el constraint viejo generado por Hibernate
ALTER TABLE payments DROP CONSTRAINT IF EXISTS payments_status_check;

-- Recreamos el constraint incluyendo todos los estados actuales del enum
ALTER TABLE payments ADD CONSTRAINT payments_status_check
    CHECK (status IN (
        'PENDING',
        'AUTHORIZED',
        'CAPTURED',
        'DECLINED',
        'REFUNDED',
        'DISPUTED',
        'FAILED'
    ));
