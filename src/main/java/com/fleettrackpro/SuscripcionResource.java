package com.fleettrackpro;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.QueryParam;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/mi-suscripcion")
public class SuscripcionResource {

    public static class PagoRequest {
        public Integer idMetodoPago;
        public String referenciaPago;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response obtener(@QueryParam("idEmpresa") String idEmpresa) {
        if (idEmpresa == null || idEmpresa.isBlank()) {
            return Response.status(400).entity("{\"mensaje\":\"idEmpresa es requerido\"}").build();
        }

        FacturaEstadoPago estadoPendiente = FacturaEstadoPago.find("nombreEstado", "Pendiente").firstResult();
        List<FacturaSaas> facturas = estadoPendiente == null
                ? List.of()
                : FacturaSaas.list(
                        "idEmpresa = ?1 and idEstadoPago = ?2 order by fechaVencimiento asc",
                        idEmpresa, estadoPendiente.idEstadoPago);

        if (facturas.isEmpty()) {
            Empresa empresa = Empresa.findById(idEmpresa);
            if (empresa == null || empresa.fechaRegistroSaas == null) {
                return Response.noContent().build();
            }
            LocalDate proximaFecha = proximaFechaFacturacion(empresa);
            EmpresaSaasPlan plan = EmpresaSaasPlan.find("lower(nombrePlan) = lower(?1)", empresa.planSuscripcion)
                    .firstResult();

            Map<String, Object> result = new HashMap<>();
            result.put("tienePagoPendiente", false);
            result.put("fechaProximaFactura", proximaFecha);
            LocalDate fechaHabilitacionPago = proximaFecha.minusDays(7);
            boolean dentroDeVentanaPago = !LocalDate.now().isBefore(fechaHabilitacionPago);
            result.put("puedePagarAdelantado",
                    dentroDeVentanaPago
                            && FacturaSaas.count("idEmpresa = ?1 and fechaVencimiento = ?2", idEmpresa, proximaFecha) == 0);
            result.put("fechaHabilitacionPago", fechaHabilitacionPago);
            result.put("monto", plan == null ? null : plan.precioMensual);
            result.put("moneda", empresa.monedaBase == null ? "PEN" : empresa.monedaBase);
            return Response.ok(result).build();
        }

        FacturaSaas siguiente = facturas.get(0);
        long diasRestantes = ChronoUnit.DAYS.between(LocalDate.now(), siguiente.fechaVencimiento);

        Map<String, Object> result = new HashMap<>();
        result.put("tienePagoPendiente", true);
        result.put("idFactura", siguiente.idFactura);
        result.put("numeroFactura", siguiente.numeroFactura);
        result.put("periodoFacturado", siguiente.periodoFacturado);
        result.put("monto", siguiente.monto);
        result.put("moneda", siguiente.moneda);
        result.put("fechaVencimiento", siguiente.fechaVencimiento);
        result.put("diasRestantes", diasRestantes);

        return Response.ok(result).build();
    }

    @POST
    @Path("/pagar-adelantado")
    @Transactional
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response pagarAdelantado(@QueryParam("idEmpresa") String idEmpresa, PagoRequest pago) {
        if (idEmpresa == null || idEmpresa.isBlank()) {
            return error(400, "idEmpresa es requerido");
        }
        if (pago == null || pago.idMetodoPago == null) {
            return error(400, "idMetodoPago es requerido");
        }
        if (MetodoPagoSaas.findById(pago.idMetodoPago) == null) {
            return error(400, "Método de pago no válido");
        }

        Empresa empresa = Empresa.findById(idEmpresa);
        if (empresa == null || empresa.fechaRegistroSaas == null) {
            return error(404, "Empresa o fecha de suscripción no encontrada");
        }
        EmpresaSaasPlan plan = EmpresaSaasPlan.find("lower(nombrePlan) = lower(?1)", empresa.planSuscripcion)
                .firstResult();
        if (plan == null) {
            return error(409, "El plan de suscripción no tiene una tarifa configurada");
        }

        LocalDate fechaFactura = proximaFechaFacturacion(empresa);
        LocalDate fechaHabilitacionPago = fechaFactura.minusDays(7);
        if (LocalDate.now().isBefore(fechaHabilitacionPago)) {
            return error(422, "La factura podrá pagarse desde el " + fechaHabilitacionPago);
        }
        long yaRegistrada = FacturaSaas.count("idEmpresa = ?1 and fechaVencimiento = ?2", idEmpresa, fechaFactura);
        if (yaRegistrada > 0) {
            return error(409, "La próxima factura ya fue registrada");
        }

        FacturaEstadoPago pagado = obtenerOCrearEstado("Pagado");
        FacturaSaas factura = new FacturaSaas();
        factura.idEmpresa = idEmpresa;
        factura.numeroFactura = String.format("SAAS-%04d", FacturaSaas.count() + 1);
        factura.periodoFacturado = String.format("%d-%02d", fechaFactura.getYear(), fechaFactura.getMonthValue());
        factura.monto = plan.precioMensual;
        factura.moneda = empresa.monedaBase == null ? "PEN" : empresa.monedaBase;
        factura.fechaEmision = LocalDate.now();
        factura.fechaVencimiento = fechaFactura;
        factura.idEstadoPago = pagado.idEstadoPago;
        factura.fechaPago = LocalDate.now();
        factura.idMetodoPago = pago.idMetodoPago;
        factura.referenciaPago = pago.referenciaPago;
        factura.persist();

        return Response.ok(resultadoPago(factura, pagado)).build();
    }

    @GET
    @Path("/pagos")
    @Produces(MediaType.APPLICATION_JSON)
    public Response pagos(@QueryParam("idEmpresa") String idEmpresa) {
        if (idEmpresa == null || idEmpresa.isBlank()) {
            return error(400, "idEmpresa es requerido");
        }
        FacturaEstadoPago pagado = FacturaEstadoPago.find("nombreEstado", "Pagado").firstResult();
        if (pagado == null)
            return Response.ok(List.of()).build();

        List<FacturaSaas> pagos = FacturaSaas.list(
                "idEmpresa = ?1 and idEstadoPago = ?2 order by fechaPago desc, idFactura desc",
                idEmpresa, pagado.idEstadoPago);
        return Response.ok(pagos).build();
    }

    @POST
    @Path("/{idFactura}/pagar")
    @Transactional
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response pagar(
            @PathParam("idFactura") Integer idFactura,
            @QueryParam("idEmpresa") String idEmpresa,
            PagoRequest pago) {
        if (idEmpresa == null || idEmpresa.isBlank()) {
            return error(400, "idEmpresa es requerido");
        }
        if (pago == null || pago.idMetodoPago == null) {
            return error(400, "idMetodoPago es requerido");
        }
        if (MetodoPagoSaas.findById(pago.idMetodoPago) == null) {
            return error(400, "Método de pago no válido");
        }

        FacturaSaas factura = FacturaSaas.findById(idFactura);
        if (factura == null || !idEmpresa.equals(factura.idEmpresa)) {
            return error(404, "Factura no encontrada para la empresa");
        }

        FacturaEstadoPago pendiente = FacturaEstadoPago.find("nombreEstado", "Pendiente").firstResult();
        FacturaEstadoPago pagado = obtenerOCrearEstado("Pagado");
        if (pendiente == null)
            return error(409, "El estado Pendiente no está configurado");
        if (!pendiente.idEstadoPago.equals(factura.idEstadoPago)) {
            return error(409, "La factura ya fue pagada o no está pendiente");
        }
        if (factura.fechaVencimiento == null) {
            return error(409, "La factura no tiene fecha de vencimiento");
        }

        LocalDate fechaHabilitacionPago = factura.fechaVencimiento.minusDays(7);
        if (LocalDate.now().isBefore(fechaHabilitacionPago)) {
            return error(422, "La factura podrá pagarse desde el " + fechaHabilitacionPago);
        }

        factura.idEstadoPago = pagado.idEstadoPago;
        factura.fechaPago = LocalDate.now();
        factura.idMetodoPago = pago.idMetodoPago;
        factura.referenciaPago = pago.referenciaPago;

        return Response.ok(resultadoPago(factura, pagado)).build();
    }

    private LocalDate proximaFechaFacturacion(Empresa empresa) {
        LocalDate hoy = LocalDate.now();
        int dia = empresa.fechaRegistroSaas.getDayOfMonth();
        YearMonth mes = YearMonth.from(hoy);
        LocalDate candidata = mes.atDay(Math.min(dia, mes.lengthOfMonth()));
        if (!candidata.isAfter(hoy)) {
            mes = mes.plusMonths(1);
            candidata = mes.atDay(Math.min(dia, mes.lengthOfMonth()));
        }

        FacturaSaas ultimaFactura = FacturaSaas.find(
                "idEmpresa = ?1 and fechaVencimiento is not null order by fechaVencimiento desc",
                empresa.idEmpresa).firstResult();
        if (ultimaFactura != null && !ultimaFactura.fechaVencimiento.isBefore(candidata)) {
            YearMonth siguienteMes = YearMonth.from(ultimaFactura.fechaVencimiento).plusMonths(1);
            candidata = siguienteMes.atDay(Math.min(dia, siguienteMes.lengthOfMonth()));
        }
        return candidata;
    }

    private FacturaEstadoPago obtenerOCrearEstado(String nombre) {
        FacturaEstadoPago estado = FacturaEstadoPago.find("nombreEstado", nombre).firstResult();
        if (estado == null) {
            estado = new FacturaEstadoPago();
            estado.nombreEstado = nombre;
            estado.persist();
        }
        return estado;
    }

    private Map<String, Object> resultadoPago(FacturaSaas factura, FacturaEstadoPago estado) {
        Map<String, Object> result = new HashMap<>();
        result.put("idFactura", factura.idFactura);
        result.put("numeroFactura", factura.numeroFactura);
        result.put("estado", estado.nombreEstado);
        result.put("fechaPago", factura.fechaPago);
        return result;
    }

    private Response error(int status, String mensaje) {
        return Response.status(status).entity(Map.of("mensaje", mensaje)).build();
    }
}
