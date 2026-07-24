package com.fleettrackpro;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "sucursales_garitas", schema = "fleet")
public class SucursalGarita extends PanacheEntityBase {
    @Id
    @Column(name = "id_sucursal")
    public String idSucursal;

    @Column(name = "id_empresa")
    public String idEmpresa;

    @Column(name = "nombre_sucursal")
    public String nombreSucursal;

    public String direccion;

    @Column(name = "id_distrito")
    public String idDistrito;

    @Column(name = "es_taller_interno")
    public String esTallerInterno;
}
