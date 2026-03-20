package com.loginbruteforce.auth;

import com.loginbruteforce.totp.TOTPHelper;
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
import javax.swing.JTextField;

/**
 * 2FA challenge screen that verifies TOTP codes or emergency backup codes.
 *
 * <p>Enforces brute-force protection: after {@value #MAX_2FA_ATTEMPTS}
 * consecutive failures, the account is locked for 15 minutes.</p>
 */
public class TwoFAPanel extends JPanel {

    private static final int MAX_2FA_ATTEMPTS = 5;
    private static final int CODE_FIELD_COLUMNS = 10;
    private static final int COMPONENT_PADDING = 10;

    private final MainController controller;
    private final AuthManager authManager;
    private final JTextField codeField;

    /**
     * @param controller the application's {@link MainController} for navigation
     */
    public TwoFAPanel(MainController controller) {
        this.controller = controller;
        this.authManager = new AuthManager();

        setLayout(new GridBagLayout());

        JLabel label = new JLabel("Enter 6-digit Authenticator Code:");
        codeField = new JTextField(CODE_FIELD_COLUMNS);

        JButton verifyButton = new JButton("Verify Code");
        JButton backupButton = new JButton("Use Backup Code Instead");
        JButton cancelButton = new JButton("Cancel");

        verifyButton.addActionListener(e -> verifyTotp());
        backupButton.addActionListener(e -> promptBackupCode());
        cancelButton.addActionListener(e -> {
            codeField.setText("");
            controller.navigateTo("HOME");
        });

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(COMPONENT_PADDING, COMPONENT_PADDING,
                                COMPONENT_PADDING, COMPONENT_PADDING);

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2; add(label, gbc);
        gbc.gridy = 1; add(codeField, gbc);
        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = 2; add(cancelButton, gbc);
        gbc.gridx = 1; add(verifyButton, gbc);
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2; add(backupButton, gbc);
    }

    // ── TOTP Verification ─────────────────────────────────────────────

    private void verifyTotp() {
        String username = controller.getCurrentUser();
        String email = controller.getCurrentEmail();
        String code = codeField.getText().replaceAll("\\s+", "");

        if (code.isEmpty()) {
            showError("Code cannot be empty.");
            return;
        }
        if (isLockedOut(username, email)) {
            return;
        }

        String totpSecret = authManager.getTotpSecret(username);
        if (totpSecret == null) {
            showError("Error finding 2FA configuration for this user.");
            return;
        }

        if (TOTPHelper.verifyCode(totpSecret, code)) {
            onSuccess(username, email);
        } else {
            handleFailedAttempt(username, email, "Invalid Authenticator Code.");
        }
    }

    // ── Backup Code Verification ──────────────────────────────────────

    private void promptBackupCode() {
        String username = controller.getCurrentUser();
        String email = controller.getCurrentEmail();

        if (isLockedOut(username, email)) {
            return;
        }

        String code = JOptionPane.showInputDialog(this,
                "Enter one of your 8-character emergency backup codes:\n"
                + "(Note: This will consume the code)",
                "Use Backup Code", JOptionPane.PLAIN_MESSAGE);

        if (code != null && !code.trim().isEmpty()) {
            if (authManager.useBackupCode(username, code)) {
                onSuccess(username, email);
            } else {
                handleFailedAttempt(username, email, "Invalid or already-used Backup Code.");
            }
        }
    }

    // ── Shared Helpers ────────────────────────────────────────────────

    private boolean isLockedOut(String username, String email) {
        if (authManager.is2FALocked(username)) {
            Logger.log(email, "2FA Blocked — Account Locked (Too Many Attempts)");
            showError("Too many failed 2FA attempts.\nAccount locked for 15 minutes.");
            codeField.setText("");
            controller.navigateTo("HOME");
            return true;
        }
        return false;
    }

    private void handleFailedAttempt(String username, String email, String baseMessage) {
        int attempts = authManager.increment2FAFailedAttempts(username);
        if (attempts >= MAX_2FA_ATTEMPTS) {
            Logger.log(email, "2FA Brute Force Detected (Account Locked)");
            showError("Too many failed 2FA attempts.\nAccount locked for 15 minutes.");
            codeField.setText("");
            controller.navigateTo("HOME");
        } else {
            int remaining = MAX_2FA_ATTEMPTS - attempts;
            showError(baseMessage + " " + remaining + " attempt(s) remaining.");
        }
    }

    private void onSuccess(String username, String email) {
        authManager.reset2FAFailedAttempts(username);
        Logger.log(email, "Login Successful (2FA Passed)");
        codeField.setText("");
        controller.navigateTo("GENERATOR");
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
