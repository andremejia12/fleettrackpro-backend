package com.fleettrackpro;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

@QuarkusTest
class FindMissingFKTest {

    @Inject
    DataSource dataSource;

    @Test
    void findMissingFKs() throws Exception {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             PrintWriter writer = new PrintWriter(new FileWriter("missing_fk_analysis.txt"))) {
            
            // 1. Get all foreign keys
            String fkSql = "SELECT " +
                           "  kcu.table_schema, " +
                           "  kcu.table_name, " +
                           "  kcu.column_name " +
                           "FROM " +
                           "  information_schema.table_constraints AS tc " +
                           "  JOIN information_schema.key_column_usage AS kcu " +
                           "    ON tc.constraint_name = kcu.constraint_name " +
                           "    AND tc.table_schema = kcu.table_schema " +
                           "WHERE tc.constraint_type = 'FOREIGN KEY' " +
                           "  AND tc.table_schema IN ('fleet', 'employees', 'operations', 'maintenance', 'costs', 'companies', 'security', 'saas_admin')";
            
            Set<String> existingFKs = new HashSet<>();
            try (ResultSet rs = stmt.executeQuery(fkSql)) {
                while (rs.next()) {
                    String schema = rs.getString("table_schema");
                    String table = rs.getString("table_name");
                    String column = rs.getString("column_name");
                    existingFKs.add(schema + "." + table + "." + column);
                }
            }
            
            // 2. Get all columns starting with id_ in our schemas
            String colSql = "SELECT table_schema, table_name, column_name, data_type " +
                            "FROM information_schema.columns " +
                            "WHERE table_schema IN ('fleet', 'employees', 'operations', 'maintenance', 'costs', 'companies', 'security', 'saas_admin') " +
                            "  AND column_name LIKE 'id_%' " +
                            "ORDER BY table_schema, table_name, column_name";
            
            writer.println("=== ANALISIS DE CLAVES FORANEAS (FK) FALTANTES EN LA BASE DE DATOS ===");
            writer.println("Este reporte lista las columnas que comienzan con 'id_' pero que NO tienen una constraint FOREIGN KEY física en PostgreSQL.\n");
            
            try (ResultSet rs = stmt.executeQuery(colSql)) {
                while (rs.next()) {
                    String schema = rs.getString("table_schema");
                    String table = rs.getString("table_name");
                    String column = rs.getString("column_name");
                    String dataType = rs.getString("data_type");
                    
                    // Simple PK heuristic: if column name is id_ + singular of table name
                    // e.g. id_vehiculo in fleet.vehiculos
                    String tableBasename = table;
                    String singular = tableBasename.endsWith("s") ? tableBasename.substring(0, tableBasename.length() - 1) : tableBasename;
                    boolean isPk = column.equals("id_" + singular) || column.equals("id_" + tableBasename);
                    
                    // Check if it exists in the set of foreign keys
                    String key = schema + "." + table + "." + column;
                    if (!isPk && !existingFKs.contains(key)) {
                        writer.printf("Tabla: %s.%s | Columna: %s (%s) -> [SIN FK FISICA]%n", schema, table, column, dataType);
                    }
                }
            }
            System.out.println("MISSING FK ANALYSIS REPORT GENERATED SUCCESSFULLY!");
        }
    }
}
