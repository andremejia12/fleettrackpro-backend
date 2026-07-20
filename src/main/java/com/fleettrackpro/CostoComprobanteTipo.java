package com.fleettrackpro;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "costo_comprobante_tipo", schema = "costs")
public class CostoComprobanteTipo extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_comprobante_tipo")
    public Integer idComprobanteTipo;

    @Column(name = "codigo_sunat")
    public String codigoSunat;

    @Column(name = "nombre_comprobante")
    public String nombreComprobante;
}
