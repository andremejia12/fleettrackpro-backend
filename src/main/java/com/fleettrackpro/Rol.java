package com.fleettrackpro;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

@Entity
@Table(name = "rol", schema = "security")
public class Rol extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_rol")
    public Integer idRol;

    @Column(name = "nombre_rol")
    public String nombreRol;

    public String descripcion;
}