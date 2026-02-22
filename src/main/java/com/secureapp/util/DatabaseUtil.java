package com.secureapp.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Database utility class for managing H2 database connections and
 * initialization.
 * Implements secure database connection handling and table creation.
 */
public final class DatabaseUtil {
    private static final String DB_DRIVER = "org.h2.Driver";
    private static final String DB_DIR = System.getProperty("user.home") + "/secure-app-db";
    private static final String DB_URL = "jdbc:h2:" + DB_DIR + "/secure-app-db;AUTO_SERVER=TRUE";
    private static final String DB_USER = "sa";
    private static final String DB_PASSWORD = "";

    private DatabaseUtil() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    static {
        try {
            Class.forName(DB_DRIVER);
            initializeDatabase();
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError("Failed to load H2 driver: " + e.getMessage());
        }
    }

    /**
     * Get a database connection.
     * 
     * @return Connection object
     * @throws SQLException if connection fails
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    private static void initializeDatabase() {
        try (Connection conn = getConnection();
                Statement stmt = conn.createStatement()) {

            String createUsersTable = "CREATE TABLE IF NOT EXISTS users (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "email VARCHAR(255) NOT NULL UNIQUE, " +
                    "password_hash VARCHAR(255) NOT NULL, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ")";
            stmt.execute(createUsersTable);

            String createFilesTable = "CREATE TABLE IF NOT EXISTS files (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "user_id INT NOT NULL, " +
                    "original_filename VARCHAR(255) NOT NULL, " +
                    "stored_filename VARCHAR(255) NOT NULL UNIQUE, " +
                    "file_size BIGINT NOT NULL, " +
                    "upload_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE" +
                    ")";
            stmt.execute(createFilesTable);

        } catch (SQLException e) {
            throw new ExceptionInInitializerError("Failed to initialize database: " + e.getMessage());
        }
    }

    /**
     * Close a database connection safely.
     * 
     * @param conn Connection to close
     */
    public static void closeConnection(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                // Log error but don't throw exception in close method
                System.err.println("Error closing connection: " + e.getMessage());
            }
        }
    }

    /**
     * Close a PreparedStatement safely.
     * 
     * @param pstmt PreparedStatement to close
     */
    public static void closeStatement(PreparedStatement pstmt) {
        if (pstmt != null) {
            try {
                pstmt.close();
            } catch (SQLException e) {
                System.err.println("Error closing statement: " + e.getMessage());
            }
        }
    }
}
