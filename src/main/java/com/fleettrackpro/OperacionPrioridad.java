package com.fleettrackpro;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "operacion_prioridad", schema = "operations")
public class OperacionPrioridad extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_prioridad")
    public Integer idPrioridad;

    @Column(name = "nombre_prioridad")
    public String nombrePrioridad;

    @Column(name = "color_hex")
    public String colorHex;
}
