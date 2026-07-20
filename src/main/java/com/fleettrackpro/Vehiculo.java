package com.fleettrackpro;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "vehiculos", schema = "fleet")
public class Vehiculo extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_vehiculo")
    public Integer idVehiculo;

    public String placa;

    @Column(name = "id_marca")
    public Integer idMarca;

    @Column(name = "id_modelo")
    public Integer idModelo;

    public Integer anio;

    @Column(name = "capacidad_carga")
    public Double capacidadCarga;

    @Column(name = "id_estado_operativo")
    public Integer idEstadoOperativo;

    @Column(name = "id_color")
    public Integer idColor;

    @Column(name = "id_tipo_unidad")
    public Integer idTipoUnidad;

    @Column(name = "id_propiedad_tipo")
    public Integer idPropiedadTipo;

    @Column(name = "nro_chasis_vin")
    public String nroChasisVin;

    @Column(name = "kilometraje_actual")
    public Integer kilometrajeActual;

    @Column(name = "soat_vencimiento")
    public LocalDate soatVencimiento;

    @Column(name = "revision_tecnica_vencimiento")
    public LocalDate revisionTecnicaVencimiento;

    @Column(name = "fecha_registro")
    public LocalDateTime fechaRegistro;

    @Column(name = "id_empresa")
    public String idEmpresa;
}