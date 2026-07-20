package com.fleettrackpro;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

@Entity
@Table(name = "rol_saas", schema = "saas_admin")
public class RolSaas extends PanacheEntityBase {
    @Id
    @Column(name = "id_rol_saas")
    public Integer idRolSaas;

    @Column(name = "nombre_rol")
    public String nombreRol;

    public String descripcion;
}
