package com.fleettrackpro;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.*;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.Duration;

@Path("/analytics")
public class AnalyticsResource {
    @Context
    ContainerRequestContext request;

    @GET
    @Path("/margen-por-viaje")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Map<String, Object>> margenPorViaje(@QueryParam("idEmpresa") String idEmpresa) {
        String empresa = TenantAccess.company(request);
        List<Viaje> viajes = Viaje.list("idEmpresa", empresa);
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
        String empresa = TenantAccess.company(request);
        List<Vehiculo> vehiculos = Vehiculo.list("idEmpresa", empresa);
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

    @GET
    @Path("/ckv-mensual")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> ckvMensual(
            @QueryParam("idEmpresa") String idEmpresa,
            @QueryParam("mes") Integer mes,
            @QueryParam("anio") Integer anio) {
        YearMonth actual = YearMonth.now();
        int mesConsultado = mes == null ? actual.getMonthValue() : mes;
        int anioConsultado = anio == null ? actual.getYear() : anio;
        if (mesConsultado < 1 || mesConsultado > 12 || anioConsultado < 1) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "El mes o año consultado no es válido"))
                    .type(MediaType.APPLICATION_JSON)
                    .build());
        }

        String empresa = TenantAccess.company(request);
        LocalDateTime inicio = LocalDate.of(anioConsultado, mesConsultado, 1).atStartOfDay();
        LocalDateTime fin = inicio.plusMonths(1);
        List<Vehiculo> vehiculos = Vehiculo.list(
                "idEmpresa = ?1 and idEstadoOperativo = ?2", empresa, 1);
        List<Gasto> gastosMes = Gasto.<Gasto>list("idEmpresa", empresa).stream()
                .filter(g -> enPeriodo(g.fechaGasto, inicio, fin))
                .toList();

        Map<Integer, CostoGastoCategoria> categorias = new HashMap<>();
        for (Gasto gasto : gastosMes) {
            if (gasto.idGastoCategoria != null
                    && !categorias.containsKey(gasto.idGastoCategoria)) {
                CostoGastoCategoria categoria = CostoGastoCategoria.findById(gasto.idGastoCategoria);
                categorias.put(gasto.idGastoCategoria, categoria);
            }
        }

        double cfpTotal = gastosMes.stream()
                .filter(g -> tieneTipo(categorias.get(g.idGastoCategoria), "CFP"))
                .mapToDouble(this::monto)
                .sum();
        double cfpProrrateado = vehiculos.isEmpty() ? 0 : cfpTotal / vehiculos.size();
        OperacionViajeEstado completado = OperacionViajeEstado
                .find("lower(nombreEstado) = ?1", "completado")
                .firstResult();

        List<Map<String, Object>> detalle = new ArrayList<>();
        double sumaCkv = 0;
        int cantidadCkv = 0;

        for (Vehiculo vehiculo : vehiculos) {
            boolean datosIncompletos = vehiculo.valorCompra == null;
            double valorCompra = vehiculo.valorCompra == null ? 0 : vehiculo.valorCompra;
            double valorResidual = vehiculo.valorResidual == null ? 0 : vehiculo.valorResidual;
            int vidaUtil = vehiculo.vidaUtilAnios == null || vehiculo.vidaUtilAnios <= 0
                    ? 5 : vehiculo.vidaUtilAnios;
            double depreciacionMensual = ((valorCompra - valorResidual) / vidaUtil) / 12.0;

            double gastosCfv = gastosMes.stream()
                    .filter(g -> Objects.equals(g.idVehiculo, vehiculo.idVehiculo))
                    .filter(g -> tieneTipo(categorias.get(g.idGastoCategoria), "CFV"))
                    .mapToDouble(this::monto)
                    .sum();
            double cfvTotal = depreciacionMensual + gastosCfv;

            List<Gasto> gastosCvv = gastosMes.stream()
                    .filter(g -> Objects.equals(g.idVehiculo, vehiculo.idVehiculo))
                    .filter(g -> tieneTipo(categorias.get(g.idGastoCategoria), "CVV"))
                    .toList();
            double cvvTotal = gastosCvv.stream().mapToDouble(this::monto).sum();
            double galones = gastosCvv.stream()
                    .filter(g -> {
                        CostoGastoCategoria categoria = categorias.get(g.idGastoCategoria);
                        return categoria != null && categoria.nombreCategoria != null
                                && "combustible".equalsIgnoreCase(categoria.nombreCategoria.trim());
                    })
                    .mapToDouble(g -> g.cantidadGalones == null ? 0 : g.cantidadGalones)
                    .sum();

            List<Mantenimiento> mantenimientos = Mantenimiento.<Mantenimiento>list("idEmpresa", empresa)
                    .stream()
                    .filter(m -> Objects.equals(m.idVehiculo, vehiculo.idVehiculo))
                    .filter(m -> enPeriodo(m.fechaEntrada, inicio, fin))
                    .toList();
            for (Mantenimiento mantenimiento : mantenimientos) {
                cvvTotal += mantenimiento.costoReparacion == null ? 0 : mantenimiento.costoReparacion;
                cvvTotal += RepuestoMantenimientoDetalle
                        .<RepuestoMantenimientoDetalle>find(
                                "mantenimiento.idMantenimiento", mantenimiento.idMantenimiento)
                        .stream()
                        .mapToDouble(r -> r.costoTotalRepuesto == null ? 0 : r.costoTotalRepuesto)
                        .sum();
            }

            double kilometros = completado == null ? 0 : Viaje.<Viaje>list("idEmpresa", empresa)
                    .stream()
                    .filter(v -> Objects.equals(v.idVehiculo, vehiculo.idVehiculo))
                    .filter(v -> Objects.equals(v.idViajeEstado, completado.idViajeEstado))
                    .filter(v -> enPeriodo(v.fechaLlegada, inicio, fin))
                    .filter(v -> v.kilometrajeSalida != null && v.kilometrajeLlegada != null)
                    .mapToDouble(v -> Math.max(0, v.kilometrajeLlegada - v.kilometrajeSalida))
                    .sum();

            Double cvvPorKm = kilometros > 0 ? cvvTotal / kilometros : null;
            Double ckv = kilometros > 0
                    ? ((cfpProrrateado + cfvTotal) / kilometros) + cvvPorKm
                    : null;
            Double kmPorGalon = galones > 0 ? kilometros / galones : null;
            if (ckv != null) {
                sumaCkv += ckv;
                cantidadCkv++;
            }

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("idVehiculo", vehiculo.idVehiculo);
            item.put("placa", vehiculo.placa);
            item.put("cfpProrrateado", cfpProrrateado);
            item.put("cfvTotal", cfvTotal);
            item.put("cvvTotal", cvvTotal);
            item.put("kilometrosDelMes", kilometros);
            item.put("ckv", ckv);
            item.put("kmPorGalon", kmPorGalon);
            item.put("datosIncompletos", datosIncompletos);
            detalle.add(item);
        }

        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("vehiculos", detalle);
        respuesta.put("ckvPromedioFlota", cantidadCkv > 0 ? sumaCkv / cantidadCkv : null);
        respuesta.put("mesConsultado", mesConsultado);
        respuesta.put("anioConsultado", anioConsultado);
        return respuesta;
    }

    @GET
    @Path("/iuv-mensual")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> iuvMensual(
            @QueryParam("idEmpresa") String idEmpresa,
            @QueryParam("mes") Integer mes,
            @QueryParam("anio") Integer anio) {
        YearMonth actual = YearMonth.now();
        int mesConsultado = mes == null ? actual.getMonthValue() : mes;
        int anioConsultado = anio == null ? actual.getYear() : anio;
        if (mesConsultado < 1 || mesConsultado > 12 || anioConsultado < 1) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "El mes o año consultado no es válido"))
                    .type(MediaType.APPLICATION_JSON)
                    .build());
        }

        String empresa = TenantAccess.company(request);
        LocalDateTime inicio = LocalDate.of(anioConsultado, mesConsultado, 1).atStartOfDay();
        LocalDateTime fin = inicio.plusMonths(1);
        OperacionViajeEstado completado = OperacionViajeEstado
                .find("lower(nombreEstado) = ?1", "completado")
                .firstResult();
        List<Viaje> viajesMes = completado == null
                ? List.of()
                : Viaje.<Viaje>list("idEmpresa", empresa).stream()
                        .filter(v -> Objects.equals(v.idViajeEstado, completado.idViajeEstado))
                        .filter(v -> enPeriodo(v.fechaLlegada, inicio, fin))
                        .toList();
        List<Vehiculo> vehiculos = Vehiculo.list(
                "idEmpresa = ?1 and idEstadoOperativo = ?2", empresa, 1);

        List<Map<String, Object>> detalle = new ArrayList<>();
        double sumaIuv = 0;
        int cantidadConIuv = 0;
        int cantidadPermanente = 0;
        int cantidadEventual = 0;
        int cantidadSinParametro = 0;

        for (Vehiculo vehiculo : vehiculos) {
            VehiculoTipoUnidad tipo = vehiculo.idTipoUnidad == null
                    ? null : VehiculoTipoUnidad.findById(vehiculo.idTipoUnidad);
            List<Viaje> viajesVehiculo = viajesMes.stream()
                    .filter(v -> Objects.equals(v.idVehiculo, vehiculo.idVehiculo))
                    .toList();
            double kilometros = viajesVehiculo.stream()
                    .filter(v -> v.kilometrajeSalida != null && v.kilometrajeLlegada != null)
                    .mapToDouble(v -> Math.max(0, v.kilometrajeLlegada - v.kilometrajeSalida))
                    .sum();
            double horas = viajesVehiculo.stream()
                    .filter(v -> v.fechaSalida != null && v.fechaLlegada != null)
                    .mapToDouble(v -> Math.max(0,
                            Duration.between(v.fechaSalida, v.fechaLlegada).toMinutes() / 60.0))
                    .sum();

            Integer kmParametro = tipo == null ? null : tipo.kmParametroMensual;
            Integer horasParametro = tipo == null ? null : tipo.horasParametroMensual;
            Double iuvKm = kmParametro != null && kmParametro > 0
                    ? kilometros / kmParametro : null;
            Double iuvHoras = horasParametro != null && horasParametro > 0
                    ? horas / horasParametro : null;
            Double iuvGeneral;
            if (iuvKm != null && iuvHoras != null) {
                iuvGeneral = (iuvKm + iuvHoras) / 2.0;
            } else {
                iuvGeneral = iuvKm != null ? iuvKm : iuvHoras;
            }

            String clasificacion;
            if (iuvGeneral == null) {
                clasificacion = "Sin parámetro configurado";
                cantidadSinParametro++;
            } else if (iuvGeneral >= 1) {
                clasificacion = "Servicio Permanente";
                cantidadPermanente++;
                sumaIuv += iuvGeneral;
                cantidadConIuv++;
            } else {
                clasificacion = "Uso Eventual - Candidato a Flota Común";
                cantidadEventual++;
                sumaIuv += iuvGeneral;
                cantidadConIuv++;
            }

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("idVehiculo", vehiculo.idVehiculo);
            item.put("placa", vehiculo.placa);
            item.put("tipoUnidad", tipo == null ? null : tipo.nombreTipo);
            item.put("kilometrosDelMes", kilometros);
            item.put("horasDelMes", horas);
            item.put("kmParametroMensual", kmParametro);
            item.put("horasParametroMensual", horasParametro);
            item.put("iuvKm", iuvKm);
            item.put("iuvHoras", iuvHoras);
            item.put("iuvGeneral", iuvGeneral);
            item.put("clasificacion", clasificacion);
            detalle.add(item);
        }

        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("vehiculos", detalle);
        respuesta.put("iuvPromedioFlota",
                cantidadConIuv > 0 ? sumaIuv / cantidadConIuv : null);
        respuesta.put("cantidadServicioPermanente", cantidadPermanente);
        respuesta.put("cantidadUsoEventual", cantidadEventual);
        respuesta.put("cantidadSinParametro", cantidadSinParametro);
        respuesta.put("mesConsultado", mesConsultado);
        respuesta.put("anioConsultado", anioConsultado);
        return respuesta;
    }

    private boolean tieneTipo(CostoGastoCategoria categoria, String tipo) {
        return categoria != null && categoria.tipoCosto != null
                && tipo.equalsIgnoreCase(categoria.tipoCosto.trim());
    }

    private double monto(Gasto gasto) {
        return gasto.monto == null ? 0 : gasto.monto.doubleValue();
    }

    private boolean enPeriodo(LocalDateTime fecha, LocalDateTime inicio, LocalDateTime fin) {
        return fecha != null && !fecha.isBefore(inicio) && fecha.isBefore(fin);
    }

    private boolean esCostoMantenimiento(Gasto gasto) {
        CostoGastoCategoria categoria = CostoGastoCategoria.findById(gasto.idGastoCategoria);
        if (categoria == null || categoria.nombreCategoria == null) return false;
        String nombre = categoria.nombreCategoria.toLowerCase(Locale.ROOT);
        return nombre.contains("mantenimiento") || nombre.contains("mano de obra") || nombre.contains("repuesto");
    }
}
