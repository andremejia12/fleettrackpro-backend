package com.fleettrackpro;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class DatabaseInit {

    @Transactional
    public void onStart(@Observes StartupEvent ev) {
        if (ConductorEstadoLaboral.find("nombreEstado", "Cesado").firstResult() == null) {
            ConductorEstadoLaboral cesado = new ConductorEstadoLaboral();
            cesado.nombreEstado = "Cesado";
            cesado.descripcion = "Conductor cesado o dado de baja";
            cesado.persist();
        }
    }
}
