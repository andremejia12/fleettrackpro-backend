package com.fleettrackpro;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.container.ContainerRequestContext;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Path("/empresas")
public class EmpresaResource {
    @Context ContainerRequestContext request;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<EmpresaConCatalogosDTO> listar() {
        TenantAccess.requireAdminSaas(request);
        List<Empresa> empresas = Empresa.listAll();
        return empresas.stream().map(this::conCatalogos).toList();
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response obtener(@PathParam("id") String id) {
        Empresa e = Empresa.findById(id);
        if (e == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        TenantAccess.require(request, e.idEmpresa);
        return Response.ok(conCatalogos(e)).build();
    }

    @PUT
    @Path("/{id}")
    @Transactional
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response actualizar(@PathParam("id") String id, EmpresaRequest req) {
        Empresa e = Empresa.findById(id);
        if (e == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        TenantAccess.require(request, e.idEmpresa);

        e.rucTaxId = req.rucTaxId;
        e.razonSocial = req.razonSocial;
        e.nombreComercial = req.nombreComercial;
        e.giroNegocio = req.giroNegocio;
        e.descripcionCorporativa = req.descripcionCorporativa;
        e.direccionFiscal = req.direccionFiscal;
        e.telefonoCorporativo = req.telefonoCorporativo;
        e.emailCorporativo = req.emailCorporativo;
        e.sitioWeb = req.sitioWeb;
        validarCatalogos(req);
        e.idSuscripcionEstado = req.idSuscripcionEstado;
        e.idSaasPlan = req.idSaasPlan;
        e.monedaBase = req.monedaBase;
        e.logoUrl = req.logoUrl;
        e.paisOperacion = req.paisOperacion;

        return Response.ok(conCatalogos(e)).build();
    }

    @Inject
    EntityManager em;

    public static class EmpresaRequest {
        public String rucTaxId;
        public String razonSocial;
        public String nombreComercial;
        public String giroNegocio;
        public String descripcionCorporativa;
        public String direccionFiscal;
        public String telefonoCorporativo;
        public String emailCorporativo;
        public String sitioWeb;
        public Integer idSuscripcionEstado;
        public Integer idSaasPlan;
        public String monedaBase;
        public String logoUrl;
        public String paisOperacion;
    }

    private String generarNuevoIdEmpresa() {
        Number siguiente = (Number) em.createNativeQuery(
                "SELECT nextval('companies.empresa_seq')").getSingleResult();
        return String.format("EMP-%03d", siguiente.intValue());
    }

    @POST
    @Transactional
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response crear(EmpresaRequest req) {
        TenantAccess.requireAdminSaas(request);
        Empresa nueva = new Empresa();
        nueva.idEmpresa = generarNuevoIdEmpresa();
        nueva.rucTaxId = req.rucTaxId;
        nueva.razonSocial = req.razonSocial;
        nueva.nombreComercial = req.nombreComercial;
        nueva.giroNegocio = req.giroNegocio;
        nueva.descripcionCorporativa = req.descripcionCorporativa;
        nueva.direccionFiscal = req.direccionFiscal;
        nueva.telefonoCorporativo = req.telefonoCorporativo;
        nueva.emailCorporativo = req.emailCorporativo;
        nueva.sitioWeb = req.sitioWeb;
        validarCatalogos(req);
        nueva.idSuscripcionEstado = req.idSuscripcionEstado;
        nueva.idSaasPlan = req.idSaasPlan;
        nueva.monedaBase = req.monedaBase;
        nueva.fechaRegistroSaas = LocalDateTime.now();
        nueva.logoUrl = req.logoUrl;
        nueva.paisOperacion = req.paisOperacion;

        nueva.persist();

        return Response.status(Response.Status.CREATED).entity(conCatalogos(nueva)).build();
    }

    private void validarCatalogos(EmpresaRequest req) {
        if (req == null || req.idSaasPlan == null || EmpresaSaasPlan.findById(req.idSaasPlan) == null) {
            throw badRequest("Selecciona un plan de suscripción válido");
        }
        if (req.idSuscripcionEstado == null
                || EmpresaSuscripcionEstado.findById(req.idSuscripcionEstado) == null) {
            throw badRequest("Selecciona un estado de suscripción válido");
        }
    }

    private EmpresaConCatalogosDTO conCatalogos(Empresa empresa) {
        EmpresaSaasPlan plan = empresa.idSaasPlan == null
                ? null
                : EmpresaSaasPlan.findById(empresa.idSaasPlan);
        EmpresaSuscripcionEstado estado = empresa.idSuscripcionEstado == null
                ? null
                : EmpresaSuscripcionEstado.findById(empresa.idSuscripcionEstado);
        return EmpresaConCatalogosDTO.from(empresa, plan, estado);
    }

    private WebApplicationException badRequest(String mensaje) {
        return new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("message", mensaje))
                .type(MediaType.APPLICATION_JSON)
                .build());
    }
}
