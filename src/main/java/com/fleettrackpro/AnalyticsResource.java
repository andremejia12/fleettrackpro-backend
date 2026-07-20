package com.fleettrackpro;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.*;
import jakarta.ws.rs.QueryParam;

@Path("/analytics")
public class AnalyticsResource {

    @GET
    @Path("/margen-por-viaje")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Map<String, Object>> margenPorViaje(@QueryParam("idEmpresa") String idEmpresa) {
        boolean sinFiltro = idEmpresa == null || idEmpresa.isBlank();
        List<Viaje> viajes = sinFiltro ? Viaje.listAll() : Viaje.list("idEmpresa", idEmpresa);
        List<Map<String, Object>> resultado = new ArrayList<>();

        for (Viaje v : viajes) {
            // El margen solo es definitivo cuando el viaje terminó. Los viajes en curso
            // todavía pueden recibir ingresos y gastos y no deben distorsionar el reporte.
            if (v.idViajeEstado == null || v.idViajeEstado != 4 || v.fechaLlegada == null) {
                continue;
            }
            double ingresoTotal = IngresoServicio.<IngresoServicio>find("idViaje", v.idViaje)
                    .stream().mapToDouble(i -> i.montoCobrado != null ? i.montoCobrado.doubleValue() : 0).sum();

            Map<Integer, Gasto> gastosDelViaje = new LinkedHashMap<>();
            Gasto.<Gasto>find("idViaje", v.idViaje).stream()
                    .forEach(g -> gastosDelViaje.put(g.idGasto, g));

            double gastoTotal = gastosDelViaje.values().stream()
                    .mapToDouble(g -> g.monto != null ? g.monto.doubleValue() : 0).sum();

            Vehiculo veh = Vehiculo.findById(v.idVehiculo);
            Conductor conductor = Conductor.findById(v.idConductor);

            List<Map<String, Object>> detalleGastos = gastosDelViaje.values().stream().map(g -> {
                CostoGastoCategoria categoria = CostoGastoCategoria.findById(g.idGastoCategoria);
                Map<String, Object> detalle = new HashMap<>();
                detalle.put("idGasto", g.idGasto);
                detalle.put("categoria", categoria != null ? categoria.nombreCategoria : "Gasto");
                detalle.put("descripcion", g.descripcion);
                detalle.put("monto", g.monto);
                return detalle;
            }).toList();

            Map<String, Object> item = new HashMap<>();
            item.put("idViaje", v.idViaje);
            item.put("placa", veh != null ? veh.placa : "");
            item.put("conductor", conductor != null ? conductor.nombre + " " + conductor.apellido : "");
            item.put("origen", v.origen);
            item.put("destino", v.destino);
            item.put("fechaSalida", v.fechaSalida);
            item.put("ingresoTotal", ingresoTotal);
            item.put("gastoTotal", gastoTotal);
            item.put("detalleGastos", detalleGastos);
            item.put("margenNeto", ingresoTotal - gastoTotal);
            resultado.add(item);
        }
        return resultado;
    }

    @GET
    @Path("/cpk-por-vehiculo")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Map<String, Object>> cpkPorVehiculo(@QueryParam("idEmpresa") String idEmpresa) {
        boolean sinFiltro = idEmpresa == null || idEmpresa.isBlank();
        List<Vehiculo> vehiculos = sinFiltro ? Vehiculo.listAll() : Vehiculo.list("idEmpresa", idEmpresa);
        List<Map<String, Object>> resultado = new ArrayList<>();

        for (Vehiculo veh : vehiculos) {
            List<Viaje> viajesDelVehiculo = Viaje.list("idVehiculo", veh.idVehiculo);

            List<Gasto> gastosVehiculo = Gasto.list("idVehiculo", veh.idVehiculo);
            double gastoTotal = gastosVehiculo.stream()
                    .filter(g -> !esCostoMantenimiento(g))
                    .mapToDouble(g -> g.monto != null ? g.monto.doubleValue() : 0).sum();

            List<Mantenimiento> mantenimientos = Mantenimiento.list("idVehiculo", veh.idVehiculo);
            double costoMantenimiento = mantenimientos.stream()
                    .mapToDouble(m -> m.costoReparacion != null ? m.costoReparacion : 0).sum();

            double costoRepuestos = 0;
            for (Mantenimiento m : mantenimientos) {
                costoRepuestos += RepuestoMantenimientoDetalle
                        .<RepuestoMantenimientoDetalle>find("mantenimiento.idMantenimiento", m.idMantenimiento)
                        .stream().mapToDouble(r -> r.costoTotalRepuesto != null ? r.costoTotalRepuesto : 0).sum();
            }

            double costoTotalAcumulado = gastoTotal + costoMantenimiento + costoRepuestos;

            double kilometrosRecorridos = viajesDelVehiculo.stream()
                    .filter(v -> v.idViajeEstado != null && v.idViajeEstado == 4 && v.fechaLlegada != null)
                    .filter(v -> v.kilometrajeSalida != null && v.kilometrajeLlegada != null)
                    .mapToDouble(v -> v.kilometrajeLlegada - v.kilometrajeSalida)
                    .sum();

            Map<String, Object> item = new HashMap<>();
            item.put("idVehiculo", veh.idVehiculo);
            item.put("placa", veh.placa);
            item.put("costoTotalAcumulado", costoTotalAcumulado);
            item.put("kilometrosRecorridos", kilometrosRecorridos);
            item.put("cpk", kilometrosRecorridos > 0 ? costoTotalAcumulado / kilometrosRecorridos : null);
            resultado.add(item);
        }
        return resultado;
    }

    private boolean esCostoMantenimiento(Gasto gasto) {
        CostoGastoCategoria categoria = CostoGastoCategoria.findById(gasto.idGastoCategoria);
        if (categoria == null || categoria.nombreCategoria == null) return false;
        String nombre = categoria.nombreCategoria.toLowerCase(Locale.ROOT);
        return nombre.contains("mantenimiento") || nombre.contains("mano de obra") || nombre.contains("repuesto");
    }
}
