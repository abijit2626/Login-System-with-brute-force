package com.loginbruteforce.auth;

import com.loginbruteforce.db.DatabaseHelper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class LogManager {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void log(String username, String status, String reason) {
        String eventTime = LocalDateTime.now().format(FORMATTER);
        String sql = "INSERT INTO login_logs (username, event_time, status, reason) VALUES (?, ?, ?, ?)";
        
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, eventTime);
            pstmt.setString(3, status);
            pstmt.setString(4, reason);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static List<String[]> getAllLogs() {
        List<String[]> logs = new ArrayList<>();
        String sql = "SELECT username, event_time, status, reason FROM login_logs ORDER BY id DESC";
        
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
             
            while (rs.next()) {
                String username = rs.getString("username");
                String eventTime = rs.getString("event_time");
                String status = rs.getString("status");
                String reason = rs.getString("reason");
                
                if (reason == null) {
                    reason = "-";
                }
                
                logs.add(new String[]{username, eventTime, status, reason});
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return logs;
    }
}
