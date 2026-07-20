package com.fleettrackpro;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "ordenes_trabajo", schema = "operations")
public class OrdenTrabajo extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_orden_trabajo")
    public Integer idOrdenTrabajo;

    @Column(name = "id_vehiculo")
    public Integer idVehiculo;

    @Column(name = "id_conductor")
    public Integer idConductor;

    @Column(name = "tipo_incidencia")
    public String tipoIncidencia;

    public String descripcion;
    public String direccion;

    @Column(name = "id_prioridad")
    public Integer idPrioridad;

    @Column(name = "id_viaje_estado")
    public Integer idViajeEstado;

    @Column(name = "fecha_registro")
    public LocalDateTime fechaRegistro;

    @Column(name = "id_empresa")
    public String idEmpresa;
}
