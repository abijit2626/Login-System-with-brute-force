package com.loginbruteforce.auth;

import com.loginbruteforce.totp.TOTPHelper;
import com.loginbruteforce.model.User;
import com.loginbruteforce.db.DatabaseHelper;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Core authentication engine handling user registration, credential
 * verification, TOTP/backup-code management, and brute-force protection.
 *
 * <p>Passwords are hashed with BCrypt (cost factor 12). Accounts are
 * locked for {@value #LOCK_DURATION_MINUTES} minutes after exceeding
 * the failed-attempt threshold.</p>
 */
public class AuthManager extends BaseAuth {

    private static final long LOCK_DURATION_MINUTES = 15;
    private static final long LOCK_DURATION_MS = LOCK_DURATION_MINUTES * 60 * 1000;
    private static final int BCRYPT_COST_FACTOR = 12;
    private static final int LOGIN_LOCKOUT_THRESHOLD = 3;
    private static final int TWO_FA_LOCKOUT_THRESHOLD = 5;
    private static final int BACKUP_CODE_COUNT = 5;
    private static final int BACKUP_CODE_LENGTH = 8;

    /** Unambiguous character set for backup codes (no I/1/O/0). */
    private static final String BACKUP_CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    // ── Password Hashing ──────────────────────────────────────────────

    /**
     * Hashes a plaintext password using BCrypt with a random salt.
     *
     * @param password the plaintext password
     * @return the BCrypt hash string
     */
    public static String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(BCRYPT_COST_FACTOR));
    }

    /**
     * Verifies a plaintext password against a stored BCrypt hash.
     *
     * @param password   the plaintext password
     * @param storedHash the BCrypt hash to compare against
     * @return {@code true} if the password matches
     */
    public static boolean checkPassword(String password, String storedHash) {
        return BCrypt.checkpw(password, storedHash);
    }

    // ── Backup Codes ──────────────────────────────────────────────────

    /**
     * Generates a set of one-time-use emergency backup codes.
     *
     * @return comma-separated backup codes
     */
    public static String generateBackupCodes() {
        SecureRandom random = new SecureRandom();
        StringBuilder codes = new StringBuilder();
        for (int i = 0; i < BACKUP_CODE_COUNT; i++) {
            if (i > 0) {
                codes.append(',');
            }
            StringBuilder code = new StringBuilder(BACKUP_CODE_LENGTH);
            for (int j = 0; j < BACKUP_CODE_LENGTH; j++) {
                code.append(BACKUP_CODE_ALPHABET.charAt(
                        random.nextInt(BACKUP_CODE_ALPHABET.length())));
            }
            codes.append(code);
        }
        return codes.toString();
    }

    // ── User Existence Check ──────────────────────────────────────────

    /**
     * Checks whether an email or username is already registered.
     *
     * @param email    the email to check
     * @param username the username to check
     * @return {@code true} if either identifier is already taken
     */
    public boolean checkUserExists(String email, String username) {
        String sql = "SELECT id FROM users WHERE email = ? OR username = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email);
            pstmt.setString(2, username);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return true; // fail-safe: assume taken on DB error
    }

    // ── Registration ──────────────────────────────────────────────────

    /**
     * Registers a new user with a BCrypt-hashed password, a unique TOTP
     * secret, and a set of emergency backup codes.
     *
     * @param email    the user's email
     * @param username the desired username
     * @param password the plaintext password (will be hashed)
     * @return {@code [totpSecret, backupCodes]} on success, or {@code null} on failure
     */
    public String[] registerUser(String email, String username, String password) {
        if (checkUserExists(email, username)) {
            return null;
        }

        String totpSecret = TOTPHelper.generateSecret();
        String backupCodes = generateBackupCodes();

        String sql = "INSERT INTO users (email, username, password_hash, totp_secret, backup_codes) "
                   + "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email);
            pstmt.setString(2, username);
            pstmt.setString(3, hashPassword(password));
            pstmt.setString(4, totpSecret);
            pstmt.setString(5, backupCodes);
            pstmt.executeUpdate();
            return new String[]{totpSecret, backupCodes};
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    // ── Authentication ────────────────────────────────────────────────

    /**
     * Resolves the email address associated with a username.
     *
     * @param username the username to look up
     * @return the associated email, or {@code null} if not found
     */
    public String getEmailByUsername(String username) {
        String sql = "SELECT email FROM users WHERE username = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("email");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * {@inheritDoc}
     * Verifies credentials using BCrypt comparison.
     */
    @Override
    public boolean authenticate(String username, String password) {
        String sql = "SELECT password_hash FROM users WHERE username = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return checkPassword(password, rs.getString("password_hash"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // ── TOTP Secret Lookup ────────────────────────────────────────────

    /**
     * Retrieves the Base32-encoded TOTP secret for a user.
     *
     * @param username the username to look up
     * @return the TOTP secret, or {@code null} if not found
     */
    public String getTotpSecret(String username) {
        String sql = "SELECT totp_secret FROM users WHERE username = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("totp_secret");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // ── Backup Code Management ────────────────────────────────────────

    /**
     * Retrieves the remaining comma-separated backup codes for a user.
     *
     * @param username the username to look up
     * @return remaining codes, or {@code null} if not found
     */
    public String getBackupCodes(String username) {
        String sql = "SELECT backup_codes FROM users WHERE username = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("backup_codes");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Attempts to consume a one-time backup code. If the code matches,
     * it is permanently removed from the user's remaining codes.
     *
     * @param username the username
     * @param code     the backup code to consume
     * @return {@code true} if the code was valid and consumed
     */
    public boolean useBackupCode(String username, String code) {
        String storedCodes = getBackupCodes(username);
        if (storedCodes == null || storedCodes.isEmpty()) {
            return false;
        }

        String[] codes = storedCodes.split(",");
        StringBuilder remaining = new StringBuilder();
        boolean found = false;

        for (String c : codes) {
            if (!found && c.trim().equalsIgnoreCase(code.trim())) {
                found = true;
            } else {
                if (remaining.length() > 0) {
                    remaining.append(',');
                }
                remaining.append(c.trim());
            }
        }

        if (found) {
            String sql = "UPDATE users SET backup_codes = ? WHERE username = ?";
            try (Connection conn = DatabaseHelper.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, remaining.toString());
                pstmt.setString(2, username);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return found;
    }

    // ── Brute-Force Protection (Login) ────────────────────────────────

    /**
     * Checks if a user's account is currently locked due to excessive
     * failed login attempts. Auto-unlocks after the lock duration expires.
     *
     * @param username the username to check
     * @return {@code true} if the account is currently locked
     */
    public boolean isAccountLocked(String username) {
        String sql = "SELECT locked, lock_time FROM failed_attempts WHERE username = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                boolean locked = rs.getBoolean("locked");
                long lockTime = rs.getLong("lock_time");
                if (locked && lockTime > 0) {
                    if (System.currentTimeMillis() - lockTime >= LOCK_DURATION_MS) {
                        resetFailedAttempts(username);
                        return false;
                    }
                    return true;
                }
                return locked;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Returns the minutes remaining on the current lockout, or 0 if unlocked.
     *
     * @param username the username to check
     * @return remaining lock minutes
     */
    public int getRemainingLockMinutes(String username) {
        String sql = "SELECT lock_time FROM failed_attempts WHERE username = ? AND locked = 1";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                long remaining = LOCK_DURATION_MS - (System.currentTimeMillis() - rs.getLong("lock_time"));
                if (remaining > 0) {
                    return (int) Math.ceil(remaining / 60_000.0);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Atomically increments the failed login attempt counter. Locks the
     * account when the threshold ({@value #LOGIN_LOCKOUT_THRESHOLD}) is reached.
     *
     * @param username the username
     * @return the updated attempt count
     */
    public int incrementFailedAttempts(String username) {
        return incrementAttempts("failed_attempts", username, LOGIN_LOCKOUT_THRESHOLD);
    }

    /**
     * Resets the failed login attempt counter and removes any active lockout.
     *
     * @param username the username
     */
    public void resetFailedAttempts(String username) {
        resetAttempts("failed_attempts", username);
    }

    // ── Brute-Force Protection (2FA) ──────────────────────────────────

    /**
     * Atomically increments the failed 2FA attempt counter. Locks the
     * account when the threshold ({@value #TWO_FA_LOCKOUT_THRESHOLD}) is reached.
     *
     * @param username the username
     * @return the updated attempt count
     */
    public int increment2FAFailedAttempts(String username) {
        return incrementAttempts("failed_2fa_attempts", username, TWO_FA_LOCKOUT_THRESHOLD);
    }

    /**
     * Checks if a user is locked out of 2FA verification.
     *
     * @param username the username to check
     * @return {@code true} if the user is 2FA-locked
     */
    public boolean is2FALocked(String username) {
        String sql = "SELECT locked, lock_time FROM failed_2fa_attempts WHERE username = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                boolean locked = rs.getBoolean("locked");
                long lockTime = rs.getLong("lock_time");
                if (locked && lockTime > 0) {
                    if (System.currentTimeMillis() - lockTime >= LOCK_DURATION_MS) {
                        reset2FAFailedAttempts(username);
                        return false;
                    }
                    return true;
                }
                return locked;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Resets the failed 2FA attempt counter upon successful verification.
     *
     * @param username the username
     */
    public void reset2FAFailedAttempts(String username) {
        resetAttempts("failed_2fa_attempts", username);
    }

    // ── Private Helpers ───────────────────────────────────────────────

    /**
     * Generic atomic increment for any failed-attempts table.
     * Uses INSERT-or-ignore + UPDATE to prevent TOCTOU race conditions.
     */
    private int incrementAttempts(String table, String username, int lockThreshold) {
        String ensureRow = "INSERT OR IGNORE INTO " + table
                + " (username, attempts, locked, lock_time) VALUES (?, 0, 0, 0)";
        String atomicIncrement = "UPDATE " + table
                + " SET attempts = attempts + 1 WHERE username = ?";
        String lockIfNeeded = "UPDATE " + table
                + " SET locked = 1, lock_time = ? WHERE username = ? AND attempts >= "
                + lockThreshold + " AND locked = 0";
        String readBack = "SELECT attempts FROM " + table + " WHERE username = ?";

        int attempts = 1;
        try (Connection conn = DatabaseHelper.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(ensureRow)) {
                ps.setString(1, username);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(atomicIncrement)) {
                ps.setString(1, username);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(lockIfNeeded)) {
                ps.setLong(1, System.currentTimeMillis());
                ps.setString(2, username);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(readBack)) {
                ps.setString(1, username);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    attempts = rs.getInt("attempts");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return attempts;
    }

    /**
     * Resets all counters and lockout flags in the specified failed-attempts table.
     */
    private void resetAttempts(String table, String username) {
        String sql = "UPDATE " + table
                + " SET attempts = 0, locked = 0, lock_time = 0 WHERE username = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
