package com.fleettrackpro;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;

@QuarkusTest
class DataInspectorTest {

    @Inject
    DataSource dataSource;

    @Test
    void inspectData() throws Exception {
        String[] tables = {
            "fleet.vehiculos",
            "employees.conductores",
            "operations.viajes",
            "maintenance.mantenimientos",
            "maintenance.repuestos_mantenimiento_detalle",
            "operations.ordenes_trabajo",
            "costs.gastos",
            "costs.ingresos_servicios",
            "fleet.vehiculo_marca",
            "fleet.vehiculo_modelo"
        };

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            for (String table : tables) {
                System.out.println("=== TABLE: " + table + " ===");
                try (ResultSet rs = stmt.executeQuery("SELECT * FROM " + table + " LIMIT 3")) {
                    ResultSetMetaData md = rs.getMetaData();
                    int columns = md.getColumnCount();
                    boolean hasRows = false;
                    while (rs.next()) {
                        hasRows = true;
                        for (int i = 1; i <= columns; i++) {
                            System.out.print(md.getColumnName(i) + "=" + rs.getObject(i) + " | ");
                        }
                        System.out.println();
                    }
                    if (!hasRows) {
                        System.out.println("(Empty table)");
                    }
                } catch (Exception e) {
                    System.out.println("Error reading " + table + ": " + e.getMessage());
                }
                System.out.println();
            }
        }
    }
}
