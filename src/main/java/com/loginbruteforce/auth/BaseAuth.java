package com.loginbruteforce.auth;

/**
 * Abstract contract for all authentication providers.
 *
 * <p>Subclasses must implement {@link #authenticate(String, String)}
 * to define their specific credential verification strategy.</p>
 */
public abstract class BaseAuth {

    /**
     * Verifies a user's credentials.
     *
     * @param username the account username
     * @param password the plaintext password to verify
     * @return {@code true} if the credentials are valid, {@code false} otherwise
     */
    public abstract boolean authenticate(String username, String password);
}
