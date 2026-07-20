BEGIN;

ALTER TABLE costs.gastos ADD COLUMN IF NOT EXISTS id_metodo_pago integer;
ALTER TABLE costs.ingresos_servicios ADD COLUMN IF NOT EXISTS id_metodo_pago integer;

UPDATE costs.gastos
SET id_metodo_pago = (SELECT id_metodo_pago FROM saas_admin.metodo_pago WHERE lower(nombre_metodo) = 'efectivo' LIMIT 1)
WHERE id_metodo_pago IS NULL;

UPDATE costs.ingresos_servicios
SET id_metodo_pago = (SELECT id_metodo_pago FROM saas_admin.metodo_pago WHERE lower(nombre_metodo) = 'efectivo' LIMIT 1)
WHERE id_metodo_pago IS NULL;

ALTER TABLE costs.gastos ALTER COLUMN id_metodo_pago SET NOT NULL;
ALTER TABLE costs.ingresos_servicios ALTER COLUMN id_metodo_pago SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_gastos_metodo_pago') THEN
        ALTER TABLE costs.gastos ADD CONSTRAINT fk_gastos_metodo_pago
            FOREIGN KEY (id_metodo_pago) REFERENCES saas_admin.metodo_pago(id_metodo_pago);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_ingresos_metodo_pago') THEN
        ALTER TABLE costs.ingresos_servicios ADD CONSTRAINT fk_ingresos_metodo_pago
            FOREIGN KEY (id_metodo_pago) REFERENCES saas_admin.metodo_pago(id_metodo_pago);
    END IF;
END $$;

COMMIT;
