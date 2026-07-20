package com.fleettrackpro;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Path("/gastos-internos-saas")
public class GastoInternoSaasResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<GastoInternoSaas> listar() {
        return GastoInternoSaas.listAll();
    }

    @POST
    @Transactional
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response crear(GastoInternoSaas nuevo) {
        if (nuevo.idMetodoPago == null || MetodoPagoSaas.findById(nuevo.idMetodoPago) == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("mensaje", "Selecciona un método de pago válido.")).build();
        }
        try {
            nuevo.fechaRegistro = LocalDateTime.now();
            nuevo.persistAndFlush();
            return Response.status(Response.Status.CREATED).entity(nuevo).build();
        } catch (RuntimeException ex) {
            return Response.serverError()
                    .entity(Map.of("mensaje", "No se pudo registrar el gasto: " + causaRaiz(ex))).build();
        }
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response obtener(@PathParam("id") Integer id) {
        GastoInternoSaas g = GastoInternoSaas.findById(id);
        if (g == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(g).build();
    }

    @PUT
    @Path("/{id}")
    @Transactional
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response actualizar(@PathParam("id") Integer id, GastoInternoSaas req) {
        GastoInternoSaas g = GastoInternoSaas.findById(id);
        if (g == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        if (req.idMetodoPago == null || MetodoPagoSaas.findById(req.idMetodoPago) == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("mensaje", "Selecciona un método de pago válido.")).build();
        }

        g.concepto = req.concepto;
        g.idCategoria = req.idCategoria;
        g.monto = req.monto;
        g.fechaGasto = req.fechaGasto;
        g.comprobanteNro = req.comprobanteNro;
        g.idMetodoPago = req.idMetodoPago;
        g.descripcion = req.descripcion;

        return Response.ok(g).build();
    }

    private String causaRaiz(Throwable error) {
        Throwable causa = error;
        while (causa.getCause() != null) {
            causa = causa.getCause();
        }
        return causa.getMessage() != null ? causa.getMessage() : causa.getClass().getSimpleName();
    }
}
