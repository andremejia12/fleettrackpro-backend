package com.fleettrackpro;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

@Entity
@Table(name = "gasto_interno_categoria", schema = "saas_admin")
public class GastoInternoCategoria extends PanacheEntityBase {
    @Id
    @Column(name = "id_categoria")
    public Integer idCategoria;

    @Column(name = "nombre_categoria")
    public String nombreCategoria;
}
