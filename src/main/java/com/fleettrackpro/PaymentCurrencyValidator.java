package com.fleettrackpro;

import jakarta.ws.rs.BadRequestException;
import java.util.Locale;

public final class PaymentCurrencyValidator {
    private PaymentCurrencyValidator() {
    }

    public static void validate(Integer idMetodoPago, Integer idMoneda) {
        MetodoPagoSaas metodo = MetodoPagoSaas.findById(idMetodoPago);
        CostoMoneda moneda = CostoMoneda.findById(idMoneda);
        if (metodo == null) throw new BadRequestException("Método de pago válido es requerido");
        if (moneda == null) throw new BadRequestException("Moneda válida es requerida");

        String metodoNombre = metodo.nombreMetodo == null ? "" : metodo.nombreMetodo.toLowerCase(Locale.ROOT);
        String codigo = moneda.codigoIso == null ? "" : moneda.codigoIso.toUpperCase(Locale.ROOT);
        if (metodoNombre.contains("plin") && !"PEN".equals(codigo)) {
            throw new BadRequestException("Plin solo admite operaciones en soles (PEN)");
        }
        if (!"PEN".equals(codigo) && !"USD".equals(codigo)) {
            throw new BadRequestException("Solo se admiten operaciones en PEN o USD");
        }
    }
}
