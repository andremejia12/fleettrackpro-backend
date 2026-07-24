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
    public static class VehiculoDisponibleDTO {
        public Integer idVehiculo;
        public String placa;
        public String marca;
        public String modelo;
        public String idSucursalBase;

        public static VehiculoDisponibleDTO from(Vehiculo vehiculo) {
            VehiculoMarca marca = vehiculo.idMarca == null ? null : VehiculoMarca.findById(vehiculo.idMarca);
            VehiculoModelo modelo = vehiculo.idModelo == null ? null : VehiculoModelo.findById(vehiculo.idModelo);
            VehiculoDisponibleDTO dto = new VehiculoDisponibleDTO();
            dto.idVehiculo = vehiculo.idVehiculo;
            dto.placa = vehiculo.placa;
            dto.marca = marca == null ? "" : marca.nombreMarca;
            dto.modelo = modelo == null ? "" : modelo.nombreModelo;
            dto.idSucursalBase = vehiculo.idSucursalBase;
            return dto;
        }
    }

    @Context
    ContainerRequestContext request;

    @GET
    public List<?> listar(@QueryParam("idEmpresa") String idEmpresa) {
        if (RoleAccess.isConductor(request)) {
            List<Viaje> viajes = Viaje.list("idEmpresa = ?1 and idConductor = ?2",
                    TenantAccess.company(request), RoleAccess.conductorIdFor(request));
            return viajes.stream()
                    .map(this::conDatosVehiculo)
                    .toList();
        }
        return Viaje.list("idEmpresa", TenantAccess.company(request));
    }

    @GET
    @Path("/vehiculos-disponibles")
    public List<VehiculoDisponibleDTO> vehiculosDisponibles() {
        if (!RoleAccess.isConductor(request)) {
            throw forbidden("Este recurso requiere el rol conductor");
        }
        String empresa = TenantAccess.company(request);
        OperacionViajeEstado enGarita = requireEstado("En Garita");
        OperacionViajeEstado enRuta = requireEstado("En Ruta");
        List<Vehiculo> operativos = Vehiculo.list("idEmpresa = ?1 and idEstadoOperativo = ?2", empresa, 1);
        return operativos.stream()
                .filter(v -> Viaje.count(
                        "idVehiculo = ?1 and (idViajeEstado = ?2 or idViajeEstado = ?3)",
                        v.idVehiculo, enGarita.idViajeEstado, enRuta.idViajeEstado) == 0)
                .map(VehiculoDisponibleDTO::from)
                .toList();
    }

    @POST
    @Transactional
    public Viaje crear(Viaje nuevo) {
        if (RoleAccess.isConductor(request)) {
            String empresa = TenantAccess.company(request);
            nuevo.idEmpresa = empresa;
            nuevo.idConductor = RoleAccess.conductorIdFor(request);
            nuevo.idOrdenTrabajo = null;
            nuevo.ordenTrabajoNro = null;
            nuevo.idViajeEstado = requireEstado("Programado").idViajeEstado;

            Vehiculo vehiculo = nuevo.idVehiculo == null ? null : Vehiculo.findById(nuevo.idVehiculo);
            if (vehiculo == null || !empresa.equals(vehiculo.idEmpresa)
                    || !Integer.valueOf(1).equals(vehiculo.idEstadoOperativo)) {
                throw error(Response.Status.BAD_REQUEST, "El vehículo seleccionado no está disponible");
            }
            OperacionViajeEstado enGarita = requireEstado("En Garita");
            OperacionViajeEstado enRuta = requireEstado("En Ruta");
            if (Viaje.count("idVehiculo = ?1 and (idViajeEstado = ?2 or idViajeEstado = ?3)",
                    nuevo.idVehiculo, enGarita.idViajeEstado, enRuta.idViajeEstado) > 0) {
                throw conflicto("Este vehículo ya tiene un viaje en curso");
            }
            validarReferencias(nuevo);
            Viaje.persist(nuevo);
            return nuevo;
        }
        RoleAccess.requireRole(request, "admin", "despachador");
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
        RoleAccess.requireNotConductor(request);
        RoleAccess.requireRole(request, "admin", "despachador");
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
    public Object obtener(@PathParam("id") Integer id) {
        Viaje viaje = Viaje.findById(id);
        if (viaje == null) {
            throw new NotFoundException("Viaje no encontrado");
        }
        TenantAccess.require(request, viaje.idEmpresa);
        requireViajeDelConductor(viaje);
        if (RoleAccess.isConductor(request)) {
            return conDatosVehiculo(viaje);
        }
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
        if (RoleAccess.isConductor(request)) {
            requireViajeDelConductor(viaje);
            return actualizarComoConductor(viaje, actualizado);
        }
        RoleAccess.requireRole(request, "admin", "despachador");
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
                    || !Objects.equals(viaje.idSucursalOrigen, actualizado.idSucursalOrigen)
                    || !Objects.equals(viaje.origen, actualizado.origen)
                    || !Objects.equals(viaje.destino, actualizado.destino)
                    || !Objects.equals(viaje.fechaSalida, actualizado.fechaSalida)
                    || !Objects.equals(viaje.kilometrajeSalida, actualizado.kilometrajeSalida))) {
            throw conflicto("En ruta solo se puede editar la llegada estimada y el volumen");
        }
        viaje.idVehiculo = actualizado.idVehiculo;
        viaje.idConductor = actualizado.idConductor;
        viaje.idOrdenTrabajo = actualizado.idOrdenTrabajo;
        viaje.idSucursalOrigen = actualizado.idSucursalOrigen;
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
        if (viaje.idSucursalOrigen != null && viaje.idSucursalOrigen.isBlank()) {
            viaje.idSucursalOrigen = null;
        }
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
        if (viaje.idSucursalOrigen != null && !viaje.idSucursalOrigen.isBlank()) {
            SucursalGarita sucursal = SucursalGarita.findById(viaje.idSucursalOrigen);
            if (sucursal == null || !empresa.equals(sucursal.idEmpresa)) {
                throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("message", "La sede de origen no pertenece a tu empresa"))
                        .type(MediaType.APPLICATION_JSON)
                        .build());
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
        if (!RoleAccess.isConductor(request)) {
            RoleAccess.requireRole(request, "admin", "despachador");
        }
        Viaje viaje = Viaje.findById(id);
        if (viaje == null) throw new NotFoundException("Viaje no encontrado");
        TenantAccess.require(request, viaje.idEmpresa);
        requireViajeDelConductor(viaje);
        if (cambio == null || cambio.idEstado == null) {
            throw new WebApplicationException("Selecciona un estado", 400);
        }
        Integer siguiente = cambio.idEstado;
        if (!esTransicionValida(viaje.idViajeEstado, siguiente)) {
            throw new WebApplicationException("La transición de estado no está permitida", 400);
        }
        Integer cancelado = requireEstado("Cancelado").idViajeEstado;
        Integer completado = requireEstado("Completado").idViajeEstado;
        if (siguiente.equals(cancelado)
                && (Gasto.count("idViaje", id) > 0 || IngresoServicio.count("idViaje", id) > 0)) {
            throw conflicto("No se puede cancelar un viaje con gastos o ingresos asociados");
        }
        if (siguiente.equals(completado)) {
            validarChecklistObligatorio(id);
            validarDatosFinalizacion(cambio.fechaLlegada, cambio.kilometrajeLlegada, viaje);
            viaje.fechaLlegada = cambio.fechaLlegada;
            viaje.kilometrajeLlegada = cambio.kilometrajeLlegada;
        }
        viaje.idViajeEstado = siguiente;
        return viaje;
    }

    private Viaje actualizarComoConductor(Viaje viaje, Viaje actualizado) {
        if (actualizado == null) {
            throw new WebApplicationException("Los datos del viaje son obligatorios", 400);
        }
        if (!Objects.equals(viaje.idVehiculo, actualizado.idVehiculo)
                || !Objects.equals(viaje.idConductor, actualizado.idConductor)
                || !Objects.equals(viaje.idOrdenTrabajo, actualizado.idOrdenTrabajo)
                || !Objects.equals(viaje.idSucursalOrigen, actualizado.idSucursalOrigen)
                || !Objects.equals(viaje.origen, actualizado.origen)
                || !Objects.equals(viaje.destino, actualizado.destino)
                || !Objects.equals(viaje.fechaLlegadaEstimada, actualizado.fechaLlegadaEstimada)
                || !Objects.equals(viaje.ordenTrabajoNro, actualizado.ordenTrabajoNro)
                || !Objects.equals(viaje.volumenAtendidoM3, actualizado.volumenAtendidoM3)) {
            throw forbidden("El conductor solo puede modificar fechas, kilometrajes y estado de su viaje");
        }
        Integer completado = requireEstado("Completado").idViajeEstado;
        Integer cancelado = requireEstado("Cancelado").idViajeEstado;
        if (viaje.idViajeEstado != null
                && (viaje.idViajeEstado.equals(completado) || viaje.idViajeEstado.equals(cancelado))) {
            throw conflicto("No se puede editar un viaje finalizado");
        }
        validarTransicionEstado(viaje.idViajeEstado, actualizado.idViajeEstado);
        if (actualizado.idViajeEstado != null && actualizado.idViajeEstado.equals(cancelado)
                && (Gasto.count("idViaje", viaje.idViaje) > 0
                    || IngresoServicio.count("idViaje", viaje.idViaje) > 0)) {
            throw conflicto("No se puede cancelar un viaje con gastos o ingresos asociados");
        }
        if (actualizado.idViajeEstado != null && actualizado.idViajeEstado.equals(completado)) {
            validarChecklistObligatorio(viaje.idViaje);
            validarDatosFinalizacion(actualizado.fechaLlegada, actualizado.kilometrajeLlegada, actualizado);
        }
        viaje.fechaSalida = actualizado.fechaSalida;
        viaje.fechaLlegada = actualizado.fechaLlegada;
        viaje.kilometrajeSalida = actualizado.kilometrajeSalida;
        viaje.kilometrajeLlegada = actualizado.kilometrajeLlegada;
        viaje.idViajeEstado = actualizado.idViajeEstado;
        return viaje;
    }

    private void validarTransicionEstado(Integer estadoActual, Integer estadoNuevo) {
        if (estadoNuevo == null) {
            throw new WebApplicationException("Selecciona un estado", 400);
        }
        if (Objects.equals(estadoActual, estadoNuevo)) {
            return;
        }
        if (!esTransicionValida(estadoActual, estadoNuevo)) {
            throw new WebApplicationException("La transición de estado no está permitida", 400);
        }
    }

    private boolean esTransicionValida(Integer actual, Integer siguiente) {
        if (actual == null || siguiente == null) {
            return false;
        }
        Integer programado = requireEstado("Programado").idViajeEstado;
        Integer enGarita = requireEstado("En Garita").idViajeEstado;
        Integer enRuta = requireEstado("En Ruta").idViajeEstado;
        Integer completado = requireEstado("Completado").idViajeEstado;
        Integer cancelado = requireEstado("Cancelado").idViajeEstado;
        return (actual.equals(programado) && siguiente.equals(enGarita))
                || (actual.equals(enGarita) && siguiente.equals(enRuta))
                || (actual.equals(enRuta) && siguiente.equals(completado))
                || ((actual.equals(programado) || actual.equals(enGarita) || actual.equals(enRuta))
                    && siguiente.equals(cancelado));
    }

    private void requireViajeDelConductor(Viaje viaje) {
        if (RoleAccess.isConductor(request)
                && !RoleAccess.conductorIdFor(request).equals(viaje.idConductor)) {
            throw forbidden("El viaje no pertenece al conductor autenticado");
        }
    }

    private ViajeConVehiculoDTO conDatosVehiculo(Viaje viaje) {
        Vehiculo vehiculo = viaje.idVehiculo == null ? null : Vehiculo.findById(viaje.idVehiculo);
        VehiculoMarca marca = vehiculo == null || vehiculo.idMarca == null
                ? null
                : VehiculoMarca.findById(vehiculo.idMarca);
        VehiculoModelo modelo = vehiculo == null || vehiculo.idModelo == null
                ? null
                : VehiculoModelo.findById(vehiculo.idModelo);
        SucursalGarita sucursal = viaje.idSucursalOrigen == null
                ? null
                : SucursalGarita.findById(viaje.idSucursalOrigen);
        ViajeConVehiculoDTO dto = ViajeConVehiculoDTO.from(
                viaje,
                vehiculo == null ? null : vehiculo.placa,
                marca == null ? null : marca.nombreMarca,
                modelo == null ? null : modelo.nombreModelo);
        dto.nombreSucursalOrigen = sucursal == null ? null : sucursal.nombreSucursal;
        OperacionViajeEstado estadoActual = viaje.idViajeEstado == null
                ? null
                : OperacionViajeEstado.findById(viaje.idViajeEstado);
        dto.estadoViaje = estadoActual == null ? null : estadoActual.nombreEstado;
        OperacionViajeEstado siguiente = siguienteEstado(viaje.idViajeEstado);
        dto.idEstadoSiguiente = siguiente == null ? null : siguiente.idViajeEstado;
        dto.nombreEstadoSiguiente = siguiente == null ? null : siguiente.nombreEstado;
        if (siguiente != null) {
            dto.idEstadoCancelado = requireEstado("Cancelado").idViajeEstado;
        }
        return dto;
    }

    private OperacionViajeEstado siguienteEstado(Integer estadoActual) {
        if (estadoActual == null) return null;
        if (estadoActual.equals(requireEstado("Programado").idViajeEstado)) {
            return requireEstado("En Garita");
        }
        if (estadoActual.equals(requireEstado("En Garita").idViajeEstado)) {
            return requireEstado("En Ruta");
        }
        if (estadoActual.equals(requireEstado("En Ruta").idViajeEstado)) {
            return requireEstado("Completado");
        }
        return null;
    }

    private WebApplicationException forbidden(String mensaje) {
        return new WebApplicationException(Response.status(Response.Status.FORBIDDEN)
                .entity(Map.of("message", mensaje))
                .type(MediaType.APPLICATION_JSON)
                .build());
    }

    private OperacionViajeEstado requireEstado(String nombre) {
        OperacionViajeEstado estado = OperacionViajeEstado
                .find("lower(nombreEstado) = ?1", nombre.toLowerCase())
                .firstResult();
        if (estado == null) {
            throw error(Response.Status.INTERNAL_SERVER_ERROR,
                    "No se encontró el estado de viaje " + nombre);
        }
        return estado;
    }

    private WebApplicationException error(Response.Status estado, String mensaje) {
        return new WebApplicationException(Response.status(estado)
                .entity(Map.of("message", mensaje))
                .type(MediaType.APPLICATION_JSON)
                .build());
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
