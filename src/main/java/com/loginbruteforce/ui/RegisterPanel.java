package com.loginbruteforce.ui;

import com.loginbruteforce.auth.AuthManager;
import com.loginbruteforce.auth.PasswordValidator;
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
 * Registration screen with password-strength validation.
 *
 * <p>On successful registration, generates TOTP credentials and
 * navigates to the {@link SetupCompletePanel} to display the QR code.</p>
 */
public class RegisterPanel extends JPanel {

    private static final int FIELD_COLUMNS = 15;
    private static final int FIELD_PADDING = 5;

    private final MainController controller;
    private final AuthManager authManager;

    /**
     * @param controller the application's {@link MainController} for navigation
     */
    public RegisterPanel(MainController controller) {
        this.controller = controller;
        this.authManager = new AuthManager();

        setLayout(new GridBagLayout());

        JLabel emailLabel = new JLabel("Email:");
        JTextField emailField = new JTextField(FIELD_COLUMNS);

        JLabel userLabel = new JLabel("Username:");
        JTextField userField = new JTextField(FIELD_COLUMNS);

        JLabel passLabel = new JLabel("Password:");
        JPasswordField passField = new JPasswordField(FIELD_COLUMNS);

        JButton submitButton = new JButton("Submit");
        JButton backButton = new JButton("Back");

        submitButton.addActionListener(e ->
                handleRegistration(emailField, userField, passField));

        backButton.addActionListener(e -> {
            clearFields(emailField, userField, passField);
            controller.navigateTo("HOME");
        });

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(FIELD_PADDING, FIELD_PADDING, FIELD_PADDING, FIELD_PADDING);

        gbc.gridx = 0; gbc.gridy = 0; add(emailLabel, gbc);
        gbc.gridx = 1; add(emailField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; add(userLabel, gbc);
        gbc.gridx = 1; add(userField, gbc);

        gbc.gridx = 0; gbc.gridy = 2; add(passLabel, gbc);
        gbc.gridx = 1; add(passField, gbc);

        gbc.gridx = 0; gbc.gridy = 3; add(backButton, gbc);
        gbc.gridx = 1; add(submitButton, gbc);
    }

    private void handleRegistration(JTextField emailField, JTextField userField,
                                    JPasswordField passField) {
        String email = emailField.getText().trim();
        String username = userField.getText().trim();
        String password = new String(passField.getPassword());

        if (email.isEmpty() || username.isEmpty() || password.isEmpty()) {
            showError("Please fill all fields");
            return;
        }

        String passwordError = PasswordValidator.validate(password);
        if (passwordError != null) {
            JOptionPane.showMessageDialog(this, passwordError, "Weak Password",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (authManager.checkUserExists(email, username)) {
            showError("Registration could not be completed. Please try different credentials.");
            return;
        }

        String[] secrets = authManager.registerUser(email, username, password);
        if (secrets != null) {
            controller.setSessionContext(username, email);
            controller.registerView(
                    new SetupCompletePanel(controller, username, secrets[0], secrets[1]),
                    "SETUP_COMPLETE");
            clearFields(emailField, userField, passField);
            controller.navigateTo("SETUP_COMPLETE");
        } else {
            showError("A database error occurred");
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private static void clearFields(JTextField emailField, JTextField userField,
                                    JPasswordField passField) {
        emailField.setText("");
        userField.setText("");
        passField.setText("");
    }
}
