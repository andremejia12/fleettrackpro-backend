package com.fleettrackpro;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.container.ContainerRequestContext;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;
import java.util.Objects;

@Path("/viajes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ViajeResource {
    @Context
    ContainerRequestContext request;

    @GET
    public List<Viaje> listar(@QueryParam("idEmpresa") String idEmpresa) {
        return Viaje.list("idEmpresa", TenantAccess.company(request));
    }

    @POST
    @Transactional
    public Viaje crear(Viaje nuevo) {
        nuevo.idEmpresa = TenantAccess.company(request);
        validarReferencias(nuevo);
        if (nuevo.idViajeEstado == null) nuevo.idViajeEstado = 1;
        if (nuevo.idViajeEstado != 1) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("mensaje", "Todo viaje nuevo debe registrarse como Programado"))
                    .type(MediaType.APPLICATION_JSON)
                    .build());
        }
        Viaje.persist(nuevo);
        return nuevo;
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    public void eliminar(@PathParam("id") Integer id) {
        Viaje viaje = Viaje.findById(id);
        if (viaje == null) {
            throw new NotFoundException("Viaje no encontrado");
        }
        TenantAccess.require(request, viaje.idEmpresa);
        if (viaje.idViajeEstado != null && viaje.idViajeEstado == 4) {
            throw conflicto("No se puede eliminar un viaje completado");
        }
        long checklistCount = ChecklistInspeccion.count("idViaje", id);
        if (checklistCount > 0) {
            throw conflicto("No se puede eliminar un viaje con checklist registrado");
        }
        if (Gasto.count("idViaje", id) > 0 || IngresoServicio.count("idViaje", id) > 0) {
            throw conflicto("No se puede eliminar un viaje con gastos o ingresos asociados");
        }
        viaje.delete();
    }

    @GET
    @Path("/{id}")
    public Viaje obtener(@PathParam("id") Integer id) {
        Viaje viaje = Viaje.findById(id);
        if (viaje == null) {
            throw new NotFoundException("Viaje no encontrado");
        }
        TenantAccess.require(request, viaje.idEmpresa);
        return viaje;
    }

    @PUT
    @Path("/{id}")
    @Transactional
    public Viaje actualizar(@PathParam("id") Integer id, Viaje actualizado) {
        Viaje viaje = Viaje.findById(id);
        if (viaje == null) {
            throw new NotFoundException("Viaje no encontrado");
        }
        TenantAccess.require(request, viaje.idEmpresa);
        if (viaje.idViajeEstado != null && (viaje.idViajeEstado == 4 || viaje.idViajeEstado == 5)) {
            throw conflicto("No se puede editar un viaje finalizado");
        }
        actualizado.idEmpresa = viaje.idEmpresa;
        validarReferencias(actualizado);
        if (actualizado.idViajeEstado == null || !actualizado.idViajeEstado.equals(viaje.idViajeEstado)) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("mensaje", "El estado solo puede cambiarse desde la tabla de viajes"))
                    .type(MediaType.APPLICATION_JSON)
                    .build());
        }
        if (actualizado.idViajeEstado != null && actualizado.idViajeEstado == 4) {
            validarChecklistObligatorio(id);
            validarDatosFinalizacion(actualizado.fechaLlegada, actualizado.kilometrajeLlegada, viaje);
        }
        if (viaje.idViajeEstado != null && viaje.idViajeEstado == 3
                && (!Objects.equals(viaje.idVehiculo, actualizado.idVehiculo)
                    || !Objects.equals(viaje.idConductor, actualizado.idConductor)
                    || !Objects.equals(viaje.idOrdenTrabajo, actualizado.idOrdenTrabajo)
                    || !Objects.equals(viaje.origen, actualizado.origen)
                    || !Objects.equals(viaje.destino, actualizado.destino)
                    || !Objects.equals(viaje.fechaSalida, actualizado.fechaSalida)
                    || !Objects.equals(viaje.kilometrajeSalida, actualizado.kilometrajeSalida))) {
            throw conflicto("En ruta solo se puede editar la llegada estimada y el volumen");
        }
        viaje.idVehiculo = actualizado.idVehiculo;
        viaje.idConductor = actualizado.idConductor;
        viaje.idOrdenTrabajo = actualizado.idOrdenTrabajo;
        viaje.origen = actualizado.origen;
        viaje.destino = actualizado.destino;
        viaje.fechaSalida = actualizado.fechaSalida;
        viaje.fechaLlegada = actualizado.fechaLlegada;
        viaje.fechaLlegadaEstimada = actualizado.fechaLlegadaEstimada;
        viaje.idViajeEstado = actualizado.idViajeEstado;
        viaje.ordenTrabajoNro = actualizado.ordenTrabajoNro;
        viaje.kilometrajeSalida = actualizado.kilometrajeSalida;
        viaje.kilometrajeLlegada = actualizado.kilometrajeLlegada;
        viaje.volumenAtendidoM3 = actualizado.volumenAtendidoM3;
        return viaje;
    }

    private void validarReferencias(Viaje viaje) {
        String empresa = TenantAccess.company(request);
        Vehiculo vehiculo = Vehiculo.findById(viaje.idVehiculo);
        Conductor conductor = Conductor.findById(viaje.idConductor);
        if (vehiculo == null || conductor == null
                || !empresa.equals(vehiculo.idEmpresa) || !empresa.equals(conductor.idEmpresa)) {
            throw new WebApplicationException("Vehículo o conductor no pertenece a tu empresa", 400);
        }
        if (viaje.idOrdenTrabajo != null) {
            OrdenTrabajo orden = OrdenTrabajo.findById(viaje.idOrdenTrabajo);
            if (orden == null || !empresa.equals(orden.idEmpresa)) {
                throw new WebApplicationException("La orden de trabajo no pertenece a tu empresa", 400);
            }
        }
    }

    public static class EstadoRequest {
        public Integer idEstado;
        public LocalDateTime fechaLlegada;
        public Integer kilometrajeLlegada;
    }

    @PUT
    @Path("/{id}/estado")
    @Transactional
    public Viaje cambiarEstado(@PathParam("id") Integer id, EstadoRequest cambio) {
        Viaje viaje = Viaje.findById(id);
        if (viaje == null) throw new NotFoundException("Viaje no encontrado");
        TenantAccess.require(request, viaje.idEmpresa);
        if (cambio == null || cambio.idEstado == null) {
            throw new WebApplicationException("Selecciona un estado", 400);
        }
        int actual = viaje.idViajeEstado == null ? 1 : viaje.idViajeEstado;
        int siguiente = cambio.idEstado;
        boolean transicionValida = (actual == 1 && (siguiente == 2 || siguiente == 5))
                || (actual == 2 && (siguiente == 3 || siguiente == 5))
                || (actual == 3 && (siguiente == 4 || siguiente == 5));
        if (!transicionValida) {
            throw new WebApplicationException("La transición de estado no está permitida", 400);
        }
        if (siguiente == 5 && (Gasto.count("idViaje", id) > 0 || IngresoServicio.count("idViaje", id) > 0)) {
            throw conflicto("No se puede cancelar un viaje con gastos o ingresos asociados");
        }
        if (siguiente == 4) {
            validarChecklistObligatorio(id);
            validarDatosFinalizacion(cambio.fechaLlegada, cambio.kilometrajeLlegada, viaje);
            viaje.fechaLlegada = cambio.fechaLlegada;
            viaje.kilometrajeLlegada = cambio.kilometrajeLlegada;
        }
        viaje.idViajeEstado = siguiente;
        return viaje;
    }

    private void validarChecklistObligatorio(Integer idViaje) {
        ChecklistInspeccion checklist = ChecklistInspeccion.find("idViaje", idViaje).firstResult();
        boolean completo = checklist != null
                && checklist.nivelCombustibleProporcion != null && !checklist.nivelCombustibleProporcion.isBlank()
                && checklist.estadoNeumaticos != null && !checklist.estadoNeumaticos.isBlank()
                && checklist.tieneHerramientasEmergencia != null && !checklist.tieneHerramientasEmergencia.isBlank()
                && checklist.lucesOperativas != null && !checklist.lucesOperativas.isBlank()
                && checklist.firmaDigitalConductorUrl != null && !checklist.firmaDigitalConductorUrl.isBlank();
        if (!completo) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("mensaje", "El checklist es obligatorio"))
                    .type(MediaType.APPLICATION_JSON)
                    .build());
        }
    }

    private void validarDatosFinalizacion(LocalDateTime fechaLlegada, Integer kilometrajeLlegada, Viaje viaje) {
        if (fechaLlegada == null || kilometrajeLlegada == null) {
            throw new WebApplicationException("Fecha y kilometraje de llegada son obligatorios", 400);
        }
        if (viaje.fechaSalida != null && fechaLlegada.isBefore(viaje.fechaSalida)) {
            throw new WebApplicationException("La llegada no puede ser anterior a la salida", 400);
        }
        if (viaje.kilometrajeSalida != null && kilometrajeLlegada < viaje.kilometrajeSalida) {
            throw new WebApplicationException("El kilometraje de llegada no puede ser menor al de salida", 400);
        }
    }

    private WebApplicationException conflicto(String mensaje) {
        return new WebApplicationException(Response.status(Response.Status.CONFLICT)
                .entity(Map.of("message", mensaje))
                .type(MediaType.APPLICATION_JSON)
                .build());
    }
}
