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
@Table(name = "viajes", schema = "operations")
public class Viaje extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_viaje")
    public Integer idViaje;

    @Column(name = "id_vehiculo")
    public Integer idVehiculo;

    @Column(name = "id_conductor")
    public Integer idConductor;

    @Column(name = "id_orden_trabajo")
    public Integer idOrdenTrabajo;

    public String origen;

    @Column(name = "id_sucursal_origen")
    public String idSucursalOrigen;

    public String destino;

    @Column(name = "fecha_salida")
    public LocalDateTime fechaSalida;

    @Column(name = "fecha_llegada")
    public LocalDateTime fechaLlegada;

    @Column(name = "fecha_llegada_estimada")
    public LocalDateTime fechaLlegadaEstimada;

    @Column(name = "id_viaje_estado")
    public Integer idViajeEstado;

    @Column(name = "orden_trabajo_nro")
    public String ordenTrabajoNro;

    @Column(name = "kilometraje_salida")
    public Integer kilometrajeSalida;

    @Column(name = "kilometraje_llegada")
    public Integer kilometrajeLlegada;

    @Column(name = "volumen_atendido_m3")
    public Double volumenAtendidoM3;

    @Column(name = "id_empresa")
    public String idEmpresa;
}
