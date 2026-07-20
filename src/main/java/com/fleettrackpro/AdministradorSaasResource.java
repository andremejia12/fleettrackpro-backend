package com.fleettrackpro;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import io.quarkus.elytron.security.common.BcryptUtil;

@Path("/administradores-saas")
public class AdministradorSaasResource {

    public static class AdminResponse {
        public Integer idAdmin;
        public String nombre;
        public String email;
        public Integer idRolSaas;
        public String rol;
        public String estadoCuenta;
        public LocalDateTime fechaCreacion;
        public LocalDateTime ultimoAcceso;

        public static AdminResponse fromEntity(AdministradorSaas a) {
            AdminResponse r = new AdminResponse();
            r.idAdmin = a.idAdmin;
            r.nombre = a.nombre;
            r.email = a.email;
            r.idRolSaas = a.idRolSaas;
            r.estadoCuenta = a.estadoCuenta;
            r.fechaCreacion = a.fechaCreacion;
            r.ultimoAcceso = a.ultimoAcceso;
            
            RolSaas rolEntity = RolSaas.findById(a.idRolSaas);
            r.rol = rolEntity != null ? rolEntity.nombreRol : "";
            return r;
        }
    }

    public static class AdminCreateRequest {
        public String nombre;
        public String email;
        public String contrasenia;
        public Integer idRolSaas;
    }

    public static class ChangePasswordRequest {
        public String contraseniaActual;
        public String contraseniaNueva;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<AdminResponse> listar() {
        List<AdministradorSaas> list = AdministradorSaas.listAll();
        return list.stream().map(AdminResponse::fromEntity).collect(Collectors.toList());
    }

    @POST
    @Transactional
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response crear(AdminCreateRequest req) {
        AdministradorSaas a = new AdministradorSaas();
        a.nombre = req.nombre;
        a.email = req.email;
        a.contrasenia = BcryptUtil.bcryptHash(req.contrasenia);
        a.idRolSaas = req.idRolSaas;
        a.estadoCuenta = "Activo";
        a.fechaCreacion = LocalDateTime.now();
        a.persist();

        return Response.status(Response.Status.CREATED).entity(AdminResponse.fromEntity(a)).build();
    }

    @PUT
    @Path("/{id}/password")
    @Transactional
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response cambiarContrasenia(@PathParam("id") Integer id, ChangePasswordRequest req) {
        AdministradorSaas a = AdministradorSaas.findById(id);
        if (a == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"mensaje\":\"Usuario no encontrado.\"}")
                    .build();
        }

        String hashNormalizado = a.contrasenia.replaceFirst("^\\$2[abxy]\\$", "\\$2a\\$");
        if (!BcryptUtil.matches(req.contraseniaActual, hashNormalizado)) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"mensaje\":\"La contraseña actual es incorrecta.\"}")
                    .build();
        }

        if (req.contraseniaNueva == null || req.contraseniaNueva.length() < 6) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"mensaje\":\"La nueva contraseña debe tener al menos 6 caracteres.\"}")
                    .build();
        }

        a.contrasenia = BcryptUtil.bcryptHash(req.contraseniaNueva);
        return Response.ok("{\"ok\":true}").build();
    }

    public static class AdminUpdateRequest {
        public String nombre;
        public Integer idRolSaas;
        public String estadoCuenta;
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response obtener(@PathParam("id") Integer id) {
        AdministradorSaas a = AdministradorSaas.findById(id);
        if (a == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(AdminResponse.fromEntity(a)).build();
    }

    @PUT
    @Path("/{id}")
    @Transactional
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response actualizar(@PathParam("id") Integer id, AdminUpdateRequest req) {
        AdministradorSaas a = AdministradorSaas.findById(id);
        if (a == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        
        if (req.nombre != null) {
            a.nombre = req.nombre;
        }
        if (req.idRolSaas != null) {
            a.idRolSaas = req.idRolSaas;
        }
        if (req.estadoCuenta != null) {
            a.estadoCuenta = req.estadoCuenta;
        }
        
        return Response.ok(AdminResponse.fromEntity(a)).build();
    }
}
