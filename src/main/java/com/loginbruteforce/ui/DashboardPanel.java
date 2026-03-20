package com.loginbruteforce.ui;

import com.loginbruteforce.db.DatabaseHelper;
import com.loginbruteforce.MainController;

import java.awt.BorderLayout;
import java.awt.Font;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;

/**
 * Dashboard displaying the current user's login audit history.
 *
 * <p>Logs are filtered by email so users only see their own activity.</p>
 */
public class DashboardPanel extends JPanel {

    private static final String LOGS_QUERY =
            "SELECT status, timestamp FROM login_logs WHERE email = ? ORDER BY timestamp DESC";

    /**
     * @param controller the {@link MainController} for navigation
     * @param username   the authenticated username
     * @param userEmail  the authenticated user's email (used to filter logs)
     */
    public DashboardPanel(MainController controller, String username, String userEmail) {
        setLayout(new BorderLayout());

        JLabel titleLabel = new JLabel("Dashboard — Welcome " + username, SwingConstants.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        titleLabel.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(titleLabel, BorderLayout.NORTH);

        DefaultTableModel tableModel = new DefaultTableModel(
                new String[]{"Status", "Timestamp"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        add(new JScrollPane(new JTable(tableModel)), BorderLayout.CENTER);
        loadLogs(tableModel, userEmail);

        JPanel bottomPanel = new JPanel();
        JButton logoutButton = new JButton("Logout");
        logoutButton.addActionListener(e -> {
            controller.setSessionContext(null, null);
            controller.navigateTo("HOME");
        });
        bottomPanel.add(logoutButton);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void loadLogs(DefaultTableModel model, String userEmail) {
        if (userEmail == null) {
            return;
        }
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(LOGS_QUERY)) {
            pstmt.setString(1, userEmail);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getString("status"),
                        rs.getString("timestamp")
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to load logs from database.",
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
