package com.fleettrackpro;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Arrays;
import java.util.Map;

public final class RoleAccess {
    private static final String ROL_CONDUCTOR = "conductor";

    private RoleAccess() {
    }

    public static boolean isConductor(ContainerRequestContext request) {
        return hasRole(request, ROL_CONDUCTOR);
    }

    public static boolean hasRole(ContainerRequestContext request, String role) {
        String currentRole = roleName(request);
        return currentRole != null && role != null
                && role.trim().equalsIgnoreCase(currentRole);
    }

    public static void requireRole(ContainerRequestContext request, String... allowedRoles) {
        String currentRole = roleName(request);
        boolean allowed = currentRole != null && allowedRoles != null
                && Arrays.stream(allowedRoles)
                        .filter(role -> role != null)
                        .anyMatch(role -> role.trim().equalsIgnoreCase(currentRole));
        if (!allowed) {
            throw forbidden("Tu rol no tiene permiso para realizar esta acción");
        }
    }

    private static String roleName(ContainerRequestContext request) {
        SessionService.Session session = session(request);
        if (session == null || session.administradorSaas() || session.userId() == null) {
            return null;
        }
        Usuario usuario = Usuario.findById(session.userId());
        if (usuario == null || usuario.idRol == null) {
            return null;
        }
        Rol rol = Rol.findById(usuario.idRol);
        return rol == null || rol.nombreRol == null ? null : rol.nombreRol.trim();
    }

    public static Integer conductorIdFor(ContainerRequestContext request) {
        if (!isConductor(request)) {
            return null;
        }
        SessionService.Session session = session(request);
        Conductor conductor = Conductor.find("idUsuario", session.userId()).firstResult();
        if (conductor == null) {
            throw forbidden("El usuario conductor no está vinculado a un conductor");
        }
        TenantAccess.require(request, conductor.idEmpresa);
        return conductor.idConductor;
    }

    public static void requireNotConductor(ContainerRequestContext request) {
        if (isConductor(request)) {
            throw forbidden("El rol conductor no tiene acceso a este recurso");
        }
    }

    private static SessionService.Session session(ContainerRequestContext request) {
        Object value = request.getProperty("fleettrack.session");
        return value instanceof SessionService.Session session ? session : null;
    }

    private static WebApplicationException forbidden(String mensaje) {
        return new WebApplicationException(Response.status(Response.Status.FORBIDDEN)
                .entity(Map.of("message", mensaje))
                .type(MediaType.APPLICATION_JSON)
                .build());
    }
}
