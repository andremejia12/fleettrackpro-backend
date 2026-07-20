package com.fleettrackpro;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

@Entity
@Table(name = "factura_estado_pago", schema = "saas_admin")
public class FacturaEstadoPago extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estado_pago")
    public Integer idEstadoPago;

    @Column(name = "nombre_estado")
    public String nombreEstado;
}
