package com.fleettrackpro;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "ubigeo_provincia", schema = "companies")
public class UbigeoProvincia extends PanacheEntityBase {

    @Id
    @Column(name = "id_provincia")
    public String idProvincia;

    @Column(name = "id_departamento")
    public String idDepartamento;

    @Column(name = "nombre_provincia")
    public String nombreProvincia;
}
