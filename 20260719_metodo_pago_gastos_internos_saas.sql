ALTER TABLE saas_admin.gastos_internos_saas
    ADD COLUMN IF NOT EXISTS id_metodo_pago INTEGER;

UPDATE saas_admin.gastos_internos_saas
SET id_metodo_pago = (
    SELECT id_metodo_pago
    FROM saas_admin.metodo_pago
    WHERE lower(nombre_metodo) = 'efectivo'
    LIMIT 1
)
WHERE id_metodo_pago IS NULL;

ALTER TABLE saas_admin.gastos_internos_saas
    ALTER COLUMN id_metodo_pago SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk_gastos_internos_saas_metodo_pago'
    ) THEN
        ALTER TABLE saas_admin.gastos_internos_saas
            ADD CONSTRAINT fk_gastos_internos_saas_metodo_pago
            FOREIGN KEY (id_metodo_pago)
            REFERENCES saas_admin.metodo_pago(id_metodo_pago);
    END IF;
END $$;
