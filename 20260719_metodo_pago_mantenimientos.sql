ALTER TABLE maintenance.mantenimientos
    ADD COLUMN IF NOT EXISTS id_metodo_pago INTEGER;

UPDATE maintenance.mantenimientos
SET id_metodo_pago = (
    SELECT id_metodo_pago
    FROM saas_admin.metodo_pago
    WHERE LOWER(nombre_metodo) = 'efectivo'
    ORDER BY id_metodo_pago
    LIMIT 1
)
WHERE id_metodo_pago IS NULL;

ALTER TABLE maintenance.mantenimientos
    ALTER COLUMN id_metodo_pago SET NOT NULL;
