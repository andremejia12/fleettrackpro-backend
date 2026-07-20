package com.fleettrackpro;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "ingresos_servicios", schema = "costs")
public class IngresoServicio extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_ingreso")
    public Integer idIngreso;

    @Column(name = "id_viaje")
    public Integer idViaje;

    @Column(name = "id_vehiculo")
    public Integer idVehiculo;

    @Column(name = "monto_cobrado")
    public Double montoCobrado;

    @Column(name = "costo_mano_obra_asociado")
    public Double costoManoObraAsociado;

    @Column(name = "id_moneda")
    public Integer idMoneda;

    @Column(name = "fecha_pago")
    public LocalDateTime fechaPago;

    @Column(name = "id_viaje_estado")
    public Integer idViajeEstado;

    @Column(name = "id_empresa")
    public String idEmpresa;

    @Column(name = "id_metodo_pago", nullable = false)
    public Integer idMetodoPago;
}
