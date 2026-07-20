package com.fleettrackpro;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.*;
import java.util.stream.Collectors;
import jakarta.ws.rs.QueryParam;

@Path("/dashboard")
public class DashboardResource {

    @GET
    @Path("/summary")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> summary(@QueryParam("idEmpresa") String idEmpresa) {
        boolean sinFiltro = idEmpresa == null || idEmpresa.isBlank();
        CostoMoneda monedaPen = CostoMoneda.find("upper(codigoIso)", "PEN").firstResult();
        Integer idMonedaPen = monedaPen != null ? monedaPen.idMoneda : null;

        long totalVehicles = sinFiltro ? Vehiculo.count() : Vehiculo.count("idEmpresa", idEmpresa);
        long activeVehicles = sinFiltro
                ? Vehiculo.count("idEstadoOperativo = ?1", 1)
                : Vehiculo.count("idEstadoOperativo = ?1 and idEmpresa = ?2", 1, idEmpresa);
        long driversOnRoute = sinFiltro
                ? Conductor.count("idEstadoLaboral = ?1", 2)
                : Conductor.count("idEstadoLaboral = ?1 and idEmpresa = ?2", 2, idEmpresa);
        long pendingMaintenance = sinFiltro
                ? Mantenimiento.count("fechaSalida is null")
                : Mantenimiento.count("fechaSalida is null and idEmpresa = ?1", idEmpresa);

        int currentMonth = java.time.LocalDate.now().getMonthValue();
        int currentYear = java.time.LocalDate.now().getYear();

        List<Gasto> listGastos = sinFiltro
                ? Gasto.list(
                        "fechaGasto is not null and extract(month from fechaGasto) = ?1 and extract(year from fechaGasto) = ?2",
                        currentMonth, currentYear)
                : Gasto.list(
                        "fechaGasto is not null and extract(month from fechaGasto) = ?1 and extract(year from fechaGasto) = ?2 and idEmpresa = ?3",
                        currentMonth, currentYear, idEmpresa);
        double monthlyOperatingCost = listGastos.stream()
                .filter(g -> idMonedaPen != null && idMonedaPen.equals(g.idMoneda))
                .mapToDouble(g -> g.monto != null ? g.monto.doubleValue() : 0.0)
                .sum();

        List<Mantenimiento> currentMonthMaintenances = sinFiltro
                ? Mantenimiento.list(
                        "fechaEntrada is not null and extract(month from fechaEntrada) = ?1 and extract(year from fechaEntrada) = ?2",
                        currentMonth, currentYear)
                : Mantenimiento.list(
                        "fechaEntrada is not null and extract(month from fechaEntrada) = ?1 and extract(year from fechaEntrada) = ?2 and idEmpresa = ?3",
                        currentMonth, currentYear, idEmpresa);
        double monthlyMaintenanceCost = 0.0;
        for (Mantenimiento m : currentMonthMaintenances) {
            if (idMonedaPen == null || !idMonedaPen.equals(m.idMoneda)) continue;
            if (m.costoReparacion != null) {
                monthlyMaintenanceCost += m.costoReparacion;
            }
            if (m.repuestos != null) {
                for (RepuestoMantenimientoDetalle r : m.repuestos) {
                    if (r.costoTotalRepuesto != null) {
                        monthlyMaintenanceCost += r.costoTotalRepuesto;
                    }
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalVehicles", totalVehicles);
        result.put("activeVehicles", activeVehicles);
        result.put("driversOnRoute", driversOnRoute);
        result.put("pendingMaintenance", pendingMaintenance);
        result.put("monthlyOperatingCost", monthlyOperatingCost);
        result.put("monthlyMaintenanceCost", monthlyMaintenanceCost);
        result.put("currency", "PEN");
        return result;
    }

    @GET
    @Path("/balance")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> balance(
            @QueryParam("idEmpresa") String idEmpresa,
            @QueryParam("idMetodoPago") Integer idMetodoPago,
            @QueryParam("moneda") String monedaSolicitada) {
        boolean sinFiltro = idEmpresa == null || idEmpresa.isBlank();
        String moneda = monedaSolicitada == null || monedaSolicitada.isBlank()
                ? "PEN" : monedaSolicitada.toUpperCase(Locale.ROOT);
        CostoMoneda monedaEntidad = CostoMoneda.find("upper(codigoIso)", moneda).firstResult();
        if (monedaEntidad == null || (!"PEN".equals(moneda) && !"USD".equals(moneda))) {
            throw new jakarta.ws.rs.BadRequestException("Moneda de balance no válida");
        }
        Integer idMoneda = monedaEntidad.idMoneda;
        int currentMonth = java.time.LocalDate.now().getMonthValue();
        int currentYear = java.time.LocalDate.now().getYear();

        List<IngresoServicio> listIngresos = sinFiltro
                ? IngresoServicio.list(
                        "fechaPago is not null and extract(month from fechaPago) = ?1 and extract(year from fechaPago) = ?2",
                        currentMonth, currentYear)
                : IngresoServicio.list(
                        "fechaPago is not null and extract(month from fechaPago) = ?1 and extract(year from fechaPago) = ?2 and idEmpresa = ?3",
                        currentMonth, currentYear, idEmpresa);
        double totalIngresos = listIngresos.stream()
                .filter(i -> idMetodoPago == null || idMetodoPago.equals(i.idMetodoPago))
                .filter(i -> idMoneda.equals(i.idMoneda))
                .mapToDouble(i -> i.montoCobrado != null ? i.montoCobrado : 0.0)
                .sum();

        List<Gasto> listGastos = sinFiltro
                ? Gasto.list(
                        "fechaGasto is not null and extract(month from fechaGasto) = ?1 and extract(year from fechaGasto) = ?2",
                        currentMonth, currentYear)
                : Gasto.list(
                        "fechaGasto is not null and extract(month from fechaGasto) = ?1 and extract(year from fechaGasto) = ?2 and idEmpresa = ?3",
                        currentMonth, currentYear, idEmpresa);
        double totalGastos = listGastos.stream()
                .filter(g -> idMetodoPago == null || idMetodoPago.equals(g.idMetodoPago))
                .filter(g -> idMoneda.equals(g.idMoneda))
                .mapToDouble(g -> g.monto != null ? g.monto.doubleValue() : 0.0)
                .sum();

        List<Mantenimiento> listMantenimientos = sinFiltro
                ? Mantenimiento.list(
                        "fechaEntrada is not null and extract(month from fechaEntrada) = ?1 and extract(year from fechaEntrada) = ?2",
                        currentMonth, currentYear)
                : Mantenimiento.list(
                        "fechaEntrada is not null and extract(month from fechaEntrada) = ?1 and extract(year from fechaEntrada) = ?2 and idEmpresa = ?3",
                        currentMonth, currentYear, idEmpresa);
        double totalMantenimientos = 0.0;
        for (Mantenimiento m : listMantenimientos) {
            if (idMetodoPago != null && !idMetodoPago.equals(m.idMetodoPago)) continue;
            if (!idMoneda.equals(m.idMoneda)) continue;
            if (m.costoReparacion != null) {
                totalMantenimientos += m.costoReparacion;
            }
            if (m.repuestos != null) {
                for (RepuestoMantenimientoDetalle r : m.repuestos) {
                    if (r.costoTotalRepuesto != null) {
                        totalMantenimientos += r.costoTotalRepuesto;
                    }
                }
            }
        }

        List<FacturaSaas> listSuscripciones = sinFiltro
                ? FacturaSaas.list(
                        "fechaPago is not null and extract(month from fechaPago) = ?1 and extract(year from fechaPago) = ?2",
                        currentMonth, currentYear)
                : FacturaSaas.list(
                        "fechaPago is not null and extract(month from fechaPago) = ?1 and extract(year from fechaPago) = ?2 and idEmpresa = ?3",
                        currentMonth, currentYear, idEmpresa);
        double totalSuscripciones = listSuscripciones.stream()
                .filter(f -> idMetodoPago == null || idMetodoPago.equals(f.idMetodoPago))
                .filter(f -> moneda.equalsIgnoreCase(f.moneda))
                .mapToDouble(f -> f.monto != null ? f.monto.doubleValue() : 0.0)
                .sum();

        double totalEgresos = totalGastos + totalMantenimientos + totalSuscripciones;
        double balanceVal = totalIngresos - totalEgresos;

        Map<String, Object> result = new HashMap<>();
        result.put("totalIngresos", totalIngresos);
        result.put("totalEgresos", totalEgresos);
        result.put("totalGastos", totalGastos);
        result.put("totalMantenimientos", totalMantenimientos);
        result.put("totalSuscripciones", totalSuscripciones);
        result.put("balance", balanceVal);
        result.put("currency", moneda);
        return result;
    }

    @GET
    @Path("/status-breakdown")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Map<String, Object>> statusBreakdown(@QueryParam("idEmpresa") String idEmpresa) {
        boolean sinFiltro = idEmpresa == null || idEmpresa.isBlank();
        Map<Integer, String> labels = Map.of(1, "En ruta", 2, "Disponible", 3, "Mantenimiento", 4, "Inactivo");
        Map<Integer, String> colors = Map.of(1, "var(--accent-teal)", 2, "var(--accent-blue)", 3, "var(--accent-amber)",
                4, "#8b94a7");

        List<Map<String, Object>> result = new ArrayList<>();
        for (Integer estadoId : List.of(1, 2, 3, 4)) {
            long count = sinFiltro
                    ? Vehiculo.count("idEstadoOperativo = ?1", estadoId)
                    : Vehiculo.count("idEstadoOperativo = ?1 and idEmpresa = ?2", estadoId, idEmpresa);
            Map<String, Object> item = new HashMap<>();
            item.put("status", estadoId == 1 ? "en_ruta"
                    : estadoId == 2 ? "disponible" : estadoId == 3 ? "mantenimiento" : "inactivo");
            item.put("label", labels.get(estadoId));
            item.put("count", count);
            item.put("color", colors.get(estadoId));
            result.add(item);
        }
        return result;
    }

    @GET
    @Path("/maintenance-alerts")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Map<String, Object>> maintenanceAlerts(@QueryParam("idEmpresa") String idEmpresa) {
        boolean sinFiltro = idEmpresa == null || idEmpresa.isBlank();
        List<Mantenimiento> enTaller = sinFiltro
                ? Mantenimiento.list("fechaSalida is null")
                : Mantenimiento.list("fechaSalida is null and idEmpresa = ?1", idEmpresa);
        return enTaller.stream().map(m -> {
            Vehiculo v = Vehiculo.findById(m.idVehiculo);
            Map<String, Object> item = new HashMap<>();
            item.put("id", m.idMantenimiento);
            item.put("vehicle", v != null ? v.placa : "");
            item.put("plate", v != null ? v.placa : "");
            item.put("task", m.descripcionFalla);
            item.put("urgency", "alta");
            item.put("dueInDays", 0);
            return item;
        }).collect(Collectors.toList());
    }

    @GET
    @Path("/recent-operations")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Map<String, Object>> recentOperations(@QueryParam("idEmpresa") String idEmpresa) {
        boolean sinFiltro = idEmpresa == null || idEmpresa.isBlank();
        List<Viaje> viajes = sinFiltro ? Viaje.listAll() : Viaje.list("idEmpresa", idEmpresa);
        return viajes.stream().map(v -> {
            Vehiculo veh = Vehiculo.findById(v.idVehiculo);
            Conductor c = Conductor.findById(v.idConductor);
            Map<String, Object> item = new HashMap<>();
            item.put("id", v.idViaje);
            item.put("plate", veh != null ? veh.placa : "");
            item.put("driver", c != null ? c.nombre + " " + c.apellido : "");
            item.put("route", v.origen + " - " + v.destino);
            String status = estadoViaje(v.idViajeEstado);
            item.put("status", status);
            item.put("eta", "completado".equals(status) ? "Llegó"
                    : "cancelado".equals(status) ? "Cancelado"
                    : "programado".equals(status) ? "Programado" : "En curso");
            return item;
        }).collect(Collectors.toList());
    }

    @GET
    @Path("/monthly-costs")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Map<String, Object>> monthlyCosts(@QueryParam("idEmpresa") String idEmpresa) {
        boolean sinFiltro = idEmpresa == null || idEmpresa.isBlank();
        CostoMoneda monedaPen = CostoMoneda.find("upper(codigoIso)", "PEN").firstResult();
        Integer idMonedaPen = monedaPen != null ? monedaPen.idMoneda : null;
        List<Map<String, Object>> result = new ArrayList<>();
        YearMonth actual = YearMonth.now();
        for (int offset = 5; offset >= 0; offset--) {
            YearMonth periodo = actual.minusMonths(offset);
            int mesNum = periodo.getMonthValue();
            int anio = periodo.getYear();

            double combustible = (sinFiltro
                    ? Gasto.<Gasto>find("idGastoCategoria = ?1 and extract(month from fechaGasto) = ?2 and extract(year from fechaGasto) = ?3", 1, mesNum, anio)
                    : Gasto.<Gasto>find(
                            "idGastoCategoria = ?1 and extract(month from fechaGasto) = ?2 and extract(year from fechaGasto) = ?3 and idEmpresa = ?4", 1,
                            mesNum, anio, idEmpresa))
                    .stream().filter(g -> idMonedaPen != null && idMonedaPen.equals(g.idMoneda))
                    .mapToDouble(g -> g.monto != null ? g.monto.doubleValue() : 0).sum();

            double peajes = (sinFiltro
                    ? Gasto.<Gasto>find("idGastoCategoria = ?1 and extract(month from fechaGasto) = ?2 and extract(year from fechaGasto) = ?3", 2, mesNum, anio)
                    : Gasto.<Gasto>find(
                            "idGastoCategoria = ?1 and extract(month from fechaGasto) = ?2 and extract(year from fechaGasto) = ?3 and idEmpresa = ?4", 2,
                            mesNum, anio, idEmpresa))
                    .stream().filter(g -> idMonedaPen != null && idMonedaPen.equals(g.idMoneda))
                    .mapToDouble(g -> g.monto != null ? g.monto.doubleValue() : 0).sum();

            double mantenimiento = (sinFiltro
                    ? Mantenimiento.<Mantenimiento>find("extract(month from fechaEntrada) = ?1 and extract(year from fechaEntrada) = ?2", mesNum, anio)
                    : Mantenimiento.<Mantenimiento>find("extract(month from fechaEntrada) = ?1 and extract(year from fechaEntrada) = ?2 and idEmpresa = ?3",
                            mesNum, anio, idEmpresa))
                    .stream().filter(m -> idMonedaPen != null && idMonedaPen.equals(m.idMoneda))
                    .mapToDouble(m -> m.costoReparacion != null ? m.costoReparacion : 0).sum();

            Map<String, Object> item = new HashMap<>();
            String mes = periodo.getMonth().getDisplayName(TextStyle.SHORT, new Locale("es", "PE"));
            item.put("month", Character.toUpperCase(mes.charAt(0)) + mes.substring(1).replace(".", ""));
            item.put("combustible", combustible);
            item.put("peajes", peajes);
            item.put("mantenimiento", mantenimiento);
            result.add(item);
        }
        return result;
    }

    private String estadoViaje(Integer idEstado) {
        if (idEstado == null) return "desconocido";
        return switch (idEstado) {
            case 1 -> "programado";
            case 2, 3 -> "en_ruta";
            case 4 -> "completado";
            case 5 -> "cancelado";
            default -> "desconocido";
        };
    }

    @GET
    @Path("/license-alerts")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Map<String, Object>> licenseAlerts(@QueryParam("idEmpresa") String idEmpresa) {
        boolean sinFiltro = idEmpresa == null || idEmpresa.isBlank();
        LocalDate hoy = LocalDate.now();
        LocalDate limite = hoy.plusDays(30);

        List<Conductor> porVencer = sinFiltro
                ? Conductor.list("licenciaVencimiento is not null and licenciaVencimiento <= ?1", limite)
                : Conductor.list("licenciaVencimiento is not null and licenciaVencimiento <= ?1 and idEmpresa = ?2",
                        limite, idEmpresa);

        return porVencer.stream().map(c -> {
            long diasRestantes = java.time.temporal.ChronoUnit.DAYS.between(hoy, c.licenciaVencimiento);
            Map<String, Object> item = new HashMap<>();
            item.put("id", c.idConductor);
            item.put("conductor", c.nombre + " " + c.apellido);
            item.put("documento", c.numeroDocumento);
            item.put("dueInDays", Math.max(diasRestantes, 0));
            item.put("vencida", diasRestantes < 0);
            return item;
        }).collect(Collectors.toList());
    }
}
