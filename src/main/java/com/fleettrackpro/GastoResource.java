package com.fleettrackpro;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.container.ContainerRequestContext;
import java.util.List;
import java.util.Objects;
import java.util.Map;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

@Path("/gastos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GastoResource {
    @Context ContainerRequestContext request;

    private void validarMetodoPago(Gasto gasto) {
        PaymentCurrencyValidator.validate(gasto.idMetodoPago, gasto.idMoneda);
    }

    private void validarDatos(Gasto gasto, Integer idActual) {
        if (gasto.monto == null || gasto.monto.signum() <= 0) {
            throw error(Response.Status.BAD_REQUEST, "El monto del gasto debe ser mayor que cero");
        }
        if (gasto.fechaGasto == null) {
            throw error(Response.Status.BAD_REQUEST, "La fecha del gasto es obligatoria");
        }
        if (gasto.kilometrajeRegistro != null && gasto.kilometrajeRegistro < 0) {
            throw error(Response.Status.BAD_REQUEST, "El kilometraje no puede ser negativo");
        }
        if (gasto.comprobanteNro != null && !gasto.comprobanteNro.isBlank()) {
            String numero = gasto.comprobanteNro.trim().toLowerCase();
            Gasto duplicado = Gasto.find("lower(comprobanteNro) = ?1 and idEmpresa = ?2", numero, gasto.idEmpresa).firstResult();
            if (duplicado != null && !Objects.equals(duplicado.idGasto, idActual)) {
                throw error(Response.Status.CONFLICT, "Ya existe un gasto con ese número de comprobante");
            }
            gasto.comprobanteNro = gasto.comprobanteNro.trim();
        }
    }

    private void validarViaje(Gasto gasto) {
        CostoGastoCategoria categoria = gasto.idGastoCategoria == null
                ? null : CostoGastoCategoria.findById(gasto.idGastoCategoria);
        boolean esCfp = categoria != null && categoria.tipoCosto != null
                && "CFP".equalsIgnoreCase(categoria.tipoCosto.trim());
        if (gasto.idVehiculo == null) {
            if (esCfp && gasto.idViaje == null) {
                return;
            }
            throw new BadRequestException("Selecciona un vehículo para este tipo de gasto");
        }
        Vehiculo vehiculo = Vehiculo.findById(gasto.idVehiculo);
        if (vehiculo == null || !TenantAccess.company(request).equals(vehiculo.idEmpresa)) {
            throw new BadRequestException("El vehículo no pertenece a tu empresa");
        }
        if (gasto.idViaje == null) return;
        Viaje viaje = Viaje.findById(gasto.idViaje);
        if (viaje == null || viaje.idViajeEstado == null || viaje.idViajeEstado != 4 || viaje.fechaLlegada == null) {
            throw new BadRequestException("Solo puedes asociar gastos a viajes completados");
        }
        if (!viaje.idVehiculo.equals(gasto.idVehiculo)
                || (gasto.idEmpresa != null && !gasto.idEmpresa.equals(viaje.idEmpresa))) {
            throw new BadRequestException("El viaje no pertenece al vehículo o empresa seleccionados");
        }
        if (gasto.fechaGasto == null
                || gasto.fechaGasto.toLocalDate().isBefore(viaje.fechaSalida.toLocalDate())
                || gasto.fechaGasto.toLocalDate().isAfter(viaje.fechaLlegada.toLocalDate())) {
            throw new BadRequestException("La fecha del gasto debe estar dentro de las fechas del viaje");
        }
    }

    @GET
    public List<Gasto> listar(@QueryParam("idEmpresa") String idEmpresa) {
        return Gasto.list("idEmpresa", TenantAccess.company(request));
    }

    @GET
    @Path("/{id}")
    public Gasto obtener(@PathParam("id") Integer id) {
        Gasto gasto = Gasto.findById(id);
        if (gasto == null) {
            throw new NotFoundException("Gasto no encontrado");
        }
        TenantAccess.require(request, gasto.idEmpresa);
        return gasto;
    }

    @POST
    @Transactional
    public Gasto crear(Gasto nuevo) {
        RoleAccess.requireRole(request, "admin", "contador");
        nuevo.idEmpresa = TenantAccess.company(request);
        validarMetodoPago(nuevo);
        validarDatos(nuevo, null);
        validarViaje(nuevo);
        nuevo.persist();
        return nuevo;
    }

    @PUT
    @Path("/{id}")
    @Transactional
    public Gasto actualizar(@PathParam("id") Integer id, Gasto actualizado) {
        RoleAccess.requireRole(request, "admin", "contador");
        Gasto gasto = Gasto.findById(id);
        if (gasto == null) {
            throw new NotFoundException("Gasto no encontrado");
        }
        TenantAccess.require(request, gasto.idEmpresa);
        if (!Objects.equals(gasto.idVehiculo, actualizado.idVehiculo)
                || !Objects.equals(gasto.idViaje, actualizado.idViaje)) {
            throw new BadRequestException("No se puede cambiar el vehículo ni el viaje de un gasto registrado");
        }
        actualizado.idEmpresa = gasto.idEmpresa;
        validarMetodoPago(actualizado);
        validarDatos(actualizado, id);
        validarViaje(actualizado);
        gasto.idGastoCategoria = actualizado.idGastoCategoria;
        gasto.monto = actualizado.monto;
        gasto.idMoneda = actualizado.idMoneda;
        gasto.fechaGasto = actualizado.fechaGasto;
        gasto.descripcion = actualizado.descripcion;
        gasto.idComprobanteTipo = actualizado.idComprobanteTipo;
        gasto.comprobanteNro = actualizado.comprobanteNro;
        gasto.kilometrajeRegistro = actualizado.kilometrajeRegistro;
        gasto.idMetodoPago = actualizado.idMetodoPago;
        gasto.cantidadGalones = actualizado.cantidadGalones;
        return gasto;
    }

    private WebApplicationException error(Response.Status estado, String mensaje) {
        return new WebApplicationException(Response.status(estado)
                .entity(Map.of("message", mensaje))
                .type(MediaType.APPLICATION_JSON)
                .build());
    }
}
