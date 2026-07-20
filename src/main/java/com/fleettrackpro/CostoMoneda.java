package com.fleettrackpro;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "costo_moneda", schema = "costs")
public class CostoMoneda extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_moneda")
    public Integer idMoneda;

    @Column(name = "codigo_iso")
    public String codigoIso;

    public String simbolo;

    @Column(name = "nombre_divisa")
    public String nombreDivisa;
}
