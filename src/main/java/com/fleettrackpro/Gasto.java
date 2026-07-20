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
@Table(name = "gastos", schema = "costs")
public class Gasto extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_gasto")
    public Integer idGasto;

    @Column(name = "id_vehiculo")
    public Integer idVehiculo;

    @Column(name = "id_viaje")
    public Integer idViaje;

    @Column(name = "id_gasto_categoria")
    public Integer idGastoCategoria;

    public java.math.BigDecimal monto;

    @Column(name = "id_moneda")
    public Integer idMoneda;

    @Column(name = "fecha_gasto")
    public LocalDateTime fechaGasto;

    public String descripcion;

    @Column(name = "id_comprobante_tipo")
    public Integer idComprobanteTipo;

    @Column(name = "comprobante_nro")
    public String comprobanteNro;

    @Column(name = "kilometraje_registro")
    public Integer kilometrajeRegistro;

    @Column(name = "id_empresa")
    public String idEmpresa;

    @Column(name = "id_metodo_pago", nullable = false)
    public Integer idMetodoPago;
}
