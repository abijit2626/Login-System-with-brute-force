package com.loginbruteforce.ui;

import com.loginbruteforce.totp.QRCodeHelper;
import com.loginbruteforce.model.Account;
import com.loginbruteforce.MainController;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Color;
import java.awt.image.BufferedImage;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

/**
 * Post-registration screen displaying the user's TOTP QR code and
 * emergency backup codes so they can configure their authenticator app.
 */
public class SetupCompletePanel extends JPanel {

    private static final int QR_SIZE = 200;

    /**
     * @param controller   the {@link MainController} for navigation
     * @param username     the newly registered username
     * @param base32Secret the generated TOTP secret
     * @param backupCodes  comma-separated emergency backup codes
     */
    public SetupCompletePanel(MainController controller, String username,
                              String base32Secret, String backupCodes) {
        setLayout(new BorderLayout(10, 10));

        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel successLabel = createCenteredLabel("Account created successfully!",
                new Font("SansSerif", Font.BOLD, 18));
        centerPanel.add(successLabel);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 15)));

        centerPanel.add(createCenteredLabel(
                "Scan this QR code with your Authenticator App:", null));
        centerPanel.add(Box.createRigidArea(new Dimension(0, 15)));

        BufferedImage qrImage = QRCodeHelper.generateQRCodeImage(
                username, base32Secret, QR_SIZE, QR_SIZE);
        if (qrImage != null) {
            JLabel qrLabel = new JLabel(new ImageIcon(qrImage));
            qrLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            centerPanel.add(qrLabel);
        } else {
            JLabel errorLabel = createCenteredLabel("(Failed to generate QR Code image)", null);
            errorLabel.setForeground(Color.RED);
            centerPanel.add(errorLabel);
        }

        centerPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        JTextField secretField = new JTextField(base32Secret);
        secretField.setEditable(false);
        secretField.setHorizontalAlignment(SwingConstants.CENTER);
        secretField.setMaximumSize(new Dimension(300, 30));
        centerPanel.add(secretField);

        centerPanel.add(Box.createRigidArea(new Dimension(0, 20)));

        JLabel backupTitle = createCenteredLabel("SAVE THESE BACKUP CODES NOW:",
                new Font("SansSerif", Font.BOLD, 14));
        backupTitle.setForeground(Color.RED);
        centerPanel.add(backupTitle);

        JTextArea codesArea = new JTextArea(backupCodes.replace(',', '\n'));
        codesArea.setEditable(false);
        codesArea.setFont(new Font("Monospaced", Font.BOLD, 16));
        codesArea.setOpaque(false);
        codesArea.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel codesWrapper = new JPanel();
        codesWrapper.add(codesArea);
        centerPanel.add(codesWrapper);

        add(centerPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel();
        JButton continueButton = new JButton("I have saved my backup codes & scanned the QR");
        continueButton.addActionListener(e -> controller.navigateTo("HOME"));
        bottomPanel.add(continueButton);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private static JLabel createCenteredLabel(String text, Font font) {
        JLabel label = new JLabel(text);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        if (font != null) {
            label.setFont(font);
        }
        return label;
    }
}
