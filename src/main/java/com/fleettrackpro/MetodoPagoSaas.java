package com.fleettrackpro;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "metodo_pago", schema = "saas_admin")
public class MetodoPagoSaas extends PanacheEntityBase {

    @Id
    @Column(name = "id_metodo_pago")
    public Integer idMetodoPago;

    @Column(name = "nombre_metodo")
    public String nombreMetodo;
}