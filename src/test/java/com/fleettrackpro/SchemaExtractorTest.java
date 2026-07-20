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

@QuarkusTest
class SchemaExtractorTest {

    @Inject
    DataSource dataSource;

    @Test
    void extractSchema() throws Exception {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             PrintWriter writer = new PrintWriter(new FileWriter("database_schema_dump.txt"))) {
            
            String sql = "SELECT table_schema, table_name, column_name, data_type, is_nullable, column_default " +
                         "FROM information_schema.columns " +
                         "WHERE table_schema IN ('fleet', 'employees', 'operations', 'maintenance', 'costs', 'companies', 'security', 'saas_admin') " +
                         "ORDER BY table_schema, table_name, ordinal_position";
            
            try (ResultSet rs = stmt.executeQuery(sql)) {
                writer.println("schema | table_name | column_name | data_type | is_nullable | column_default");
                writer.println("-----------------------------------------------------------------------------");
                while (rs.next()) {
                    writer.printf("%s | %s | %s | %s | %s | %s%n",
                            rs.getString("table_schema"),
                            rs.getString("table_name"),
                            rs.getString("column_name"),
                            rs.getString("data_type"),
                            rs.getString("is_nullable"),
                            rs.getString("column_default")
                    );
                }
            }
            System.out.println("SCHEMA DUMP COMPLETED SUCCESSFULLY!");
        }
    }
}
