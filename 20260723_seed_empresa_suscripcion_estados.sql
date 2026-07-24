INSERT INTO companies.empresa_suscripcion_estado (nombre_estado, descripcion)
SELECT seed.nombre_estado, seed.descripcion
FROM (
    VALUES
        ('Prueba', 'Periodo de prueba de la suscripción'),
        ('Activo', 'Suscripción activa'),
        ('Suspendido', 'Suscripción suspendida')
) AS seed(nombre_estado, descripcion)
WHERE NOT EXISTS (
    SELECT 1
    FROM companies.empresa_suscripcion_estado existente
    WHERE lower(existente.nombre_estado) = lower(seed.nombre_estado)
);
