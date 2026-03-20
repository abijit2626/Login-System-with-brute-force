package com.loginbruteforce.util;

import com.loginbruteforce.db.DatabaseHelper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Persists login audit events to the {@code login_logs} table.
 */
public final class Logger {

    private static final String INSERT_LOG_SQL =
            "INSERT INTO login_logs (email, status) VALUES (?, ?)";

    private Logger() {
        // Utility class — prevent instantiation
    }

    /**
     * Records a login event for the given user.
     *
     * @param email  the user's email address
     * @param status a description of the event (e.g. "Login Successful")
     */
    public static void log(String email, String status) {
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(INSERT_LOG_SQL)) {
            pstmt.setString(1, email);
            pstmt.setString(2, status);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[ERROR] Failed to write audit log.");
            e.printStackTrace();
        }
    }
}
