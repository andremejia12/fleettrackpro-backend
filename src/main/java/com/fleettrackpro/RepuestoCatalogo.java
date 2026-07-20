package com.fleettrackpro;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "repuesto_catalogo", schema = "maintenance")
public class RepuestoCatalogo extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_repuesto")
    public Integer idRepuesto;

    @Column(name = "codigo_repuesto")
    public String codigoRepuesto;

    @Column(name = "nombre_repuesto")
    public String nombreRepuesto;

    @Column(name = "costo_referencial")
    public Double costoReferencial;
}
