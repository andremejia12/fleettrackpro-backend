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
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.container.ContainerRequestContext;
import java.util.List;
import java.util.Map;

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
        RoleAccess.requireRole(request, "admin", "despachador");
        String idEmpresa = TenantAccess.company(request);
        Empresa empresa = Empresa.findById(idEmpresa);
        EmpresaSaasPlan plan = empresa == null || empresa.idSaasPlan == null
                ? null
                : EmpresaSaasPlan.findById(empresa.idSaasPlan);
        if (plan != null && plan.limiteVehiculos != null) {
            long cantidadVehiculos = Vehiculo.count("idEmpresa", idEmpresa);
            if (cantidadVehiculos >= plan.limiteVehiculos) {
                throw new WebApplicationException(Response.status(Response.Status.CONFLICT)
                        .entity(Map.of("message",
                                "Se alcanzó el límite de vehículos permitido por su plan actual ("
                                        + plan.limiteVehiculos
                                        + "). Actualice su plan para agregar más vehículos."))
                        .type(MediaType.APPLICATION_JSON)
                        .build());
            }
        }
        nuevo.idEmpresa = idEmpresa;
        if (nuevo.idSucursalBase != null && nuevo.idSucursalBase.isBlank()) {
            nuevo.idSucursalBase = null;
        }
        validarSucursal(nuevo.idSucursalBase, idEmpresa);
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
        RoleAccess.requireRole(request, "admin", "despachador");
        Vehiculo vehiculo = Vehiculo.findById(id);
        if (vehiculo == null) {
            throw new NotFoundException("Vehículo no encontrado");
        }
        TenantAccess.require(request, vehiculo.idEmpresa);
        if (actualizado.idSucursalBase != null && actualizado.idSucursalBase.isBlank()) {
            actualizado.idSucursalBase = null;
        }
        validarSucursal(actualizado.idSucursalBase, vehiculo.idEmpresa);
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
        vehiculo.idSucursalBase = actualizado.idSucursalBase;
        vehiculo.valorCompra = actualizado.valorCompra;
        vehiculo.valorResidual = actualizado.valorResidual;
        vehiculo.vidaUtilAnios = actualizado.vidaUtilAnios;
        if (actualizado.fechaRegistro != null) {
            vehiculo.fechaRegistro = actualizado.fechaRegistro;
        }
        return vehiculo;
    }

    private void validarSucursal(String idSucursal, String idEmpresa) {
        if (idSucursal == null || idSucursal.isBlank()) {
            return;
        }
        SucursalGarita sucursal = SucursalGarita.findById(idSucursal);
        if (sucursal == null || !idEmpresa.equals(sucursal.idEmpresa)) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "La sede base no pertenece a tu empresa"))
                    .type(MediaType.APPLICATION_JSON)
                    .build());
        }
    }
}
