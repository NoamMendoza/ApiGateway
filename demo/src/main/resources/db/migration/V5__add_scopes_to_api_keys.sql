-- Agregar columna de permisos (scopes) a las llaves de acceso.
-- Asignamos 'CHARGE' por defecto para que las llaves previas sigan funcionando.
ALTER TABLE api_keys ADD COLUMN scopes VARCHAR(255) DEFAULT 'CHARGE';
