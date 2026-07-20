package com.fleettrackpro;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.NotFoundException;
import java.util.List;

@Path("/catalogos")
@Produces(MediaType.APPLICATION_JSON)
public class CatalogoResource {

    @GET
    @Path("/planes-saas")
    public List<EmpresaSaasPlan> planesSaas() {
        return EmpresaSaasPlan.listAll();
    }

    @GET
    @Path("/roles-saas")
    public List<RolSaas> rolesSaas() {
        return RolSaas.listAll();
    }

    @GET
    @Path("/estados-suscripcion")
    public List<EmpresaSuscripcionEstado> estadosSuscripcion() {
        return EmpresaSuscripcionEstado.listAll();
    }

    @GET
    @Path("/ubigeo-departamentos")
    public List<UbigeoDepartamento> ubigeoDepartamentos() {
        return UbigeoDepartamento.listAll();
    }

    @GET
    @Path("/ubigeo-provincias")
    public List<UbigeoProvincia> ubigeoProvincias() {
        return UbigeoProvincia.listAll();
    }

    @GET
    @Path("/ubigeo-distritos")
    public List<UbigeoDistrito> ubigeoDistritos() {
        return UbigeoDistrito.listAll();
    }

    @GET
    @Path("/marcas")
    public List<VehiculoMarca> marcas() {
        return VehiculoMarca.listAll();
    }

    @GET
    @Path("/modelos")
    public List<VehiculoModelo> modelos() {
        return VehiculoModelo.listAll();
    }

    @GET
    @Path("/colores")
    public List<VehiculoColor> colores() {
        return VehiculoColor.listAll();
    }

    @GET
    @Path("/tipos-unidad")
    public List<VehiculoTipoUnidad> tiposUnidad() {
        return VehiculoTipoUnidad.listAll();
    }

    @GET
    @Path("/propiedad-tipos")
    public List<VehiculoPropiedadTipo> propiedadTipos() {
        return VehiculoPropiedadTipo.listAll();
    }

    @GET
    @Path("/estados-operativos")
    public List<VehiculoEstadoOperativo> estadosOperativos() {
        return VehiculoEstadoOperativo.listAll();
    }

    @GET
    @Path("/licencia-categorias")
    public List<ConductorLicenciaCategoria> licenciaCategorias() {
        return ConductorLicenciaCategoria.listAll();
    }

    @GET
    @Path("/tipos-sangre")
    public List<ConductorTipoSangre> tiposSangre() {
        return ConductorTipoSangre.listAll();
    }

    @GET
    @Path("/estados-laborales")
    public List<ConductorEstadoLaboral> estadosLaborales() {
        return ConductorEstadoLaboral.listAll();
    }

    @GET
    @Path("/prioridades")
    public List<OperacionPrioridad> prioridades() {
        return OperacionPrioridad.listAll();
    }

    @GET
    @Path("/viaje-estados")
    public List<OperacionViajeEstado> viajeEstados() {
        return OperacionViajeEstado.listAll();
    }

    @GET
    @Path("/tipos-servicio")
    public List<MantenimientoTipoServicio> tiposServicio() {
        return MantenimientoTipoServicio.listAll();
    }

    @GET
    @Path("/repuestos")
    public List<RepuestoCatalogo> repuestos() {
        return RepuestoCatalogo.listAll();
    }

    @GET
    @Path("/talleres")
    public List<TallerCatalogo> talleres() {
        return TallerCatalogo.listAll();
    }

    @GET
    @Path("/monedas")
    public List<CostoMoneda> monedas() {
        return CostoMoneda.listAll();
    }

    @GET
    @Path("/gasto-categorias")
    public List<CostoGastoCategoria> gastoCategorias() {
        return CostoGastoCategoria.listAll();
    }

    @GET
    @Path("/comprobante-tipos")
    public List<CostoComprobanteTipo> comprobanteTipos() {
        return CostoComprobanteTipo.listAll();
    }

    @GET
    @Path("/departamentos")
    public List<UbigeoDepartamento> departamentos() {
        return UbigeoDepartamento.listAll();
    }

    @GET
    @Path("/provincias")
    public List<UbigeoProvincia> provincias() {
        return UbigeoProvincia.listAll();
    }

    @GET
    @Path("/distritos")
    public List<UbigeoDistrito> distritos() {
        return UbigeoDistrito.listAll();
    }

    @GET
    @Path("/tipos-documento")
    @Produces(MediaType.APPLICATION_JSON)
    public List<TipoDocumento> tiposDocumento() {
        return TipoDocumento.listAll();
    }

    @POST
    @Path("/repuestos")
    @Transactional
    @Consumes(MediaType.APPLICATION_JSON)
    public RepuestoCatalogo crearRepuesto(RepuestoCatalogo nuevo) {
        if (nuevo.codigoRepuesto == null || nuevo.codigoRepuesto.trim().isEmpty() ||
                nuevo.nombreRepuesto == null || nuevo.nombreRepuesto.trim().isEmpty()) {
            throw new jakarta.ws.rs.WebApplicationException(
                    jakarta.ws.rs.core.Response.status(jakarta.ws.rs.core.Response.Status.BAD_REQUEST)
                            .entity(java.util.Map.of("message", "Código y nombre de repuesto son obligatorios"))
                            .type(MediaType.APPLICATION_JSON)
                            .build());
        }
        nuevo.persist();
        return nuevo;
    }

    @POST
    @Path("/talleres")
    @Transactional
    @Consumes(MediaType.APPLICATION_JSON)
    public TallerCatalogo crearTaller(TallerCatalogo nuevo) {
        if (nuevo.nombreTaller == null || nuevo.nombreTaller.trim().isEmpty()) {
            throw new jakarta.ws.rs.WebApplicationException(
                    jakarta.ws.rs.core.Response.status(jakarta.ws.rs.core.Response.Status.BAD_REQUEST)
                            .entity(java.util.Map.of("message", "Nombre de taller es obligatorio"))
                            .type(MediaType.APPLICATION_JSON)
                            .build());
        }
        if (nuevo.telefono != null && !nuevo.telefono.trim().isEmpty()) {
            String tel = nuevo.telefono.trim();
            if (!tel.matches("^[0-9]{9}$")) {
                throw new jakarta.ws.rs.WebApplicationException(
                        jakarta.ws.rs.core.Response.status(jakarta.ws.rs.core.Response.Status.BAD_REQUEST)
                                .entity(java.util.Map.of("message",
                                        "El teléfono del taller debe tener exactamente 9 dígitos numéricos"))
                                .type(MediaType.APPLICATION_JSON)
                                .build());
            }
        }
        nuevo.persist();
        return nuevo;
    }

    @GET
    @Path("/repuestos/{id}")
    public RepuestoCatalogo obtenerRepuesto(@PathParam("id") Integer id) {
        RepuestoCatalogo repuesto = RepuestoCatalogo.findById(id);
        if (repuesto == null) {
            throw new NotFoundException("Repuesto no encontrado en catálogo");
        }
        return repuesto;
    }

    @jakarta.ws.rs.PUT
    @Path("/repuestos/{id}")
    @Transactional
    @Consumes(MediaType.APPLICATION_JSON)
    public RepuestoCatalogo actualizarRepuesto(@PathParam("id") Integer id, RepuestoCatalogo actualizado) {
        RepuestoCatalogo repuesto = RepuestoCatalogo.findById(id);
        if (repuesto == null) {
            throw new NotFoundException("Repuesto no encontrado en catálogo");
        }
        if (actualizado.codigoRepuesto == null || actualizado.codigoRepuesto.trim().isEmpty() ||
                actualizado.nombreRepuesto == null || actualizado.nombreRepuesto.trim().isEmpty()) {
            throw new jakarta.ws.rs.WebApplicationException(
                    jakarta.ws.rs.core.Response.status(jakarta.ws.rs.core.Response.Status.BAD_REQUEST)
                            .entity(java.util.Map.of("message", "Código y nombre de repuesto son obligatorios"))
                            .type(MediaType.APPLICATION_JSON)
                            .build());
        }
        repuesto.codigoRepuesto = actualizado.codigoRepuesto;
        repuesto.nombreRepuesto = actualizado.nombreRepuesto;
        repuesto.costoReferencial = actualizado.costoReferencial;
        return repuesto;
    }

    @GET
    @Path("/talleres/{id}")
    public TallerCatalogo obtenerTaller(@PathParam("id") Integer id) {
        TallerCatalogo taller = TallerCatalogo.findById(id);
        if (taller == null) {
            throw new NotFoundException("Taller no encontrado en catálogo");
        }
        return taller;
    }

    @jakarta.ws.rs.PUT
    @Path("/talleres/{id}")
    @Transactional
    @Consumes(MediaType.APPLICATION_JSON)
    public TallerCatalogo actualizarTaller(@PathParam("id") Integer id, TallerCatalogo actualizado) {
        TallerCatalogo taller = TallerCatalogo.findById(id);
        if (taller == null) {
            throw new NotFoundException("Taller no encontrado en catálogo");
        }
        if (actualizado.nombreTaller == null || actualizado.nombreTaller.trim().isEmpty()) {
            throw new jakarta.ws.rs.WebApplicationException(
                    jakarta.ws.rs.core.Response.status(jakarta.ws.rs.core.Response.Status.BAD_REQUEST)
                            .entity(java.util.Map.of("message", "Nombre de taller es obligatorio"))
                            .type(MediaType.APPLICATION_JSON)
                            .build());
        }
        if (actualizado.telefono != null && !actualizado.telefono.trim().isEmpty()) {
            String tel = actualizado.telefono.trim();
            if (!tel.matches("^[0-9]{9}$")) {
                throw new jakarta.ws.rs.WebApplicationException(
                        jakarta.ws.rs.core.Response.status(jakarta.ws.rs.core.Response.Status.BAD_REQUEST)
                                .entity(java.util.Map.of("message",
                                        "El teléfono del taller debe tener exactamente 9 dígitos numéricos"))
                                .type(MediaType.APPLICATION_JSON)
                                .build());
            }
        }
        taller.nombreTaller = actualizado.nombreTaller;
        taller.direccion = actualizado.direccion;
        taller.telefono = actualizado.telefono;
        taller.ruc = actualizado.ruc;
        return taller;
    }

    @GET
    @Path("/saas-gasto-categorias")
    public List<GastoInternoCategoria> saasGastoCategorias() {
        return GastoInternoCategoria.listAll();
    }

    @GET
    @Path("/factura-estados")
    public List<FacturaEstadoPago> facturaEstados() {
        return FacturaEstadoPago.listAll();
    }

    @GET
    @Path("/metodos-pago-saas")
    public List<MetodoPagoSaas> metodosPagoSaas() {
        return MetodoPagoSaas.listAll();
    }
}
