package com.fleettrackpro;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
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
import java.util.Objects;
import jakarta.ws.rs.QueryParam;

@Path("/mantenimientos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MantenimientoResource {
    @Context
    ContainerRequestContext request;

    @GET
    public List<Mantenimiento> listar(@QueryParam("idEmpresa") String idEmpresa) {
        return Mantenimiento.list("idEmpresa", TenantAccess.company(request));
    }

    @GET
    @Path("/{id}")
    public Mantenimiento obtener(@PathParam("id") Integer id) {
        Mantenimiento mantenimiento = Mantenimiento.findById(id);
        if (mantenimiento == null) {
            throw new NotFoundException("Mantenimiento no encontrado");
        }
        TenantAccess.require(request, mantenimiento.idEmpresa);
        return mantenimiento;
    }

    @POST
    @Transactional
    public Mantenimiento crear(Mantenimiento nuevo) {
        RoleAccess.requireRole(request, "admin", "mecanico");
        nuevo.idEmpresa = TenantAccess.company(request);
        validarVehiculo(nuevo.idVehiculo, true);
        validarDatos(nuevo, null);
        PaymentCurrencyValidator.validate(nuevo.idMetodoPago, nuevo.idMoneda);
        if (nuevo.idTaller != null) {
            TallerCatalogo tc = TallerCatalogo.findById(nuevo.idTaller);
            if (tc != null) {
                nuevo.tallerNombre = tc.nombreTaller;
            }
        }
        Mantenimiento.persist(nuevo);
        return nuevo;
    }

    @jakarta.ws.rs.PUT
    @Path("/{id}")
    @Transactional
    public Mantenimiento actualizar(@PathParam("id") Integer id, Mantenimiento actualizado) {
        RoleAccess.requireRole(request, "admin", "mecanico");
        Mantenimiento mantenimiento = Mantenimiento.findById(id);
        if (mantenimiento == null) {
            throw new NotFoundException("Mantenimiento no encontrado");
        }
        TenantAccess.require(request, mantenimiento.idEmpresa);
        if (!Objects.equals(mantenimiento.idVehiculo, actualizado.idVehiculo)) {
            throw new WebApplicationException(
                    Response.status(Response.Status.CONFLICT)
                            .entity(Map.of("message", "No se puede cambiar el vehículo de un mantenimiento registrado"))
                            .type(MediaType.APPLICATION_JSON)
                            .build());
        }
        boolean finalizado = mantenimiento.fechaSalida != null;
        if (finalizado && (!Objects.equals(mantenimiento.idTipoServicio, actualizado.idTipoServicio)
                || !Objects.equals(mantenimiento.descripcionFalla, actualizado.descripcionFalla)
                || !Objects.equals(mantenimiento.fechaEntrada, actualizado.fechaEntrada)
                || !Objects.equals(mantenimiento.fechaSalida, actualizado.fechaSalida)
                || !Objects.equals(mantenimiento.idTaller, actualizado.idTaller)
                || !Objects.equals(mantenimiento.kilometrajeEntrada, actualizado.kilometrajeEntrada)
                || !Objects.equals(mantenimiento.ordenServicioTaller, actualizado.ordenServicioTaller)
                || !Objects.equals(mantenimiento.garantiaMeses, actualizado.garantiaMeses))) {
            throw conflicto("Un mantenimiento finalizado solo permite corregir los datos económicos y de pago");
        }
        validarVehiculo(actualizado.idVehiculo, false);
        validarDatos(actualizado, id);
        PaymentCurrencyValidator.validate(actualizado.idMetodoPago, actualizado.idMoneda);
        mantenimiento.idTipoServicio = actualizado.idTipoServicio;
        mantenimiento.descripcionFalla = actualizado.descripcionFalla;
        mantenimiento.costoReparacion = actualizado.costoReparacion;
        mantenimiento.fechaEntrada = actualizado.fechaEntrada;
        mantenimiento.fechaSalida = actualizado.fechaSalida;
        mantenimiento.kilometrajeEntrada = actualizado.kilometrajeEntrada;
        mantenimiento.ordenServicioTaller = actualizado.ordenServicioTaller;
        mantenimiento.garantiaMeses = actualizado.garantiaMeses;
        mantenimiento.idMetodoPago = actualizado.idMetodoPago;
        mantenimiento.idMoneda = actualizado.idMoneda;
        mantenimiento.idTaller = actualizado.idTaller;
        mantenimiento.naturalezaMantenimiento = actualizado.naturalezaMantenimiento; // ← NUEVO
        mantenimiento.periodicidadKm = actualizado.periodicidadKm;
        if (actualizado.idTaller != null) {
            TallerCatalogo tc = TallerCatalogo.findById(actualizado.idTaller);
            if (tc != null) {
                mantenimiento.tallerNombre = tc.nombreTaller;
            }
        } else {
            mantenimiento.tallerNombre = actualizado.tallerNombre;
        }

        if (!finalizado) {
            mantenimiento.repuestos.clear();
            if (actualizado.repuestos != null) {
                for (RepuestoMantenimientoDetalle r : actualizado.repuestos) {
                    r.mantenimiento = mantenimiento;
                    mantenimiento.repuestos.add(r);
                }
            }
        }
        return mantenimiento;
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    public void eliminar(@PathParam("id") Integer id) {
        RoleAccess.requireRole(request, "admin", "mecanico");
        Mantenimiento mantenimiento = Mantenimiento.findById(id);
        if (mantenimiento == null) {
            throw new NotFoundException("Mantenimiento no encontrado");
        }
        TenantAccess.require(request, mantenimiento.idEmpresa);
        if (mantenimiento.fechaSalida != null) {
            throw conflicto("No se puede eliminar un mantenimiento finalizado");
        }
        long repuestosCount = RepuestoMantenimientoDetalle.count("mantenimiento", mantenimiento);
        if (repuestosCount > 0) {
            throw new WebApplicationException(
                    Response.status(Response.Status.CONFLICT)
                            .entity(Map.of("message",
                                    "No se puede eliminar un mantenimiento con repuestos registrados"))
                            .type(MediaType.APPLICATION_JSON)
                            .build());
        }
        mantenimiento.delete();
    }

    private void validarVehiculo(Integer idVehiculo, boolean exigirOperativo) {
        Vehiculo vehiculo = Vehiculo.findById(idVehiculo);
        if (vehiculo == null || !TenantAccess.company(request).equals(vehiculo.idEmpresa)) {
            throw new WebApplicationException("El vehículo no pertenece a tu empresa", 400);
        }
        if (exigirOperativo && !Objects.equals(vehiculo.idEstadoOperativo, 1)) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "Solo se puede registrar mantenimiento para vehículos operativos"))
                    .type(MediaType.APPLICATION_JSON)
                    .build());
        }
    }

    private void validarDatos(Mantenimiento mantenimiento, Integer idActual) {
        if (mantenimiento.fechaEntrada == null) {
            throw error(Response.Status.BAD_REQUEST, "La fecha de ingreso es obligatoria");
        }
        if (mantenimiento.fechaSalida != null && mantenimiento.fechaSalida.isBefore(mantenimiento.fechaEntrada)) {
            throw error(Response.Status.BAD_REQUEST, "La fecha de salida no puede ser anterior a la fecha de ingreso");
        }
        if (mantenimiento.kilometrajeEntrada != null && mantenimiento.kilometrajeEntrada < 0) {
            throw error(Response.Status.BAD_REQUEST, "El kilometraje no puede ser negativo");
        }
        if (mantenimiento.garantiaMeses != null && mantenimiento.garantiaMeses < 0) {
            throw error(Response.Status.BAD_REQUEST, "La garantía no puede ser negativa");
        }

        // ↓↓↓ NUEVO: validación de naturaleza del mantenimiento ↓↓↓
        if (mantenimiento.naturalezaMantenimiento != null
                && mantenimiento.naturalezaMantenimiento.equals("Correctivo")
                && mantenimiento.periodicidadKm != null) {
            throw error(Response.Status.BAD_REQUEST,
                    "La periodicidad en km solo aplica para mantenimientos preventivos");
        }
        if (mantenimiento.naturalezaMantenimiento != null
                && mantenimiento.naturalezaMantenimiento.equals("Preventivo")
                && mantenimiento.periodicidadKm == null) {
            throw error(Response.Status.BAD_REQUEST,
                    "Debe indicar la periodicidad en km para un mantenimiento preventivo");
        }
        // ↑↑↑ NUEVO ↑↑↑

        double total = mantenimiento.costoReparacion == null ? 0 : mantenimiento.costoReparacion;
        if (total < 0) {
            throw error(Response.Status.BAD_REQUEST, "El costo de reparación no puede ser negativo");
        }
        // ... resto del método sin cambios
    }

    private WebApplicationException conflicto(String mensaje) {
        return error(Response.Status.CONFLICT, mensaje);
    }

    private WebApplicationException error(Response.Status estado, String mensaje) {
        return new WebApplicationException(Response.status(estado)
                .entity(Map.of("message", mensaje))
                .type(MediaType.APPLICATION_JSON)
                .build());
    }

}
