package com.fleettrackpro;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "conductores", schema = "employees")
public class Conductor extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_conductor")
    public Integer idConductor;

    @Column(name = "numero_documento")
    public String numeroDocumento;

    @Column(name = "id_tipo_documento")
    public Integer idTipoDocumento;
    public String nombre;
    public String apellido;

    @Column(name = "licencia_nro")
    public String licenciaNro;

    public String telefono;
    public String email;
    public String puesto;

    @Column(name = "id_estado_laboral")
    public Integer idEstadoLaboral;

    @Column(name = "id_categoria")
    public Integer idCategoria;

    @Column(name = "licencia_vencimiento")
    public LocalDate licenciaVencimiento;

    @Column(name = "id_tipo_sangre")
    public Integer idTipoSangre;

    @Column(name = "contacto_emergencia")
    public String contactoEmergencia;

    @Column(name = "costo_hora")
    public Double costoHora;

    @Column(name = "id_empresa")
    public String idEmpresa;
}
