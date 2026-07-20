ALTER TABLE maintenance.mantenimientos
    ADD COLUMN IF NOT EXISTS id_moneda INTEGER;

UPDATE maintenance.mantenimientos
SET id_moneda = (
    SELECT id_moneda FROM costs.costo_moneda
    WHERE UPPER(codigo_iso) = 'PEN'
    ORDER BY id_moneda LIMIT 1
)
WHERE id_moneda IS NULL;

ALTER TABLE maintenance.mantenimientos
    ALTER COLUMN id_moneda SET NOT NULL;
