package com.loginbruteforce.ui;

import com.loginbruteforce.auth.AuthManager;
import com.loginbruteforce.model.Account;
import com.loginbruteforce.util.Logger;
import com.loginbruteforce.MainController;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

/**
 * Login screen with brute-force protection.
 *
 * <p>On successful credential verification, stores the user context
 * in the {@link MainController} and navigates to the 2FA challenge.</p>
 */
public class LoginPanel extends JPanel {

    private static final int FIELD_COLUMNS = 15;
    private static final int FIELD_PADDING = 5;
    private static final int LOCKOUT_THRESHOLD = 3;

    private final MainController controller;
    private final AuthManager authManager;

    /**
     * @param controller the application's {@link MainController} for navigation
     */
    public LoginPanel(MainController controller) {
        this.controller = controller;
        this.authManager = new AuthManager();

        setLayout(new GridBagLayout());

        JLabel userLabel = new JLabel("Username:");
        JTextField userField = new JTextField(FIELD_COLUMNS);

        JLabel passLabel = new JLabel("Password:");
        JPasswordField passField = new JPasswordField(FIELD_COLUMNS);

        JButton loginButton = new JButton("Login");
        JButton backButton = new JButton("Back");

        loginButton.addActionListener(e -> handleLogin(userField, passField));

        backButton.addActionListener(e -> {
            clearFields(userField, passField);
            controller.navigateTo("HOME");
        });

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(FIELD_PADDING, FIELD_PADDING, FIELD_PADDING, FIELD_PADDING);

        gbc.gridx = 0; gbc.gridy = 0; add(userLabel, gbc);
        gbc.gridx = 1; add(userField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; add(passLabel, gbc);
        gbc.gridx = 1; add(passField, gbc);

        gbc.gridx = 0; gbc.gridy = 2; add(backButton, gbc);
        gbc.gridx = 1; add(loginButton, gbc);
    }

    private void handleLogin(JTextField userField, JPasswordField passField) {
        String username = userField.getText().trim();
        String password = new String(passField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            showError("Please fill all fields");
            return;
        }

        if (authManager.isAccountLocked(username)) {
            int minsLeft = authManager.getRemainingLockMinutes(username);
            String email = authManager.getEmailByUsername(username);
            Logger.log(email != null ? email : username, "Blocked — Still Locked");
            showError("Account Locked: Brute Force Detected.\nPlease try again in "
                    + minsLeft + " minute(s).");
            return;
        }

        if (authManager.authenticate(username, password)) {
            authManager.resetFailedAttempts(username);
            String email = authManager.getEmailByUsername(username);
            controller.setSessionContext(username, email);
            controller.navigateTo("2FA");
            clearFields(userField, passField);
        } else {
            handleFailedAttempt(username);
        }
    }

    private void handleFailedAttempt(String username) {
        int attempts = authManager.incrementFailedAttempts(username);
        if (attempts >= LOCKOUT_THRESHOLD) {
            String email = authManager.getEmailByUsername(username);
            Logger.log(email != null ? email : username, "Brute Force Detected (Account Locked)");
            showError("Account Locked for 15 minutes due to multiple failed attempts.");
        } else {
            showError("Invalid credentials");
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private static void clearFields(JTextField userField, JPasswordField passField) {
        userField.setText("");
        passField.setText("");
    }
}
