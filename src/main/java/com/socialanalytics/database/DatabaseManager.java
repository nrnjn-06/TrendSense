package com.socialanalytics.database;

import com.socialanalytics.exception.DatabaseException;
import com.socialanalytics.service.ConfigService;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Direct JDBC Database Manager implementing native java.sql connections,
 * schema initialization, and transactional control.
 * Prioritizes MySQL, with an automatic embedded database fallback if MySQL is offline.
 */
public class DatabaseManager {
    private static volatile DatabaseManager instance;
    private final ConfigService config = ConfigService.getInstance();
    private boolean usingFallback = false;
    private String activeDatabaseType = "MySQL";

    private DatabaseManager() {
        initDriver();
        initializeSchema();
    }

    public static DatabaseManager getInstance() {
        if (instance == null) {
            synchronized (DatabaseManager.class) {
                if (instance == null) {
                    instance = new DatabaseManager();
                }
            }
        }
        return instance;
    }

    private void initDriver() {
        try {
            Class.forName(config.get("db.driver", "com.mysql.cj.jdbc.Driver"));
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL driver not found, checking fallback driver: " + e.getMessage());
        }
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException ignored) {
        }
    }

    /**
     * Obtains a standard JDBC Connection.
     * Attempts MySQL first, falling back to embedded database if enabled and MySQL fails.
     */
    public Connection getConnection() throws DatabaseException {
        // If we previously detected fallback or fallback is forced
        if (usingFallback) {
            return getFallbackConnection();
        }

        String mysqlUrl = config.get("db.url");
        String user = config.get("db.username");
        String pass = config.get("db.password");

        try {
            Connection conn = DriverManager.getConnection(mysqlUrl, user, pass);
            activeDatabaseType = "MySQL";
            return conn;
        } catch (SQLException e) {
            boolean fallbackEnabled = config.getBoolean("db.fallback.enabled", true);
            if (fallbackEnabled) {
                System.out.println("[DatabaseManager] Notice: MySQL not accessible (" + e.getMessage() +
                        "). Activating embedded local database fallback.");
                usingFallback = true;
                return getFallbackConnection();
            } else {
                throw new DatabaseException("Failed to connect to MySQL database at " + mysqlUrl + ": " + e.getMessage(), e);
            }
        }
    }

    private Connection getFallbackConnection() throws DatabaseException {
        try {
            File dataDir = new File("data");
            if (!dataDir.exists()) {
                dataDir.mkdirs();
            }
            String fallbackUrl = config.get("db.fallback.url", "jdbc:h2:./data/social_analytics;MODE=MySQL;DATABASE_TO_UPPER=false;AUTO_SERVER=TRUE");
            String user = config.get("db.fallback.username", "sa");
            String pass = config.get("db.fallback.password", "");
            Connection conn = DriverManager.getConnection(fallbackUrl, user, pass);
            activeDatabaseType = "Embedded (H2 MySQL-Mode)";
            return conn;
        } catch (SQLException ex) {
            throw new DatabaseException("Failed to initialize embedded fallback database: " + ex.getMessage(), ex);
        }
    }

    /**
     * Executes the schema DDL script using JDBC Statement.
     */
    public synchronized void initializeSchema() {
        try (Connection conn = getConnection()) {
            InputStream is = getClass().getClassLoader().getResourceAsStream("schema.sql");
            if (is == null) {
                System.err.println("schema.sql not found in resources!");
                return;
            }
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (!trimmed.startsWith("--") && !trimmed.isEmpty()) {
                        sb.append(line).append("\n");
                    }
                }
            }

            String[] statements = sb.toString().split(";");
            try (Statement stmt = conn.createStatement()) {
                for (String sql : statements) {
                    String trimmed = sql.trim();
                    if (!trimmed.isEmpty()) {
                        stmt.execute(trimmed);
                    }
                }
                // Auto-migrate column sizes for CDN profile images and permalinks
                try {
                    stmt.execute("ALTER TABLE social_accounts ALTER COLUMN profile_url VARCHAR(2048)");
                } catch (Exception ignored) {}
                try {
                    stmt.execute("ALTER TABLE social_accounts ALTER COLUMN status_reason VARCHAR(2048)");
                } catch (Exception ignored) {}
                try {
                    stmt.execute("ALTER TABLE posts ALTER COLUMN permalink VARCHAR(2048)");
                } catch (Exception ignored) {}
                try {
                    stmt.execute("UPDATE posts SET shares = NULL WHERE platform = 'INSTAGRAM'");
                } catch (Exception ignored) {}
            }
            System.out.println("[DatabaseManager] Database schema verified successfully on " + activeDatabaseType);
        } catch (Exception e) {
            System.err.println("[DatabaseManager] Schema initialization note: " + e.getMessage());
        }
    }

    public boolean isUsingFallback() {
        return usingFallback;
    }

    public String getActiveDatabaseType() {
        return activeDatabaseType;
    }

    /**
     * Closes a connection safely.
     */
    public void close(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException ignored) {
            }
        }
    }
}
