package com.loginbruteforce.db;

import com.loginbruteforce.Launcher;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Manages the SQLite database connection and schema initialization.
 *
 * <p>Responsible for providing database connections and ensuring all
 * required tables exist on application startup.</p>
 */
public final class DatabaseHelper {

    private static final String DB_URL = "jdbc:sqlite:" + Launcher.APP_DATA_DIR + File.separator + "app1.db";

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("[FATAL] SQLite JDBC driver not found on classpath.");
            e.printStackTrace();
        }
    }

    private DatabaseHelper() {
        // Utility class — prevent instantiation
    }

    /**
     * Returns a new connection to the SQLite database.
     *
     * @return an active {@link Connection}
     * @throws SQLException if a connection cannot be established
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    /**
     * Creates all required tables if they do not already exist.
     * Must be called once at application startup before any other database operations.
     */
    public static void initializeDatabase() {
        String usersTable = "CREATE TABLE IF NOT EXISTS users ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "email TEXT UNIQUE, "
                + "username TEXT UNIQUE, "
                + "password_hash TEXT, "
                + "totp_secret TEXT, "
                + "backup_codes TEXT"
                + ");";

        String logsTable = "CREATE TABLE IF NOT EXISTS login_logs ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "email TEXT, "
                + "status TEXT, "
                + "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP"
                + ");";

        String failedAttemptsTable = "CREATE TABLE IF NOT EXISTS failed_attempts ("
                + "username TEXT PRIMARY KEY, "
                + "attempts INTEGER DEFAULT 0, "
                + "locked BOOLEAN DEFAULT 0, "
                + "lock_time BIGINT DEFAULT 0"
                + ");";

        String failed2faTable = "CREATE TABLE IF NOT EXISTS failed_2fa_attempts ("
                + "username TEXT PRIMARY KEY, "
                + "attempts INTEGER DEFAULT 0, "
                + "locked BOOLEAN DEFAULT 0, "
                + "lock_time BIGINT DEFAULT 0"
                + ");";

        String accountsTable = "CREATE TABLE IF NOT EXISTS totp_accounts ("
                + "id TEXT PRIMARY KEY, "
                + "issuer TEXT, "
                + "account_name TEXT, "
                + "secret TEXT"
                + ");";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(usersTable);
            stmt.execute(logsTable);
            stmt.execute(failedAttemptsTable);
            stmt.execute(failed2faTable);
            stmt.execute(accountsTable);
        } catch (SQLException e) {
            System.err.println("[ERROR] Failed to initialize database schema.");
            e.printStackTrace();
        }
    }
}
