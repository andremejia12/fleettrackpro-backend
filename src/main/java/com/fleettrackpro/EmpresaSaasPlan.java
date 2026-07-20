package com.fleettrackpro;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "empresa_saas_plan", schema = "companies")
public class EmpresaSaasPlan extends PanacheEntityBase {
    @Id
    @Column(name = "id_saas_plan")
    public Integer idSaasPlan;

    @Column(name = "nombre_plan")
    public String nombrePlan;

    @Column(name = "limite_vehiculos")
    public Integer limiteVehiculos;

    @Column(name = "precio_mensual")
    public BigDecimal precioMensual;
}
