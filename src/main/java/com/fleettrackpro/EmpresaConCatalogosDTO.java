package com.fleettrackpro;

import java.time.LocalDateTime;

public class EmpresaConCatalogosDTO {
    public String idEmpresa;
    public String rucTaxId;
    public String razonSocial;
    public String nombreComercial;
    public String giroNegocio;
    public String descripcionCorporativa;
    public String direccionFiscal;
    public String telefonoCorporativo;
    public String emailCorporativo;
    public String sitioWeb;
    public Integer idSaasPlan;
    public String nombrePlan;
    public Integer idSuscripcionEstado;
    public String nombreEstado;
    public String monedaBase;
    public LocalDateTime fechaRegistroSaas;
    public String logoUrl;
    public String paisOperacion;

    public static EmpresaConCatalogosDTO from(
            Empresa empresa,
            EmpresaSaasPlan plan,
            EmpresaSuscripcionEstado estado) {
        EmpresaConCatalogosDTO dto = new EmpresaConCatalogosDTO();
        dto.idEmpresa = empresa.idEmpresa;
        dto.rucTaxId = empresa.rucTaxId;
        dto.razonSocial = empresa.razonSocial;
        dto.nombreComercial = empresa.nombreComercial;
        dto.giroNegocio = empresa.giroNegocio;
        dto.descripcionCorporativa = empresa.descripcionCorporativa;
        dto.direccionFiscal = empresa.direccionFiscal;
        dto.telefonoCorporativo = empresa.telefonoCorporativo;
        dto.emailCorporativo = empresa.emailCorporativo;
        dto.sitioWeb = empresa.sitioWeb;
        dto.idSaasPlan = empresa.idSaasPlan;
        dto.nombrePlan = plan == null ? null : plan.nombrePlan;
        dto.idSuscripcionEstado = empresa.idSuscripcionEstado;
        dto.nombreEstado = estado == null ? null : estado.nombreEstado;
        dto.monedaBase = empresa.monedaBase;
        dto.fechaRegistroSaas = empresa.fechaRegistroSaas;
        dto.logoUrl = empresa.logoUrl;
        dto.paisOperacion = empresa.paisOperacion;
        return dto;
    }
}
