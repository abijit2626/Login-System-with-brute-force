package com.loginbruteforce.ui;

import com.loginbruteforce.totp.TOTPGenerator;
import com.loginbruteforce.model.Account;
import com.loginbruteforce.model.AccountStore;
import com.loginbruteforce.MainController;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Container;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.Timer;

/**
 * Authenticator view displaying live TOTP codes for all saved accounts.
 *
 * <p>Each account's code refreshes automatically every second. A global
 * countdown timer shows how many seconds remain in the current TOTP window.</p>
 */
public class GeneratorPanel extends JPanel {

    private static final int CARD_MAX_WIDTH = 800;
    private static final int CARD_MAX_HEIGHT = 100;
    private static final int CODE_FONT_SIZE = 32;
    private static final int TIMER_FONT_SIZE = 14;
    private static final int TIMER_WARNING_FONT_SIZE = 15;
    private static final int TIMER_WARNING_THRESHOLD = 5;
    private static final int CODE_DISPLAY_LENGTH = 6;
    private static final int CODE_SPLIT_INDEX = 3;
    private static final int TIMER_INTERVAL_MS = 1000;

    private final MainController controller;
    private final AccountStore store;
    private final JPanel accountsContainer;
    private final JLabel timerLabel;
    private Timer refreshTimer;

    /**
     * @param controller the {@link MainController} for navigation
     */
    public GeneratorPanel(MainController controller) {
        this.controller = controller;
        this.store = new AccountStore();

        setLayout(new BorderLayout());

        // ── Header ────────────────────────────────────────────────────
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel leftTop = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        JButton backButton = new JButton("<- Back");
        backButton.addActionListener(e -> controller.navigateTo("HOME"));

        JLabel titleLabel = new JLabel("Authenticator");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));

        leftTop.add(backButton);
        leftTop.add(titleLabel);
        topPanel.add(leftTop, BorderLayout.WEST);

        JButton addButton = new JButton("+ Add Account");
        addButton.addActionListener(e -> controller.navigateTo("ADD_ACCOUNT"));
        topPanel.add(addButton, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        // ── Account List ──────────────────────────────────────────────
        accountsContainer = new JPanel();
        accountsContainer.setLayout(new BoxLayout(accountsContainer, BoxLayout.Y_AXIS));

        JScrollPane scrollPane = new JScrollPane(accountsContainer);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);

        // ── Countdown Timer ───────────────────────────────────────────
        timerLabel = new JLabel("Refreshes in --s", SwingConstants.CENTER);
        timerLabel.setFont(new Font("SansSerif", Font.BOLD, TIMER_FONT_SIZE));
        timerLabel.setForeground(Color.GRAY);
        timerLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(timerLabel, BorderLayout.SOUTH);

        refreshAccounts();
        startTimer();
    }

    /**
     * Returns the backing account store for external consumers.
     *
     * @return the {@link AccountStore}
     */
    public AccountStore getStore() {
        return store;
    }

    /**
     * Rebuilds the account card list. Call after adding or removing accounts.
     */
    public void refreshAccounts() {
        accountsContainer.removeAll();

        if (store.getAccounts().isEmpty()) {
            JLabel emptyLabel = new JLabel("No accounts. Click '+ Add Account' above.");
            emptyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            emptyLabel.setBorder(BorderFactory.createEmptyBorder(30, 0, 0, 0));
            accountsContainer.add(emptyLabel);
        } else {
            for (Account account : store.getAccounts()) {
                accountsContainer.add(buildAccountCard(account));
            }
        }

        accountsContainer.revalidate();
        accountsContainer.repaint();
        updateCodes();
    }

    // ── Card Builder ──────────────────────────────────────────────────

    private JPanel buildAccountCard(Account account) {
        JPanel card = new JPanel(new BorderLayout(10, 5));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(5, 10, 5, 10),
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(Color.LIGHT_GRAY), account.getIssuer())));
        card.setMaximumSize(new Dimension(CARD_MAX_WIDTH, CARD_MAX_HEIGHT));

        JPanel leftPanel = new JPanel(new GridLayout(2, 1));
        leftPanel.add(new JLabel(account.getAccountName()));

        JLabel codeLabel = new JLabel("------");
        codeLabel.setFont(new Font("Monospaced", Font.BOLD, CODE_FONT_SIZE));
        codeLabel.setForeground(Color.BLUE);
        codeLabel.putClientProperty("accountSecret", account.getSecret());
        leftPanel.add(codeLabel);
        card.add(leftPanel, BorderLayout.CENTER);

        JButton deleteButton = new JButton("\uD83D\uDDD1");
        deleteButton.setToolTipText("Remove Account");
        deleteButton.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Remove " + account.getIssuer()
                    + "?\nThis will NOT disable 2FA on the remote server.",
                    "Confirm Remove", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                store.removeAccount(account.getId());
                refreshAccounts();
            }
        });

        JPanel rightPanel = new JPanel(new GridBagLayout());
        rightPanel.add(deleteButton);
        card.add(rightPanel, BorderLayout.EAST);

        return card;
    }

    // ── Timer & Code Refresh ──────────────────────────────────────────

    private void startTimer() {
        if (refreshTimer != null) {
            refreshTimer.stop();
        }
        refreshTimer = new Timer(TIMER_INTERVAL_MS, e -> updateCodes());
        refreshTimer.start();
        updateCodes();
    }

    private void updateCodes() {
        int secondsLeft = TOTPGenerator.getRemainingSeconds();
        timerLabel.setText("All codes refresh in " + secondsLeft + "s");

        if (secondsLeft <= TIMER_WARNING_THRESHOLD) {
            timerLabel.setForeground(Color.RED);
            timerLabel.setFont(new Font("SansSerif", Font.BOLD, TIMER_WARNING_FONT_SIZE));
        } else {
            timerLabel.setForeground(Color.GRAY);
            timerLabel.setFont(new Font("SansSerif", Font.BOLD, TIMER_FONT_SIZE));
        }

        for (Component comp : accountsContainer.getComponents()) {
            if (comp instanceof JPanel) {
                updateLabelsInContainer((JPanel) comp);
            }
        }
    }

    private void updateLabelsInContainer(Container container) {
        for (Component c : container.getComponents()) {
            if (c instanceof JLabel) {
                JLabel label = (JLabel) c;
                Object secret = label.getClientProperty("accountSecret");
                if (secret != null) {
                    String code = TOTPGenerator.generateCurrentTOTP((String) secret);
                    if (code.length() == CODE_DISPLAY_LENGTH) {
                        code = code.substring(0, CODE_SPLIT_INDEX) + " "
                             + code.substring(CODE_SPLIT_INDEX);
                    }
                    label.setText(code);
                }
            } else if (c instanceof Container) {
                updateLabelsInContainer((Container) c);
            }
        }
    }
}
