package com.loginbruteforce.totp;

import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base32;

/**
 * TOTP (Time-based One-Time Password) utility compliant with
 * <a href="https://tools.ietf.org/html/rfc6238">RFC 6238</a>.
 *
 * <p>Uses Base32-encoded secrets compatible with Google Authenticator,
 * Authy, and other standard authenticator applications.</p>
 */
public final class TOTPHelper {

    private static final Base32 BASE_32 = new Base32();
    private static final String HMAC_ALGORITHM = "HmacSHA1";
    private static final int SECRET_BYTE_LENGTH = 20;
    private static final long TIME_STEP_SECONDS = 30L;
    private static final long TIME_STEP_MS = TIME_STEP_SECONDS * 1000L;
    private static final int CODE_DIGITS = 6;
    private static final int CODE_MODULUS = 1_000_000;

    /**
     * Number of time steps to check in each direction for clock-drift tolerance.
     * A value of 2 allows up to ~2.5 minutes of drift between server and client.
     */
    private static final int TOLERANCE_STEPS = 2;

    private TOTPHelper() {
        // Utility class — prevent instantiation
    }

    /**
     * Generates a cryptographically random 160-bit secret encoded as Base32.
     *
     * @return the Base32-encoded secret string
     */
    public static String generateSecret() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[SECRET_BYTE_LENGTH];
        random.nextBytes(bytes);
        return BASE_32.encodeToString(bytes).replace("=", "");
    }

    /**
     * Generates a TOTP code for a given secret and time step counter.
     *
     * @param base32Secret the Base32-encoded shared secret
     * @param timeStep     the TOTP time step counter
     * @return a zero-padded 6-digit code string
     */
    public static String generateTOTP(String base32Secret, long timeStep) {
        try {
            byte[] key = BASE_32.decode(base32Secret);
            byte[] data = ByteBuffer.allocate(8).putLong(timeStep).array();

            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(key, HMAC_ALGORITHM));
            byte[] hash = mac.doFinal(data);

            // Dynamic truncation per RFC 4226 §5.4
            int offset = hash[hash.length - 1] & 0x0F;
            long truncated = ((hash[offset] & 0x7F) << 24)
                           | ((hash[offset + 1] & 0xFF) << 16)
                           | ((hash[offset + 2] & 0xFF) << 8)
                           | (hash[offset + 3] & 0xFF);

            return String.format(java.util.Locale.US, "%0" + CODE_DIGITS + "d", truncated % CODE_MODULUS);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
            return "0".repeat(CODE_DIGITS);
        }
    }

    /**
     * Verifies a user-supplied code against the current and previous
     * time windows for clock-drift tolerance.
     *
     * @param base32Secret the user's Base32-encoded TOTP secret
     * @param code         the 6-digit code to verify
     * @return {@code true} if the code matches either window
     */
    public static boolean verifyCode(String base32Secret, String code) {
        long currentStep = System.currentTimeMillis() / TIME_STEP_MS;
        for (int i = -TOLERANCE_STEPS; i <= TOLERANCE_STEPS; i++) {
            if (code.equals(generateTOTP(base32Secret, currentStep + i))) {
                return true;
            }
        }
        return false;
    }
}
