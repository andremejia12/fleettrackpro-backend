package com.fleettrackpro;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.OneToMany;
import jakarta.persistence.CascadeType;
import jakarta.persistence.FetchType;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "mantenimientos", schema = "maintenance")
public class Mantenimiento extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_mantenimiento")
    public Integer idMantenimiento;

    @Column(name = "id_vehiculo")
    public Integer idVehiculo;

    @Column(name = "id_taller")
    public Integer idTaller;

    @Column(name = "id_tipo_servicio")
    public Integer idTipoServicio;

    @Column(name = "descripcion_falla")
    public String descripcionFalla;

    @Column(name = "costo_reparacion")
    public Double costoReparacion;

    @Column(name = "fecha_entrada")
    public LocalDateTime fechaEntrada;

    @Column(name = "fecha_salida")
    public LocalDateTime fechaSalida;

    @Column(name = "taller_nombre")
    public String tallerNombre;

    @Column(name = "kilometraje_entrada")
    public Integer kilometrajeEntrada;

    @Column(name = "orden_servicio_taller")
    public String ordenServicioTaller;

    @Column(name = "garantia_meses")
    public Integer garantiaMeses;

    @Column(name = "id_empresa")
    public String idEmpresa;

    @Column(name = "id_metodo_pago")
    public Integer idMetodoPago;

    @Column(name = "id_moneda")
    public Integer idMoneda;

    @OneToMany(mappedBy = "mantenimiento", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JsonManagedReference
    public List<RepuestoMantenimientoDetalle> repuestos = new ArrayList<>();

    @PrePersist
    @PreUpdate
    public void linkRepuestos() {
        if (repuestos != null) {
            for (RepuestoMantenimientoDetalle r : repuestos) {
                r.mantenimiento = this;
            }
        }
    }
}
