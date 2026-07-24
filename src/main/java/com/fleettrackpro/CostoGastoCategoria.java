package com.fleettrackpro;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "costo_gasto_categoria", schema = "costs")
public class CostoGastoCategoria extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_gasto_categoria")
    public Integer idGastoCategoria;

    @Column(name = "nombre_categoria")
    public String nombreCategoria;

    public String descripcion;

    @Column(name = "es_prorrateable")
    public Boolean esProrrateable;

    @Column(name = "tipo_costo")
    public String tipoCosto;
}
