package com.loginbruteforce;

import com.loginbruteforce.auth.TwoFAPanel;
import com.loginbruteforce.ui.AddAccountPanel;
import com.loginbruteforce.ui.HomePanel;
import com.loginbruteforce.ui.LoginPanel;
import com.loginbruteforce.ui.RegisterPanel;
import com.loginbruteforce.ui.GeneratorPanel;

import java.awt.BorderLayout;
import java.awt.CardLayout;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import com.formdev.flatlaf.FlatDarkLaf;

/**
 * Unified application controller using {@link CardLayout} to manage
 * screen transitions within a single {@link JFrame}.
 *
 * <p>Registered panels navigate by calling {@link #navigateTo(String)}.
 * Session state (current user) is shared via {@link #setSessionContext}.</p>
 */
public class MainController extends JFrame {

    private static final String TITLE = "Unified Security Console";
    private static final int WINDOW_WIDTH = 500;
    private static final int WINDOW_HEIGHT = 600;

    private final CardLayout cardLayout;
    private final JPanel cardContainer;

    private String currentUser;
    private String currentEmail;

    /**
     * Initialises the controller, applies FlatLaf theme, and sets up the card container.
     */
    public MainController() {
        applyLookAndFeel();

        setTitle(TITLE);
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        cardContainer = new JPanel(cardLayout);
        add(cardContainer, BorderLayout.CENTER);
    }

    // ── Navigation ────────────────────────────────────────────────────

    /**
     * Switches the visible panel to the one registered under {@code viewName}.
     *
     * @param viewName the key used during {@link #registerView}
     */
    public void navigateTo(String viewName) {
        cardLayout.show(cardContainer, viewName);
    }

    /**
     * Registers a panel into the card container.
     *
     * @param view     the panel to register
     * @param viewName the unique key for this panel
     */
    public void registerView(JPanel view, String viewName) {
        cardContainer.add(view, viewName);
    }

    // ── Session Context ───────────────────────────────────────────────

    /**
     * Stores the currently authenticated user's details so that
     * downstream panels can access them without constructor coupling.
     *
     * @param user  the authenticated username
     * @param email the associated email address
     */
    public void setSessionContext(String user, String email) {
        this.currentUser = user;
        this.currentEmail = email;
    }

    /** @return the currently authenticated username, or {@code null} */
    public String getCurrentUser() { return currentUser; }

    /** @return the currently authenticated user's email, or {@code null} */
    public String getCurrentEmail() { return currentEmail; }

    // ── Look & Feel ───────────────────────────────────────────────────

    private void applyLookAndFeel() {
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (UnsupportedLookAndFeelException e) {
            System.err.println("[WARN] FlatLaf theme unavailable; using system default.");
        }
    }

    // ── Entry Point ───────────────────────────────────────────────────

    /**
     * Application entry point invoked by Launcher. Wires all panels
     * and displays the home screen.
     *
     * @param args command-line arguments (unused)
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainController console = new MainController();

            console.registerView(new HomePanel(console), "HOME");
            console.registerView(new LoginPanel(console), "LOGIN");
            console.registerView(new RegisterPanel(console), "REGISTER");
            console.registerView(new TwoFAPanel(console), "2FA");

            GeneratorPanel generatorPanel = new GeneratorPanel(console);
            console.registerView(generatorPanel, "GENERATOR");
            console.registerView(new AddAccountPanel(console, generatorPanel), "ADD_ACCOUNT");

            console.navigateTo("HOME");
            console.setVisible(true);
        });
    }
}
