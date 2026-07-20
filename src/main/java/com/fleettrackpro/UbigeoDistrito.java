package com.fleettrackpro;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "ubigeo_distrito", schema = "companies")
public class UbigeoDistrito extends PanacheEntityBase {

    @Id
    @Column(name = "id_distrito")
    public String idDistrito;

    @Column(name = "id_provincia")
    public String idProvincia;

    @Column(name = "nombre_distrito")
    public String nombreDistrito;
}
