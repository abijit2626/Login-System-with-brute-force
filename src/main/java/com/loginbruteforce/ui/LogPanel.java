package com.loginbruteforce.ui;

import com.loginbruteforce.MainController;
import com.loginbruteforce.auth.LogManager;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class LogPanel extends JPanel {

    private final DefaultTableModel tableModel;

    public LogPanel(MainController controller) {
        setLayout(new BorderLayout(0, 20));
        setBackground(new Color(30, 30, 30));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // NORTH: Title
        JLabel titleLabel = new JLabel("App Login Logs");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        add(titleLabel, BorderLayout.NORTH);

        // CENTER: Table
        String[] columns = {"Username", "Time & Date", "Status", "Reason"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // NOT editable
            }
        };

        JTable logTable = new JTable(tableModel);
        logTable.setBackground(new Color(40, 40, 40));
        logTable.setForeground(Color.WHITE);
        logTable.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        logTable.setRowHeight(30);
        logTable.setGridColor(new Color(60, 60, 60));
        logTable.getTableHeader().setBackground(new Color(50, 50, 50));
        logTable.getTableHeader().setForeground(Color.WHITE);
        logTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));

        logTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                ((JComponent) c).setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                
                if (isSelected) {
                    c.setBackground(new Color(70, 70, 70));
                } else {
                    String status = (String) table.getValueAt(row, 2);
                    if ("SUCCESS".equals(status)) {
                        c.setBackground(new Color(30, 60, 30));
                    } else if ("FAILURE".equals(status)) {
                        c.setBackground(new Color(60, 30, 30));
                    } else {
                        c.setBackground(new Color(40, 40, 40));
                    }
                }
                c.setForeground(Color.WHITE);
                return c;
            }
        });

        JScrollPane scrollPane = new JScrollPane(logTable);
        scrollPane.getViewport().setBackground(new Color(30, 30, 30));
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(60, 60, 60)));
        add(scrollPane, BorderLayout.CENTER);

        // SOUTH: Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false);

        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refreshData());

        JButton backButton = new JButton("Back");
        backButton.addActionListener(e -> controller.navigateTo("HOME"));

        buttonPanel.add(refreshButton);
        buttonPanel.add(backButton);
        add(buttonPanel, BorderLayout.SOUTH);

        refreshData();
    }

    private void refreshData() {
        tableModel.setRowCount(0);
        List<String[]> logs = LogManager.getAllLogs();
        for (String[] row : logs) {
            tableModel.addRow(row);
        }
    }
}
