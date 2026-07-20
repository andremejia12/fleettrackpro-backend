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

@Path("/vehiculos")
public class VehiculoResource {
    @Context
    ContainerRequestContext request;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Vehiculo> listar(@QueryParam("idEmpresa") String idEmpresa) {
        return Vehiculo.list("idEmpresa", TenantAccess.company(request));
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Vehiculo obtenerPorId(@PathParam("id") Integer id) {
        Vehiculo vehiculo = Vehiculo.findById(id);
        if (vehiculo == null) {
            throw new NotFoundException("Vehículo no encontrado");
        }
        TenantAccess.require(request, vehiculo.idEmpresa);
        return vehiculo;
    }

    @POST
    @Transactional
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Vehiculo crear(Vehiculo nuevo) {
        nuevo.idEmpresa = TenantAccess.company(request);
        if (nuevo.fechaRegistro == null) {
            nuevo.fechaRegistro = java.time.LocalDateTime.now();
        }
        Vehiculo.persist(nuevo);
        return nuevo;
    }

    @PUT
    @Path("/{id}")
    @Transactional
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Vehiculo actualizar(@PathParam("id") Integer id, Vehiculo actualizado) {
        Vehiculo vehiculo = Vehiculo.findById(id);
        if (vehiculo == null) {
            throw new NotFoundException("Vehículo no encontrado");
        }
        TenantAccess.require(request, vehiculo.idEmpresa);
        vehiculo.placa = actualizado.placa;
        vehiculo.idMarca = actualizado.idMarca;
        vehiculo.idModelo = actualizado.idModelo;
        vehiculo.anio = actualizado.anio;
        vehiculo.capacidadCarga = actualizado.capacidadCarga;
        vehiculo.idEstadoOperativo = actualizado.idEstadoOperativo;
        vehiculo.idColor = actualizado.idColor;
        vehiculo.idTipoUnidad = actualizado.idTipoUnidad;
        vehiculo.idPropiedadTipo = actualizado.idPropiedadTipo;
        vehiculo.nroChasisVin = actualizado.nroChasisVin;
        vehiculo.kilometrajeActual = actualizado.kilometrajeActual;
        vehiculo.soatVencimiento = actualizado.soatVencimiento;
        vehiculo.revisionTecnicaVencimiento = actualizado.revisionTecnicaVencimiento;
        if (actualizado.fechaRegistro != null) {
            vehiculo.fechaRegistro = actualizado.fechaRegistro;
        }
        return vehiculo;
    }
}
