package com.fleettrackpro;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

@ApplicationScoped
public class DatabaseInit {
    @Inject
    EntityManager em;

    @Transactional
    public void onStart(@Observes StartupEvent ev) {
        if (ConductorEstadoLaboral.find("nombreEstado", "Cesado").firstResult() == null) {
            ConductorEstadoLaboral cesado = new ConductorEstadoLaboral();
            cesado.nombreEstado = "Cesado";
            cesado.descripcion = "Conductor cesado o dado de baja";
            cesado.persist();
        }
        em.createNativeQuery("""
                INSERT INTO companies.empresa_suscripcion_estado (nombre_estado, descripcion)
                SELECT seed.nombre_estado, seed.descripcion
                FROM (VALUES
                    ('Prueba', 'Periodo de prueba de la suscripción'),
                    ('Activo', 'Suscripción activa'),
                    ('Suspendido', 'Suscripción suspendida')
                ) AS seed(nombre_estado, descripcion)
                WHERE NOT EXISTS (
                    SELECT 1
                    FROM companies.empresa_suscripcion_estado existente
                    WHERE lower(existente.nombre_estado) = lower(seed.nombre_estado)
                )
                """).executeUpdate();
    }
}
