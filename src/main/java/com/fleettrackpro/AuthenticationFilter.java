package com.fleettrackpro;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.Provider;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class AuthenticationFilter implements ContainerRequestFilter {
    @Inject
    SessionService sessions;

    @Override
    public void filter(ContainerRequestContext request) {
        String path = request.getUriInfo().getPath();
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        if (HttpMethod.OPTIONS.equals(request.getMethod())
                || "auth/login".equals(path)
                || "admin-auth/login".equals(path)) {
            return;
        }

        String authorization = request.getHeaderString(HttpHeaders.AUTHORIZATION);
        String token = authorization != null && authorization.startsWith("Bearer ")
                ? authorization.substring(7).trim()
                : null;
        SessionService.Session session = sessions.validate(token);
        if (session == null) {
            request.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                    .type(MediaType.APPLICATION_JSON)
                    .entity("{\"mensaje\":\"Sesión inválida o expirada.\"}")
                    .build());
            return;
        }

        boolean adminPath = path.startsWith("administradores-saas")
                || path.startsWith("dashboard-saas")
                || path.startsWith("gastos-internos-saas")
                || path.startsWith("facturas-saas");
        if (adminPath && !session.administradorSaas()) {
            request.abortWith(forbidden("Este recurso requiere una sesión administrativa."));
            return;
        }

        MultivaluedMap<String, String> query = request.getUriInfo().getQueryParameters();
        String requestedCompany = query.getFirst("idEmpresa");
        if (!session.administradorSaas() && requestedCompany != null
                && !requestedCompany.equals(session.idEmpresa())) {
            request.abortWith(forbidden("No puedes consultar información de otra empresa."));
            return;
        }

        request.setProperty("fleettrack.session", session);
    }

    private Response forbidden(String message) {
        return Response.status(Response.Status.FORBIDDEN)
                .type(MediaType.APPLICATION_JSON)
                .entity("{\"mensaje\":\"" + message + "\"}")
                .build();
    }
}
