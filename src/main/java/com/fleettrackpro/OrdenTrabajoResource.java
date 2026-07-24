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
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.container.ContainerRequestContext;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;
import jakarta.ws.rs.QueryParam;

@Path("/ordenes-trabajo")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OrdenTrabajoResource {
    @Context ContainerRequestContext request;

    private void validar(OrdenTrabajo orden) {
        if (orden.idVehiculo == null || orden.idConductor == null
                || orden.tipoIncidencia == null || orden.tipoIncidencia.isBlank()
                || orden.idPrioridad == null) {
            throw new WebApplicationException("Completa los campos obligatorios", 400);
        }
        if (orden.idViajeEstado == null || orden.idViajeEstado < 1 || orden.idViajeEstado > 5) {
            throw new WebApplicationException("Estado de orden no válido", 400);
        }
        String empresa = TenantAccess.company(request);
        Vehiculo vehiculo = Vehiculo.findById(orden.idVehiculo);
        Conductor conductor = Conductor.findById(orden.idConductor);
        if (vehiculo == null || conductor == null
                || !empresa.equals(vehiculo.idEmpresa) || !empresa.equals(conductor.idEmpresa)) {
            throw new WebApplicationException("Vehículo o conductor no pertenece a tu empresa", 400);
        }
    }

    @GET
    public List<OrdenTrabajo> listar(@QueryParam("idEmpresa") String idEmpresa) {
        return OrdenTrabajo.list("idEmpresa", TenantAccess.company(request));
    }

    @POST
    @Transactional
    public OrdenTrabajo crear(OrdenTrabajo nuevo) {
        RoleAccess.requireRole(request, "admin", "despachador", "mecanico");
        nuevo.idEmpresa = TenantAccess.company(request);
        validar(nuevo);
        if (nuevo.fechaRegistro == null) nuevo.fechaRegistro = LocalDateTime.now();
        OrdenTrabajo.persist(nuevo);
        return nuevo;
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    public void eliminar(@PathParam("id") Integer id) {
        RoleAccess.requireRole(request, "admin", "despachador", "mecanico");
        OrdenTrabajo orden = OrdenTrabajo.findById(id);
        if (orden == null) {
            throw new NotFoundException("Orden de trabajo no encontrada");
        }
        TenantAccess.require(request, orden.idEmpresa);
        long viajesCount = Viaje.count("idOrdenTrabajo", id);
        if (viajesCount > 0) {
            throw new WebApplicationException(
                    Response.status(Response.Status.CONFLICT)
                            .entity(Map.of("message", "No se puede eliminar una orden de trabajo con viajes asociados"))
                            .type(MediaType.APPLICATION_JSON)
                            .build());
        }
        orden.delete();
    }

    @GET
    @Path("/{id}")
    public OrdenTrabajo obtener(@PathParam("id") Integer id) {
        OrdenTrabajo orden = OrdenTrabajo.findById(id);
        if (orden == null) {
            throw new NotFoundException("Orden de trabajo no encontrada");
        }
        TenantAccess.require(request, orden.idEmpresa);
        return orden;
    }

    @PUT
    @Path("/{id}")
    @Transactional
    public OrdenTrabajo actualizar(@PathParam("id") Integer id, OrdenTrabajo actualizado) {
        RoleAccess.requireRole(request, "admin", "despachador", "mecanico");
        OrdenTrabajo orden = OrdenTrabajo.findById(id);
        if (orden == null) {
            throw new NotFoundException("Orden de trabajo no encontrada");
        }
        TenantAccess.require(request, orden.idEmpresa);
        actualizado.idEmpresa = orden.idEmpresa;
        validar(actualizado);
        orden.idVehiculo = actualizado.idVehiculo;
        orden.idConductor = actualizado.idConductor;
        orden.tipoIncidencia = actualizado.tipoIncidencia;
        orden.descripcion = actualizado.descripcion;
        orden.direccion = actualizado.direccion;
        orden.idPrioridad = actualizado.idPrioridad;
        orden.idViajeEstado = actualizado.idViajeEstado;
        return orden;
    }
}
