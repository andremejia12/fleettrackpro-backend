package com.fleettrackpro;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "conductor_estado_laboral", schema = "employees")
public class ConductorEstadoLaboral extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estado_laboral")
    public Integer idEstadoLaboral;

    @Column(name = "nombre_estado")
    public String nombreEstado;

    public String descripcion;
}
