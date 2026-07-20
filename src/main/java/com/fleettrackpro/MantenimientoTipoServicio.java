package com.fleettrackpro;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "mantenimiento_tipo_servicio", schema = "maintenance")
public class MantenimientoTipoServicio extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tipo_servicio")
    public Integer idTipoServicio;

    @Column(name = "nombre_servicio")
    public String nombreServicio;

    public String descripcion;
}
