package com.fleettrackpro;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.LocalDateTime;
import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.inject.Inject;

@Path("/admin-auth")
public class AdminAuthResource {
    @Inject
    SessionService sessions;

    public static class LoginRequest {
        public String email;
        public String contrasenia;
    }

    public static class LoginResponse {
        public Integer idAdmin;
        public String nombre;
        public String email;
        public String rol;
        public String token;
        public String estadoCuenta;
    }

    private boolean passwordCorrecta(String contraseniaPlano, String hashGuardado) {
        if (contraseniaPlano == null || hashGuardado == null) {
            return false;
        }
        String hashNormalizado = hashGuardado.replaceFirst("^\\$2[abxy]\\$", "\\$2a\\$");
        return BcryptUtil.matches(contraseniaPlano, hashNormalizado);
    }

    @POST
    @Path("/login")
    @Transactional
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(LoginRequest req) {
        String email = req == null || req.email == null ? "" : req.email.trim();
        AdministradorSaas admin = AdministradorSaas
                .find("lower(email) = ?1", email.toLowerCase(java.util.Locale.ROOT))
                .firstResult();

        if (admin == null || !passwordCorrecta(req == null ? null : req.contrasenia, admin.contrasenia)) {
            return Response.status(401)
                    .entity("{\"mensaje\":\"Correo electrónico o contraseña incorrectos.\"}")
                    .build();
        }

        RolSaas rol = RolSaas.findById(admin.idRolSaas);
        admin.ultimoAcceso = LocalDateTime.now();

        LoginResponse resp = new LoginResponse();
        resp.idAdmin = admin.idAdmin;
        resp.nombre = admin.nombre;
        resp.email = admin.email;
        resp.rol = rol != null ? rol.nombreRol : "";
        resp.token = sessions.create(admin.idAdmin, null, true);
        resp.estadoCuenta = admin.estadoCuenta;

        return Response.ok(resp).build();
    }
}
