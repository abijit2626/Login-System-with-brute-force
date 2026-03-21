package com.loginbruteforce.totp;

import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base32;

/**
 * Generates live TOTP codes for the authenticator's account list.
 *
 * <p>Implements the same RFC 6238 algorithm as {@link TOTPHelper}
 * to ensure code synchronization between login and generator views.</p>
 */
public final class TOTPGenerator {

    private static final Base32 BASE_32 = new Base32();
    private static final String HMAC_ALGORITHM = "HmacSHA1";
    private static final long TIME_STEP_MS = 30_000L;
    private static final int CODE_DIGITS = 6;
    private static final int CODE_MODULUS = 1_000_000;

    private TOTPGenerator() {
        // Utility class — prevent instantiation
    }

    /**
     * Generates the current 6-digit TOTP code for a given secret.
     *
     * @param base32Secret the Base32-encoded account secret
     * @return a zero-padded 6-digit code, or {@code "000000"} on error
     */
    public static String generateCurrentTOTP(String base32Secret) {
        if (base32Secret == null || base32Secret.isEmpty()) {
            return "0".repeat(CODE_DIGITS);
        }

        long timeStep = System.currentTimeMillis() / TIME_STEP_MS;
        try {
            byte[] key = BASE_32.decode(base32Secret);
            byte[] data = ByteBuffer.allocate(8).putLong(timeStep).array();

            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(key, HMAC_ALGORITHM));
            byte[] hash = mac.doFinal(data);

            int offset = hash[hash.length - 1] & 0x0F;
            long truncated = ((hash[offset] & 0x7F) << 24)
                           | ((hash[offset + 1] & 0xFF) << 16)
                           | ((hash[offset + 2] & 0xFF) << 8)
                           | (hash[offset + 3] & 0xFF);

            return String.format(java.util.Locale.US, "%0" + CODE_DIGITS + "d", truncated % CODE_MODULUS);
        } catch (NoSuchAlgorithmException | InvalidKeyException | IllegalArgumentException e) {
            e.printStackTrace();
            return "ERROR ";
        }
    }

    /**
     * Returns the seconds remaining until the current TOTP time window expires.
     *
     * @return seconds remaining (0–30)
     */
    public static int getRemainingSeconds() {
        long elapsedInWindow = System.currentTimeMillis() % TIME_STEP_MS;
        return (int) (30 - (elapsedInWindow / 1000));
    }
}
