package com.loginbruteforce.ui;

import com.loginbruteforce.MainController;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JButton;
import javax.swing.JPanel;

/**
 * Landing screen presenting options to register, log in, or exit.
 */
public class HomePanel extends JPanel {

    private static final int BUTTON_PADDING = 10;

    /**
     * @param controller the application's {@link MainController} for navigation
     */
    public HomePanel(MainController controller) {
        setLayout(new GridBagLayout());

        JButton registerButton = new JButton("Register");
        JButton loginButton = new JButton("Login");
        JButton exitButton = new JButton("Exit");

        registerButton.addActionListener(e -> controller.navigateTo("REGISTER"));
        loginButton.addActionListener(e -> controller.navigateTo("LOGIN"));
        exitButton.addActionListener(e -> System.exit(0));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(BUTTON_PADDING, BUTTON_PADDING, BUTTON_PADDING, BUTTON_PADDING);
        gbc.gridx = 0;

        gbc.gridy = 0; add(registerButton, gbc);
        gbc.gridy = 1; add(loginButton, gbc);
        gbc.gridy = 2; add(exitButton, gbc);
    }
}
