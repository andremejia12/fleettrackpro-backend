package com.fleettrackpro;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.container.ContainerRequestContext;

public final class TenantAccess {
    private TenantAccess() {
    }

    public static String company(ContainerRequestContext request) {
        Object value = request.getProperty("fleettrack.session");
        if (!(value instanceof SessionService.Session session) || session.idEmpresa() == null) {
            throw new ForbiddenException("La sesión no está asociada a una empresa");
        }
        return session.idEmpresa();
    }

    public static boolean isAdminSaas(ContainerRequestContext request) {
        Object value = request.getProperty("fleettrack.session");
        return value instanceof SessionService.Session session && session.administradorSaas();
    }

    public static void requireAdminSaas(ContainerRequestContext request) {
        if (!isAdminSaas(request)) {
            throw new ForbiddenException("Este recurso requiere una sesión administrativa");
        }
    }

    public static void require(ContainerRequestContext request, String entityCompany) {
        if (isAdminSaas(request)) return;
        if (entityCompany == null || !company(request).equals(entityCompany)) {
            throw new ForbiddenException("El registro no pertenece a tu empresa");
        }
    }
}
