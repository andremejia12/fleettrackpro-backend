package com.fleettrackpro;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

@Path("/sucursales")
@Produces(MediaType.APPLICATION_JSON)
public class SucursalResource {
    @Context
    ContainerRequestContext request;

    @GET
    public List<SucursalGarita> listar() {
        return SucursalGarita.list("idEmpresa", TenantAccess.company(request));
    }
}
