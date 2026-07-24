package com.fleettrackpro;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.container.ContainerRequestContext;
import java.time.LocalDateTime;
import java.util.UUID;

@Path("/checklist")
public class ChecklistResource {
    @Context ContainerRequestContext request;

    private Viaje requireViaje(Integer idViaje) {
        Viaje viaje = Viaje.findById(idViaje);
        if (viaje == null) throw new NotFoundException("Viaje no encontrado");
        TenantAccess.require(request, viaje.idEmpresa);
        if (RoleAccess.isConductor(request)
                && !RoleAccess.conductorIdFor(request).equals(viaje.idConductor)) {
            throw new WebApplicationException(Response.status(Response.Status.FORBIDDEN)
                    .entity(java.util.Map.of("message", "El viaje no pertenece al conductor autenticado"))
                    .type(MediaType.APPLICATION_JSON)
                    .build());
        }
        return viaje;
    }

    @GET
    @Path("/viaje/{idViaje}")
    @Produces(MediaType.APPLICATION_JSON)
    public ChecklistInspeccion porViaje(@PathParam("idViaje") Integer idViaje) {
        requireViaje(idViaje);
        return ChecklistInspeccion.find("idViaje", idViaje).firstResult();
    }

    @POST
    @Transactional
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ChecklistInspeccion crear(ChecklistInspeccion nuevo) {
        requireViaje(nuevo.idViaje);
        if (nuevo.nivelCombustibleProporcion == null || nuevo.nivelCombustibleProporcion.isBlank()
                || nuevo.estadoNeumaticos == null || nuevo.estadoNeumaticos.isBlank()
                || nuevo.tieneHerramientasEmergencia == null || nuevo.tieneHerramientasEmergencia.isBlank()
                || nuevo.lucesOperativas == null || nuevo.lucesOperativas.isBlank()
                || nuevo.firmaDigitalConductorUrl == null || nuevo.firmaDigitalConductorUrl.isBlank()) {
            throw new BadRequestException("Completa y firma el checklist antes de guardarlo");
        }
        nuevo.idChecklist = "CHK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        nuevo.fechaInspeccion = LocalDateTime.now();
        ChecklistInspeccion.persist(nuevo);
        return nuevo;
    }
}
