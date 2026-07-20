package com.fleettrackpro;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.FetchType;
import com.fasterxml.jackson.annotation.JsonBackReference;
import java.util.UUID;

@Entity
@Table(name = "repuestos_mantenimiento_detalle", schema = "maintenance")
public class RepuestoMantenimientoDetalle extends PanacheEntityBase {

    @Id
    @Column(name = "id_detalle_repuesto")
    public String idDetalleRepuesto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_mantenimiento")
    @JsonBackReference
    public Mantenimiento mantenimiento;

    @Column(name = "codigo_repuesto")
    public String codigoRepuesto;

    @Column(name = "nombre_repuesto")
    public String nombreRepuesto;

    @Column(name = "cantidad_utilizada")
    public Integer cantidadUtilizada;

    @Column(name = "costo_unitario")
    public Double costoUnitario;

    @Column(name = "costo_total_repuesto")
    public Double costoTotalRepuesto;

    @Column(name = "mecanico_asignado")
    public String mecanicoAsignado;

    @PrePersist
    public void generateId() {
        if (idDetalleRepuesto == null || idDetalleRepuesto.trim().isEmpty()) {
            idDetalleRepuesto = UUID.randomUUID().toString();
        }
    }
}
