package com.fleettrackpro;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "gastos_internos_saas", schema = "saas_admin")
public class GastoInternoSaas extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_gasto_interno")
    public Integer idGastoInterno;

    public String concepto;

    @Column(name = "id_categoria")
    public Integer idCategoria;

    public BigDecimal monto;
    public String moneda;

    @Column(name = "fecha_gasto")
    public LocalDate fechaGasto;

    @Column(name = "comprobante_nro")
    public String comprobanteNro;

    @Column(name = "id_metodo_pago", nullable = false)
    public Integer idMetodoPago;

    public String descripcion;

    @Column(name = "fecha_registro")
    public LocalDateTime fechaRegistro;
}
