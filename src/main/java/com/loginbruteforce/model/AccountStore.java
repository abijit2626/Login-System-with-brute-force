package com.loginbruteforce.model;

import com.loginbruteforce.db.DatabaseHelper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Manages persistence of {@link Account} objects to the unified SQLite database.
 *
 * <p>Uses the existing {@link DatabaseHelper} connection pool to store all
 * standalone Authenticator accounts securely inside the app database,
 * replacing the old JSON file approach.</p>
 */
public class AccountStore {

    /**
     * Retrieves all saved authenticator accounts from the database.
     *
     * @return an unmodifiable list of accounts
     */
    public List<Account> getAccounts() {
        List<Account> accounts = new ArrayList<>();
        String sql = "SELECT id, issuer, account_name, secret FROM totp_accounts";
        try (Connection conn = DatabaseHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                String id = rs.getString("id");
                String issuer = rs.getString("issuer");
                String accountName = rs.getString("account_name");
                String secret = rs.getString("secret");
                accounts.add(new Account(id, issuer, accountName, secret));
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] Failed to load accounts from database: " + e.getMessage());
        }
        return Collections.unmodifiableList(accounts);
    }

    /**
     * Adds an account to the database.
     *
     * @param account the account to add
     */
    public void addAccount(Account account) {
        String sql = "INSERT INTO totp_accounts (id, issuer, account_name, secret) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, account.getId());
            pstmt.setString(2, account.getIssuer());
            pstmt.setString(3, account.getAccountName());
            pstmt.setString(4, account.getSecret());
            pstmt.executeUpdate();
            
        } catch (SQLException e) {
            System.err.println("[ERROR] Failed to insert account: " + e.getMessage());
        }
    }

    /**
     * Removes an account from the database by its unique ID.
     *
     * @param id the UUID of the account to remove
     */
    public void removeAccount(String id) {
        String sql = "DELETE FROM totp_accounts WHERE id = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, id);
            pstmt.executeUpdate();
            
        } catch (SQLException e) {
            System.err.println("[ERROR] Failed to delete account: " + e.getMessage());
        }
    }
}
