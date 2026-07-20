package com.fleettrackpro;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "vehiculo_modelo", schema = "fleet")
public class VehiculoModelo extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_modelo")
    public Integer idModelo;

    @Column(name = "id_marca")
    public Integer idMarca;

    @Column(name = "nombre_modelo")
    public String nombreModelo;
}
