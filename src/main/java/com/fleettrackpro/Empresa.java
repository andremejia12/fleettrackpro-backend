package com.fleettrackpro;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "empresas", schema = "companies")
public class Empresa extends PanacheEntityBase {
    @Id
    @Column(name = "id_empresa")
    public String idEmpresa;

    @Column(name = "ruc_tax_id")
    public String rucTaxId;

    @Column(name = "razon_social")
    public String razonSocial;

    @Column(name = "nombre_comercial")
    public String nombreComercial;

    @Column(name = "giro_negocio")
    public String giroNegocio;

    @Column(name = "descripcion_corporativa")
    public String descripcionCorporativa;

    @Column(name = "direccion_fiscal")
    public String direccionFiscal;

    @Column(name = "telefono_corporativo")
    public String telefonoCorporativo;

    @Column(name = "email_corporativo")
    public String emailCorporativo;

    @Column(name = "sitio_web")
    public String sitioWeb;

    @Column(name = "estado_suscripcion")
    public String estadoSuscripcion;

    @Column(name = "plan_suscripcion")
    public String planSuscripcion;

    @Column(name = "moneda_base")
    public String monedaBase;

    @Column(name = "fecha_registro_saas")
    public LocalDateTime fechaRegistroSaas;

    @Column(name = "logo_url")
    public String logoUrl;

    @Column(name = "pais_operacion")
    public String paisOperacion;
}