package com.fleettrackpro;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Path("/facturas-saas")
public class FacturaSaasResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<FacturaSaas> listar() {
        return FacturaSaas.listAll();
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
