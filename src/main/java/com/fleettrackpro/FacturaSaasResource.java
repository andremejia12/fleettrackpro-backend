package com.fleettrackpro;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Path("/facturas-saas")
public class FacturaSaasResource {

    public static class FacturaSaasDTO {
        public Integer idFactura;
        public String idEmpresa;
        public String numeroFactura;
        public String periodoFacturado;
        public BigDecimal monto;
        public String moneda;
        public LocalDate fechaEmision;
        public LocalDate fechaVencimiento;
        public Integer idEstadoPago;
        public LocalDate fechaPago;
        public Integer idMetodoPago;
        public String referenciaPago;
        public String nombreEmpresa;
        public String razonSocialEmpresa;
        public String rucEmpresa;
        public String nombreEstado;
        public String nombreMetodoPago;

        public static FacturaSaasDTO from(FacturaSaas factura) {
            FacturaSaasDTO dto = new FacturaSaasDTO();
            Empresa empresa = Empresa.findById(factura.idEmpresa);
            FacturaEstadoPago estado = factura.idEstadoPago == null
                    ? null : FacturaEstadoPago.findById(factura.idEstadoPago);
            MetodoPagoSaas metodo = factura.idMetodoPago == null
                    ? null : MetodoPagoSaas.findById(factura.idMetodoPago);
            dto.idFactura = factura.idFactura;
            dto.idEmpresa = factura.idEmpresa;
            dto.numeroFactura = factura.numeroFactura;
            dto.periodoFacturado = factura.periodoFacturado;
            dto.monto = factura.monto;
            dto.moneda = factura.moneda;
            dto.fechaEmision = factura.fechaEmision;
            dto.fechaVencimiento = factura.fechaVencimiento;
            dto.idEstadoPago = factura.idEstadoPago;
            dto.fechaPago = factura.fechaPago;
            dto.idMetodoPago = factura.idMetodoPago;
            dto.referenciaPago = factura.referenciaPago;
            dto.nombreEmpresa = empresa == null
                    ? "" : (empresa.nombreComercial == null || empresa.nombreComercial.isBlank()
                            ? empresa.razonSocial : empresa.nombreComercial);
            dto.razonSocialEmpresa = empresa == null ? "" : empresa.razonSocial;
            dto.rucEmpresa = empresa == null ? "" : empresa.rucTaxId;
            dto.nombreEstado = estado == null ? "" : estado.nombreEstado;
            dto.nombreMetodoPago = metodo == null ? "" : metodo.nombreMetodo;
            return dto;
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<FacturaSaasDTO> listar() {
        List<FacturaSaas> facturas = FacturaSaas.listAll();
        return facturas.stream().map(FacturaSaasDTO::from).toList();
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response obtener(@PathParam("id") Integer id) {
        FacturaSaas f = FacturaSaas.findById(id);
        if (f == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(f).build();
    }

}
