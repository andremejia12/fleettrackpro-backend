package com.fleettrackpro;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

@Entity
@Table(name = "empresa_suscripcion_estado", schema = "companies")
public class EmpresaSuscripcionEstado extends PanacheEntityBase {
    @Id
    @Column(name = "id_suscripcion_estado")
    public Integer idSuscripcionEstado;

    @Column(name = "nombre_estado")
    public String nombreEstado;

    public String descripcion;
}
