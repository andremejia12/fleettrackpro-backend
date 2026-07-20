package com.fleettrackpro;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "facturas_saas", schema = "saas_admin")
public class FacturaSaas extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_factura")
    public Integer idFactura;

    @Column(name = "id_empresa")
    public String idEmpresa;

    @Column(name = "numero_factura")
    public String numeroFactura;

    @Column(name = "periodo_facturado")
    public String periodoFacturado;

    public BigDecimal monto;
    public String moneda;

    @Column(name = "fecha_emision")
    public LocalDate fechaEmision;

    @Column(name = "fecha_vencimiento")
    public LocalDate fechaVencimiento;

    @Column(name = "id_estado_pago")
    public Integer idEstadoPago;

    @Column(name = "fecha_pago")
    public LocalDate fechaPago;

    @Column(name = "id_metodo_pago")
    public Integer idMetodoPago;

    @Column(name = "referencia_pago")
    public String referenciaPago;
}
