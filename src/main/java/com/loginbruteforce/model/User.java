package com.loginbruteforce.model;

/**
 * Domain model representing a registered user account.
 */
public class User {

    private int id;
    private String email;
    private String username;
    private String passwordHash;
    private String totpSecret;
    private String backupCodes;

    /** Default constructor for framework compatibility. */
    public User() {
    }

    /**
     * Constructs a fully initialized user.
     *
     * @param id           database primary key
     * @param email        unique email address
     * @param username     unique display name
     * @param passwordHash BCrypt-hashed password
     * @param totpSecret   Base32-encoded TOTP secret
     * @param backupCodes  comma-separated one-time backup codes
     */
    public User(int id, String email, String username, String passwordHash,
                String totpSecret, String backupCodes) {
        this.id = id;
        this.email = email;
        this.username = username;
        this.passwordHash = passwordHash;
        this.totpSecret = totpSecret;
        this.backupCodes = backupCodes;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getTotpSecret() { return totpSecret; }
    public void setTotpSecret(String totpSecret) { this.totpSecret = totpSecret; }

    public String getBackupCodes() { return backupCodes; }
    public void setBackupCodes(String backupCodes) { this.backupCodes = backupCodes; }
}
