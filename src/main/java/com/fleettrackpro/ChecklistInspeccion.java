package com.fleettrackpro;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "checklist_inspeccion_viaje", schema = "operations")
public class ChecklistInspeccion extends PanacheEntityBase {
    @Id
    @Column(name = "id_checklist")
    public String idChecklist;

    @Column(name = "id_viaje")
    public Integer idViaje;

    @Column(name = "nivel_combustible_proporcion")
    public String nivelCombustibleProporcion;

    @Column(name = "estado_neumaticos")
    public String estadoNeumaticos;

    @Column(name = "tiene_herramientas_emergencia")
    public String tieneHerramientasEmergencia;

    @Column(name = "luces_operativas")
    public String lucesOperativas;

    @Column(name = "observaciones_carroceria")
    public String observacionesCarroceria;

    @Column(name = "firma_digital_conductor_url")
    public String firmaDigitalConductorUrl;

    @Column(name = "fecha_inspeccion")
    public LocalDateTime fechaInspeccion;
}