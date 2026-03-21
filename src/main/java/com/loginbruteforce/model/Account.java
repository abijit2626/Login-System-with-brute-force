package com.loginbruteforce.model;

import java.util.UUID;

/**
 * Immutable model representing a single TOTP account in the authenticator.
 *
 * <p>Each account stores the issuer (e.g. "GitHub"), the user's account
 * name, and the Base32-encoded shared secret used to generate codes.</p>
 */
public class Account {

    private final String id;
    private final String issuer;
    private final String accountName;
    private final String secret;

    /**
     * Creates a new account with a randomly generated UUID.
     *
     * @param issuer      the service provider name
     * @param accountName the user's identifier on that service
     * @param secret      the Base32-encoded TOTP secret
     */
    public Account(String issuer, String accountName, String secret) {
        this(UUID.randomUUID().toString(), issuer, accountName, secret);
    }

    /**
     * Creates an account with a specific ID (used when loading from storage).
     *
     * @param id          the unique account identifier
     * @param issuer      the service provider name
     * @param accountName the user's identifier on that service
     * @param secret      the Base32-encoded TOTP secret
     */
    public Account(String id, String issuer, String accountName, String secret) {
        this.id = id;
        this.issuer = issuer;
        this.accountName = accountName;
        // Strip out any padding '=' characters which can break authenticator apps
        this.secret = secret != null ? secret.replace("=", "") : null;
    }

    public String getId() { return id; }
    public String getIssuer() { return issuer; }
    public String getAccountName() { return accountName; }
    public String getSecret() { return secret; }

    @Override
    public String toString() {
        return issuer + " (" + accountName + ")";
    }
}
