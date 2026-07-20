INSERT INTO saas_admin.gasto_interno_categoria (id_categoria, nombre_categoria)
SELECT COALESCE(MAX(id_categoria), 0) + 1, 'Infraestructura'
FROM saas_admin.gasto_interno_categoria
HAVING NOT EXISTS (
    SELECT 1 FROM saas_admin.gasto_interno_categoria WHERE lower(nombre_categoria) = 'infraestructura'
);

INSERT INTO saas_admin.gasto_interno_categoria (id_categoria, nombre_categoria)
SELECT COALESCE(MAX(id_categoria), 0) + 1, 'Planillas'
FROM saas_admin.gasto_interno_categoria
HAVING NOT EXISTS (
    SELECT 1 FROM saas_admin.gasto_interno_categoria WHERE lower(nombre_categoria) = 'planillas'
);

INSERT INTO saas_admin.gasto_interno_categoria (id_categoria, nombre_categoria)
SELECT COALESCE(MAX(id_categoria), 0) + 1, 'Marketing'
FROM saas_admin.gasto_interno_categoria
HAVING NOT EXISTS (
    SELECT 1 FROM saas_admin.gasto_interno_categoria WHERE lower(nombre_categoria) = 'marketing'
);

INSERT INTO saas_admin.gasto_interno_categoria (id_categoria, nombre_categoria)
SELECT COALESCE(MAX(id_categoria), 0) + 1, 'Servicios profesionales'
FROM saas_admin.gasto_interno_categoria
HAVING NOT EXISTS (
    SELECT 1 FROM saas_admin.gasto_interno_categoria WHERE lower(nombre_categoria) = 'servicios profesionales'
);

INSERT INTO saas_admin.gasto_interno_categoria (id_categoria, nombre_categoria)
SELECT COALESCE(MAX(id_categoria), 0) + 1, 'Otros'
FROM saas_admin.gasto_interno_categoria
HAVING NOT EXISTS (
    SELECT 1 FROM saas_admin.gasto_interno_categoria WHERE lower(nombre_categoria) = 'otros'
);
