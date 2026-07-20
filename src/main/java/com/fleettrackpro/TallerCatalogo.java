package com.fleettrackpro;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "taller_catalogo", schema = "maintenance")
public class TallerCatalogo extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_taller")
    public Integer idTaller;

    @Column(name = "nombre_taller")
    public String nombreTaller;

    public String direccion;
    public String telefono;
    public String ruc;
}
