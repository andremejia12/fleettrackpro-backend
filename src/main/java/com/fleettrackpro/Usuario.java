package com.fleettrackpro;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "usuario", schema = "security")
public class Usuario extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_user")
    public Integer idUser;

    public String nombre;
    public String contrasenia;
    public String dni;
    public String telefono;
    public String email;

    @Column(name = "fecha_nacimiento")
    public LocalDate fechaNacimiento;

    @Column(name = "id_rol")
    public Integer idRol;

    @Column(name = "id_usuario_estado")
    public Integer idUsuarioEstado;

    @Column(name = "id_empresa")
    public String idEmpresa;
}