package com.fleettrackpro;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Path("/dashboard-saas")
public class DashboardSaasResource {

    public static class FacturaVencimientoResponse {
        public Integer idFactura;
        public String numeroFactura;
        public String nombreEmpresa;
        public Double monto;
        public String moneda;
        public LocalDate fechaVencimiento;
        public Long diasRestantes;
    }

    @GET
    @Path("/facturas-por-vencer")
    @Produces(MediaType.APPLICATION_JSON)
    public List<FacturaVencimientoResponse> facturasPorVencer() {
        LocalDate hoy = LocalDate.now();
        LocalDate limite = hoy.plusDays(15);

        // Find all pending invoices (idEstadoPago = 1) with due date within 15 days (or overdue)
        List<FacturaSaas> listaFacturas = FacturaSaas.list("idEstadoPago = 1 and fechaVencimiento is not null and fechaVencimiento <= ?1", limite);

        return listaFacturas.stream().map(f -> {
            FacturaVencimientoResponse r = new FacturaVencimientoResponse();
            r.idFactura = f.idFactura;
            r.numeroFactura = f.numeroFactura;
            
            // Query companies.empresas
            Empresa emp = Empresa.findById(f.idEmpresa);
            if (emp != null) {
                r.nombreEmpresa = emp.nombreComercial != null ? emp.nombreComercial : emp.razonSocial;
            } else {
                r.nombreEmpresa = "Empresa Desconocida";
            }
            
            r.monto = f.monto != null ? f.monto.doubleValue() : 0.0;
            r.moneda = f.moneda;
            r.fechaVencimiento = f.fechaVencimiento;
            
            // Calculate days between hoy and fechaVencimiento
            r.diasRestantes = java.time.temporal.ChronoUnit.DAYS.between(hoy, f.fechaVencimiento);
            
            return r;
        }).sorted(Comparator.comparing(r -> r.fechaVencimiento))
          .collect(Collectors.toList());
    }
}
