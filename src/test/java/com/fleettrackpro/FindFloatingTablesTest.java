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
class FindFloatingTablesTest {

    @Inject
    DataSource dataSource;

    @Test
    void findFloatingTables() throws Exception {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             PrintWriter writer = new PrintWriter(new FileWriter("floating_tables_report.txt"))) {
            
            // 1. Get all tables in our schemas
            String tablesSql = "SELECT table_schema, table_name " +
                               "FROM information_schema.tables " +
                               "WHERE table_schema IN ('fleet', 'employees', 'operations', 'maintenance', 'costs', 'companies', 'security', 'saas_admin') " +
                               "  AND table_type = 'BASE TABLE'";
            
            Set<String> allTables = new HashSet<>();
            try (ResultSet rs = stmt.executeQuery(tablesSql)) {
                while (rs.next()) {
                    allTables.add(rs.getString("table_schema") + "." + rs.getString("table_name"));
                }
            }
            
            // 2. Get all tables involved in a foreign key relation (as source or target)
            // Fix: Join ccu on constraint_schema instead of table_schema to support cross-schema relations
            String fkSql = "SELECT " +
                           "  kcu.table_schema AS source_schema, " +
                           "  kcu.table_name AS source_table, " +
                           "  ccu.table_schema AS target_schema, " +
                           "  ccu.table_name AS target_table " +
                           "FROM " +
                           "  information_schema.table_constraints AS tc " +
                           "  JOIN information_schema.key_column_usage AS kcu " +
                           "    ON tc.constraint_name = kcu.constraint_name " +
                           "    AND tc.table_schema = kcu.table_schema " +
                           "  JOIN information_schema.constraint_column_usage AS ccu " +
                           "    ON ccu.constraint_name = tc.constraint_name " +
                           "    AND ccu.constraint_schema = tc.table_schema " +
                           "WHERE tc.constraint_type = 'FOREIGN KEY' " +
                           "  AND tc.table_schema IN ('fleet', 'employees', 'operations', 'maintenance', 'costs', 'companies', 'security', 'saas_admin')";
            
            Set<String> relatedTables = new HashSet<>();
            try (ResultSet rs = stmt.executeQuery(fkSql)) {
                while (rs.next()) {
                    String source = rs.getString("source_schema") + "." + rs.getString("source_table");
                    String target = rs.getString("target_schema") + "." + rs.getString("target_table");
                    relatedTables.add(source);
                    relatedTables.add(target);
                }
            }
            
            // 3. Find floating tables
            writer.println("=== REPORTE DE TABLAS HUERFANAS / SIN RELACION FISICA ===");
            writer.println("Estas tablas no tienen ninguna constraint de FOREIGN KEY de entrada ni de salida en PostgreSQL.\n");
            
            int count = 0;
            for (String table : allTables) {
                if (!relatedTables.contains(table)) {
                    writer.println("- " + table);
                    count++;
                }
            }
            
            writer.println("\nTotal de tablas huerfanas encontradas: " + count);
            System.out.println("FLOATING TABLES REPORT GENERATED SUCCESSFULLY WITH FIX!");
        }
    }
}
