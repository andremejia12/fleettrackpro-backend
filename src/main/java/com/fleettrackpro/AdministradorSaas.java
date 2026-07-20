package com.fleettrackpro;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "administradores_saas", schema = "saas_admin")
public class AdministradorSaas extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_admin")
    public Integer idAdmin;

    public String nombre;
    public String email;
    public String contrasenia;

    @Column(name = "id_rol_saas")
    public Integer idRolSaas;

    @Column(name = "estado_cuenta")
    public String estadoCuenta;

    @Column(name = "fecha_creacion")
    public LocalDateTime fechaCreacion;

    @Column(name = "ultimo_acceso")
    public LocalDateTime ultimoAcceso;
}
