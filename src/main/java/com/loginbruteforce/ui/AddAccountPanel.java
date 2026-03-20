package com.loginbruteforce.ui;

import com.loginbruteforce.model.Account;
import com.loginbruteforce.model.AccountStore;
import com.loginbruteforce.MainController;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.apache.commons.codec.binary.Base32;

/**
 * Panel for adding new TOTP accounts to the authenticator.
 *
 * <p>Supports two entry modes:</p>
 * <ul>
 *   <li><b>Paste URI</b> — paste an {@code otpauth://totp/} URI</li>
 *   <li><b>Manual Entry</b> — type the issuer, account name, and Base32 secret</li>
 * </ul>
 */
public class AddAccountPanel extends JPanel {

    private static final String CHARSET = "UTF-8";
    private static final int FIELD_COLUMNS = 15;
    private static final int URI_ROWS = 4;
    private static final int URI_COLUMNS = 20;

    private final MainController controller;
    private final GeneratorPanel generatorPanel;
    private final AccountStore store;

    /**
     * @param controller     the {@link MainController} for navigation
     * @param generatorPanel the generator panel to refresh after adding accounts
     */
    public AddAccountPanel(MainController controller, GeneratorPanel generatorPanel) {
        this.controller = controller;
        this.generatorPanel = generatorPanel;
        this.store = generatorPanel.getStore();

        setLayout(new BorderLayout());

        // ── Header ────────────────────────────────────────────────────
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton backButton = new JButton("<- Back");
        backButton.addActionListener(e -> controller.navigateTo("GENERATOR"));

        JLabel titleLabel = new JLabel("  Add New Account");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));

        headerPanel.add(backButton);
        headerPanel.add(titleLabel);
        add(headerPanel, BorderLayout.NORTH);

        // ── Tabbed Input ──────────────────────────────────────────────
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Paste URI", buildUriPanel());
        tabbedPane.addTab("Manual Entry", buildManualPanel());
        add(tabbedPane, BorderLayout.CENTER);
    }

    // ── URI Tab ───────────────────────────────────────────────────────

    private JPanel buildUriPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel label = new JLabel("<html>Paste a TOTP URI here:<br/>"
                + "<small>(e.g., otpauth://totp/Issuer:user?secret=XYZ)</small></html>");

        JTextArea uriArea = new JTextArea(URI_ROWS, URI_COLUMNS);
        uriArea.setLineWrap(true);

        JButton addButton = new JButton("Add Account");
        addButton.addActionListener(e -> {
            String uriStr = uriArea.getText().trim();
            if (uriStr.isEmpty()) {
                return;
            }
            Account account = parseOtpAuthUri(uriStr);
            if (account != null) {
                store.addAccount(account);
                generatorPanel.refreshAccounts();
                uriArea.setText("");
                controller.navigateTo("GENERATOR");
            } else {
                showError("Invalid URI format or missing secret code.");
            }
        });

        panel.add(label, BorderLayout.NORTH);
        panel.add(new JScrollPane(uriArea), BorderLayout.CENTER);
        panel.add(addButton, BorderLayout.SOUTH);
        return panel;
    }

    // ── Manual Tab ────────────────────────────────────────────────────

    private JPanel buildManualPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(15, 15, 15, 15));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField issuerField = new JTextField(FIELD_COLUMNS);
        JTextField nameField = new JTextField(FIELD_COLUMNS);
        JTextField secretField = new JTextField(FIELD_COLUMNS);
        JButton addButton = new JButton("Add Account");

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Issuer (e.g., GitHub):"), gbc);
        gbc.gridx = 1; panel.add(issuerField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Account Name:"), gbc);
        gbc.gridx = 1; panel.add(nameField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Secret Key (Base32):"), gbc);
        gbc.gridx = 1; panel.add(secretField, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        gbc.insets = new Insets(15, 5, 5, 5);
        panel.add(addButton, gbc);

        addButton.addActionListener(e -> {
            String issuer = issuerField.getText().trim();
            String name = nameField.getText().trim();
            String secret = secretField.getText().trim().toUpperCase().replace(" ", "");

            if (issuer.isEmpty() || name.isEmpty() || secret.isEmpty()) {
                showWarning("All fields are required.");
                return;
            }
            if (!isValidBase32(secret)) {
                showError("Invalid Base32 Secret Key.");
                return;
            }

            store.addAccount(new Account(issuer, name, secret));
            generatorPanel.refreshAccounts();
            issuerField.setText("");
            nameField.setText("");
            secretField.setText("");
            controller.navigateTo("GENERATOR");
        });

        return panel;
    }

    // ── OTP URI Parsing ───────────────────────────────────────────────

    private Account parseOtpAuthUri(String uriString) {
        try {
            URI uri = new URI(uriString);
            if (!"otpauth".equalsIgnoreCase(uri.getScheme())
                    || !"totp".equalsIgnoreCase(uri.getAuthority())) {
                return null;
            }

            String path = uri.getPath();
            if (path != null && path.startsWith("/")) {
                path = path.substring(1);
            }
            path = URLDecoder.decode(path, CHARSET);

            String issuer = "Unknown";
            String accountName = path;

            if (path.contains(":")) {
                String[] parts = path.split(":", 2);
                issuer = parts[0];
                accountName = parts[1];
            }

            String query = uri.getQuery();
            if (query == null) {
                return null;
            }

            String secret = null;
            for (String param : query.split("&")) {
                String[] pair = param.split("=", 2);
                if (pair.length == 2) {
                    if ("secret".equalsIgnoreCase(pair[0])) {
                        secret = pair[1].toUpperCase().replace(" ", "");
                    } else if ("issuer".equalsIgnoreCase(pair[0])) {
                        issuer = URLDecoder.decode(pair[1], CHARSET);
                    }
                }
            }

            if (secret == null || !isValidBase32(secret)) {
                return null;
            }
            return new Account(issuer, accountName, secret);

        } catch (URISyntaxException | UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static boolean isValidBase32(String secret) {
        return new Base32().isInAlphabet(secret);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void showWarning(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.WARNING_MESSAGE);
    }
}
