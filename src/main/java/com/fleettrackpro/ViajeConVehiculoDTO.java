package com.fleettrackpro;

import java.time.LocalDateTime;

public class ViajeConVehiculoDTO {
    public Integer idViaje;
    public Integer idVehiculo;
    public Integer idConductor;
    public Integer idOrdenTrabajo;
    public String origen;
    public String idSucursalOrigen;
    public String nombreSucursalOrigen;
    public String destino;
    public LocalDateTime fechaSalida;
    public LocalDateTime fechaLlegada;
    public LocalDateTime fechaLlegadaEstimada;
    public Integer idViajeEstado;
    public String estadoViaje;
    public Integer idEstadoSiguiente;
    public String nombreEstadoSiguiente;
    public Integer idEstadoCancelado;
    public String ordenTrabajoNro;
    public Integer kilometrajeSalida;
    public Integer kilometrajeLlegada;
    public Double volumenAtendidoM3;
    public String idEmpresa;
    public String placaVehiculo;
    public String marcaVehiculo;
    public String modeloVehiculo;

    public static ViajeConVehiculoDTO from(
            Viaje viaje,
            String placaVehiculo,
            String marcaVehiculo,
            String modeloVehiculo) {
        ViajeConVehiculoDTO dto = new ViajeConVehiculoDTO();
        dto.idViaje = viaje.idViaje;
        dto.idVehiculo = viaje.idVehiculo;
        dto.idConductor = viaje.idConductor;
        dto.idOrdenTrabajo = viaje.idOrdenTrabajo;
        dto.origen = viaje.origen;
        dto.idSucursalOrigen = viaje.idSucursalOrigen;
        dto.destino = viaje.destino;
        dto.fechaSalida = viaje.fechaSalida;
        dto.fechaLlegada = viaje.fechaLlegada;
        dto.fechaLlegadaEstimada = viaje.fechaLlegadaEstimada;
        dto.idViajeEstado = viaje.idViajeEstado;
        dto.ordenTrabajoNro = viaje.ordenTrabajoNro;
        dto.kilometrajeSalida = viaje.kilometrajeSalida;
        dto.kilometrajeLlegada = viaje.kilometrajeLlegada;
        dto.volumenAtendidoM3 = viaje.volumenAtendidoM3;
        dto.idEmpresa = viaje.idEmpresa;
        dto.placaVehiculo = placaVehiculo;
        dto.marcaVehiculo = marcaVehiculo;
        dto.modeloVehiculo = modeloVehiculo;
        return dto;
    }
}
