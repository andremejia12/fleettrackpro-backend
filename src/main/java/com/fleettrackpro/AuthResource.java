package com.fleettrackpro;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.inject.Inject;

@Path("/auth")
public class AuthResource {
    @Inject
    SessionService sessions;

    public static class LoginRequest {
        public String email;
        public String contrasenia;
    }

    public static class LoginResponse {
        public Integer idUser;
        public String nombre;
        public String email;
        public String rol;
        public String token;
        public String idEmpresa;
    }

    private boolean passwordCorrecta(String contraseniaPlano, String hashGuardado) {
        String hashNormalizado = hashGuardado.replaceFirst("^\\$2[abxy]\\$", "\\$2a\\$");
        return BcryptUtil.matches(contraseniaPlano, hashNormalizado);
    }

    @POST
    @Path("/login")
    @Transactional
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(LoginRequest req) {
        Usuario usuario = Usuario.find("email", req.email).firstResult();

        if (usuario == null || !passwordCorrecta(req.contrasenia, usuario.contrasenia)) {
            return Response.status(401)
                    .entity("{\"mensaje\":\"Correo electrónico o contraseña incorrectos.\"}")
                    .build();
        }

        Rol rol = Rol.findById(usuario.idRol);

        LoginResponse resp = new LoginResponse();
        resp.idUser = usuario.idUser;
        resp.nombre = usuario.nombre;
        resp.email = usuario.email;
        resp.rol = rol != null ? rol.nombreRol : "";
        resp.token = sessions.create(usuario.idUser, usuario.idEmpresa, false);
        resp.idEmpresa = usuario.idEmpresa;

        return Response.ok(resp).build();
    }
}
