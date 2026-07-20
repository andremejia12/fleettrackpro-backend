package com.fleettrackpro;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "vehiculo_estado_operativo", schema = "fleet")
public class VehiculoEstadoOperativo extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estado_operativo")
    public Integer idEstadoOperativo;

    @Column(name = "nombre_estado")
    public String nombreEstado;

    public String descripcion;
}
