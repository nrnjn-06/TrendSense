package com.socialanalytics.database;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility to print all database tables, columns, and row counts directly
 * to the console.
 */
public class DbSummaryViewer {
    public static void main(String[] args) {
        DatabaseManager db = DatabaseManager.getInstance();
        System.out.println("=================================================================");
        System.out.println("📊 Active Database: " + db.getActiveDatabaseType());
        System.out.println("=================================================================");

        try (Connection conn = db.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            List<String> tables = new ArrayList<>();
            try (ResultSet rs = meta.getTables(null, "PUBLIC", "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    String tableName = rs.getString("TABLE_NAME");
                    tables.add(tableName);
                }
            }

            System.out.printf("%-25s | %-10s%n", "TABLE NAME", "ROW COUNT");
            System.out.println("-------------------------------------------");
            try (Statement stmt = conn.createStatement()) {
                for (String t : tables) {
                    try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM \"" + t + "\"")) {
                        if (rs.next()) {
                            long count = rs.getLong(1);
                            System.out.printf("%-25s | %-10d%n", t, count);
                        }
                    } catch (Exception ex) {
                        System.out.printf("%-25s | Error: %s%n", t, ex.getMessage());
                    }
                }
            }
            System.out.println("=================================================================");
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.exit(0);
    }
}
