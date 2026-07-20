package com.fleettrackpro;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.container.ContainerRequestContext;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

@Path("/ingresos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class IngresoServicioResource {
    @Context ContainerRequestContext request;

    private void validarMetodoPago(IngresoServicio ingreso) {
        PaymentCurrencyValidator.validate(ingreso.idMetodoPago, ingreso.idMoneda);
    }

    private void validarViaje(IngresoServicio ingreso, Integer idActual) {
        if (ingreso.idViaje == null) {
            throw new BadRequestException("El viaje completado es requerido");
        }
        Viaje viaje = Viaje.findById(ingreso.idViaje);
        if (viaje == null || viaje.idViajeEstado == null || viaje.idViajeEstado != 4 || viaje.fechaLlegada == null) {
            throw new BadRequestException("Solo puedes registrar ingresos de viajes completados");
        }
        if (!viaje.idVehiculo.equals(ingreso.idVehiculo)
                || (ingreso.idEmpresa != null && !ingreso.idEmpresa.equals(viaje.idEmpresa))) {
            throw new BadRequestException("El viaje no pertenece al vehículo o empresa seleccionados");
        }
        IngresoServicio duplicado = IngresoServicio.find("idViaje", ingreso.idViaje).firstResult();
        if (duplicado != null && !Objects.equals(duplicado.idIngreso, idActual)) {
            throw error(Response.Status.CONFLICT, "El viaje ya tiene un ingreso registrado");
        }
        ingreso.idViajeEstado = viaje.idViajeEstado;
    }

    private void validarDatos(IngresoServicio ingreso) {
        if (ingreso.montoCobrado == null || ingreso.montoCobrado <= 0) {
            throw error(Response.Status.BAD_REQUEST, "El monto cobrado debe ser mayor que cero");
        }
        if (ingreso.fechaPago == null) {
            throw error(Response.Status.BAD_REQUEST, "La fecha de pago es obligatoria");
        }
    }

    @GET
    public List<IngresoServicio> listar(@QueryParam("idEmpresa") String idEmpresa) {
        return IngresoServicio.list("idEmpresa", TenantAccess.company(request));
    }

    @POST
    @Transactional
    public IngresoServicio crear(IngresoServicio nuevo) {
        nuevo.idEmpresa = TenantAccess.company(request);
        validarMetodoPago(nuevo);
        validarDatos(nuevo);
        validarViaje(nuevo, null);
        IngresoServicio.persist(nuevo);
        return nuevo;
    }

    @GET
    @Path("/{id}")
    public IngresoServicio obtener(@PathParam("id") Integer id) {
        IngresoServicio ingreso = IngresoServicio.findById(id);
        if (ingreso == null) {
            throw new NotFoundException("Ingreso no encontrado");
        }
        TenantAccess.require(request, ingreso.idEmpresa);
        return ingreso;
    }

    @PUT
    @Path("/{id}")
    @Transactional
    public IngresoServicio actualizar(@PathParam("id") Integer id, IngresoServicio actualizado) {
        IngresoServicio ingreso = IngresoServicio.findById(id);
        if (ingreso == null) {
            throw new NotFoundException("Ingreso no encontrado");
        }
        TenantAccess.require(request, ingreso.idEmpresa);
        if (!Objects.equals(ingreso.idViaje, actualizado.idViaje)
                || !Objects.equals(ingreso.idVehiculo, actualizado.idVehiculo)) {
            throw error(Response.Status.CONFLICT, "No se puede cambiar el viaje ni el vehículo de un ingreso registrado");
        }
        actualizado.idEmpresa = ingreso.idEmpresa;
        validarMetodoPago(actualizado);
        validarDatos(actualizado);
        validarViaje(actualizado, id);
        ingreso.montoCobrado = actualizado.montoCobrado;
        ingreso.costoManoObraAsociado = actualizado.costoManoObraAsociado;
        ingreso.idMoneda = actualizado.idMoneda;
        ingreso.fechaPago = actualizado.fechaPago;
        ingreso.idMetodoPago = actualizado.idMetodoPago;
        return ingreso;
    }

    private WebApplicationException error(Response.Status estado, String mensaje) {
        return new WebApplicationException(Response.status(estado)
                .entity(Map.of("message", mensaje))
                .type(MediaType.APPLICATION_JSON)
                .build());
    }
}
