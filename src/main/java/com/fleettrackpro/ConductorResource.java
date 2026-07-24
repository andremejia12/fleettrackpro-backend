package com.fleettrackpro;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.container.ContainerRequestContext;
import io.quarkus.elytron.security.common.BcryptUtil;
import java.util.List;
import java.util.Map;

@Path("/conductores")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ConductorResource {
    public static class CrearAccesoRequest {
        public String email;
        public String contrasenia;
    }

    @Context
    ContainerRequestContext request;

    @GET
    public List<Conductor> listar(@QueryParam("idEmpresa") String idEmpresa) {
        return Conductor.list("idEmpresa", TenantAccess.company(request));
    }

    @POST
    @Transactional
    public Conductor crear(Conductor nuevo) {
        RoleAccess.requireRole(request, "admin", "despachador");
        nuevo.idEmpresa = TenantAccess.company(request);
        Conductor.persist(nuevo);
        return nuevo;
    }

    @POST
    @Path("/{id}/crear-acceso")
    @Transactional
    public Conductor crearAcceso(@PathParam("id") Integer id, CrearAccesoRequest datos) {
        Conductor conductor = Conductor.findById(id);
        if (conductor == null) {
            throw error(Response.Status.NOT_FOUND, "Conductor no encontrado");
        }
        TenantAccess.require(request, conductor.idEmpresa);
        requireAdmin();

        if (conductor.idUsuario != null) {
            throw error(Response.Status.CONFLICT, "Este conductor ya tiene una cuenta de acceso");
        }
        if (datos == null || datos.email == null || datos.email.isBlank()) {
            throw error(Response.Status.BAD_REQUEST, "El email es obligatorio");
        }
        if (datos.contrasenia == null || datos.contrasenia.length() < 8) {
            throw error(Response.Status.BAD_REQUEST, "La contraseña debe tener al menos 8 caracteres");
        }

        String email = datos.email.trim().toLowerCase();
        if (Usuario.find("lower(email) = ?1", email).count() > 0) {
            throw error(Response.Status.CONFLICT, "Ya existe un usuario con este email");
        }
        if (Usuario.find("dni", conductor.numeroDocumento).count() > 0) {
            throw error(Response.Status.CONFLICT, "Ya existe un usuario con este dni");
        }

        Rol rolConductor = Rol.find("lower(nombreRol) = ?1", "conductor").firstResult();
        if (rolConductor == null) {
            throw error(Response.Status.INTERNAL_SERVER_ERROR, "No se encontró el rol conductor");
        }

        Usuario usuario = new Usuario();
        usuario.nombre = (conductor.nombre + " " + conductor.apellido).trim();
        usuario.contrasenia = BcryptUtil.bcryptHash(datos.contrasenia);
        usuario.dni = conductor.numeroDocumento;
        usuario.email = email;
        usuario.idRol = rolConductor.idRol;
        usuario.idEmpresa = conductor.idEmpresa;
        usuario.idUsuarioEstado = 1;
        usuario.persistAndFlush();

        conductor.idUsuario = usuario.idUser;
        return conductor;
    }

    @PUT
    @Path("/{id}")
    @Transactional
    public Conductor actualizar(@PathParam("id") Integer id, Conductor actualizado) {
        RoleAccess.requireRole(request, "admin", "despachador");
        Conductor conductor = Conductor.findById(id);
        if (conductor == null) {
            throw new NotFoundException("Conductor no encontrado");
        }
        TenantAccess.require(request, conductor.idEmpresa);
        conductor.numeroDocumento = actualizado.numeroDocumento;
        conductor.idTipoDocumento = actualizado.idTipoDocumento;
        conductor.nombre = actualizado.nombre;
        conductor.apellido = actualizado.apellido;
        conductor.licenciaNro = actualizado.licenciaNro;
        conductor.telefono = actualizado.telefono;
        conductor.email = actualizado.email;
        conductor.puesto = actualizado.puesto;
        conductor.idEstadoLaboral = actualizado.idEstadoLaboral;
        conductor.idCategoria = actualizado.idCategoria;
        conductor.licenciaVencimiento = actualizado.licenciaVencimiento;
        conductor.idTipoSangre = actualizado.idTipoSangre;
        conductor.contactoEmergencia = actualizado.contactoEmergencia;
        conductor.costoHora = actualizado.costoHora;
        return conductor;
    }

    @GET
    @Path("/{id}")
    public Conductor obtener(@PathParam("id") Integer id) {
        Conductor conductor = Conductor.findById(id);
        if (conductor == null) {
            throw new NotFoundException("Conductor no encontrado");
        }
        TenantAccess.require(request, conductor.idEmpresa);
        return conductor;
    }

    private WebApplicationException error(Response.Status estado, String mensaje) {
        return new WebApplicationException(Response.status(estado)
                .entity(Map.of("message", mensaje))
                .type(MediaType.APPLICATION_JSON)
                .build());
    }

    private void requireAdmin() {
        Object value = request.getProperty("fleettrack.session");
        if (!(value instanceof SessionService.Session session) || session.administradorSaas()
                || session.userId() == null) {
            throw error(Response.Status.FORBIDDEN, "Este recurso requiere el rol admin");
        }
        Usuario usuario = Usuario.findById(session.userId());
        Rol rol = usuario == null || usuario.idRol == null ? null : Rol.findById(usuario.idRol);
        if (rol == null || rol.nombreRol == null || !"admin".equalsIgnoreCase(rol.nombreRol.trim())) {
            throw error(Response.Status.FORBIDDEN, "Este recurso requiere el rol admin");
        }
    }
}
