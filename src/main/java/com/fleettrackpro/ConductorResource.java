package com.fleettrackpro;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.container.ContainerRequestContext;
import java.util.List;

@Path("/conductores")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ConductorResource {
    @Context
    ContainerRequestContext request;

    @GET
    public List<Conductor> listar(@QueryParam("idEmpresa") String idEmpresa) {
        return Conductor.list("idEmpresa", TenantAccess.company(request));
    }

    @POST
    @Transactional
    public Conductor crear(Conductor nuevo) {
        nuevo.idEmpresa = TenantAccess.company(request);
        Conductor.persist(nuevo);
        return nuevo;
    }

    @PUT
    @Path("/{id}")
    @Transactional
    public Conductor actualizar(@PathParam("id") Integer id, Conductor actualizado) {
        Conductor conductor = Conductor.findById(id);
        if (conductor == null) {
            throw new NotFoundException("Conductor no encontrado");
        }
        TenantAccess.require(request, conductor.idEmpresa);
        conductor.numeroDocumento = actualizado.numeroDocumento;
        conductor.idTipoDocumento = actualizado.idTipoDocumento;
        conductor.nombre = actualizado.nombre;
        conductor.apellido = actualizado.apellido;
        conductor.licenciaNro = actualizado.licenciaNro;
        conductor.telefono = actualizado.telefono;
        conductor.email = actualizado.email;
        conductor.puesto = actualizado.puesto;
        conductor.idEstadoLaboral = actualizado.idEstadoLaboral;
        conductor.idCategoria = actualizado.idCategoria;
        conductor.licenciaVencimiento = actualizado.licenciaVencimiento;
        conductor.idTipoSangre = actualizado.idTipoSangre;
        conductor.contactoEmergencia = actualizado.contactoEmergencia;
        conductor.costoHora = actualizado.costoHora;
        return conductor;
    }

    @GET
    @Path("/{id}")
    public Conductor obtener(@PathParam("id") Integer id) {
        Conductor conductor = Conductor.findById(id);
        if (conductor == null) {
            throw new NotFoundException("Conductor no encontrado");
        }
        TenantAccess.require(request, conductor.idEmpresa);
        return conductor;
    }
}
