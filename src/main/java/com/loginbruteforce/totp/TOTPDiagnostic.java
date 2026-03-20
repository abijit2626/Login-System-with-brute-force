package com.loginbruteforce.totp;

import org.apache.commons.codec.binary.Base32;
import java.nio.ByteBuffer;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;

/**
 * Diagnostic test to trace the TOTP lifecycle end-to-end.
 * Run: javac -cp ".;commons-codec-1.17.0.jar" TOTPDiagnostic.java
 *      java -cp ".;commons-codec-1.17.0.jar" TOTPDiagnostic
 */
public class TOTPDiagnostic {

    public static void main(String[] args) throws Exception {
        Base32 base32 = new Base32();

        // Step 1: Generate secret (same as TOTPHelper.generateSecret)
        byte[] rawBytes = new byte[20];
        new SecureRandom().nextBytes(rawBytes);
        String secret = base32.encodeToString(rawBytes);

        System.out.println("=== TOTP Diagnostic ===");
        System.out.println("Raw bytes length  : " + rawBytes.length);
        System.out.println("Encoded secret    : [" + secret + "]");
        System.out.println("Secret length     : " + secret.length());
        System.out.println("Secret chars      : ");
        for (int i = 0; i < secret.length(); i++) {
            char c = secret.charAt(i);
            System.out.printf("  [%d] '%c' (0x%02X)%n", i, c, (int) c);
        }

        // Step 2: Check for hidden characters
        boolean hasNonAlnum = false;
        for (char c : secret.toCharArray()) {
            if (!Character.isLetterOrDigit(c)) {
                hasNonAlnum = true;
                System.out.println("WARNING: Non-alphanumeric char found: 0x" + Integer.toHexString(c));
            }
        }
        if (!hasNonAlnum) {
            System.out.println("OK: No non-alphanumeric characters in secret");
        }

        // Step 3: Decode roundtrip
        byte[] decoded = base32.decode(secret);
        System.out.println("\nDecoded length    : " + decoded.length);
        boolean match = java.util.Arrays.equals(rawBytes, decoded);
        System.out.println("Roundtrip match   : " + match);

        // Step 4: Generate TOTP code
        long timeStepMs = 30_000L;
        long currentStep = System.currentTimeMillis() / timeStepMs;
        String code = generateTOTP(secret, currentStep, base32);
        System.out.println("\nCurrent time ms   : " + System.currentTimeMillis());
        System.out.println("Current time step : " + currentStep);
        System.out.println("Generated TOTP    : " + code);

        // Step 5: Verify the code we just generated (should pass)
        boolean verified = verifyCode(secret, code, base32);
        System.out.println("Self-verify       : " + verified);

        // Step 6: Test with a KNOWN secret to compare with Google Authenticator
        // RFC 6238 test vector: Base32("12345678901234567890") = GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ
        String testSecret = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";
        long testStep = 1; // T = 30 seconds
        String testCode = generateTOTP(testSecret, testStep, base32);
        System.out.println("\n=== RFC 6238 Test Vector ===");
        System.out.println("Secret: " + testSecret);
        System.out.println("Time step 1 code  : " + testCode);
        // Expected for SHA1, step=1: should be 287082 per RFC
        // Actually that's for step 59/30=1, let me check
        // RFC 6238 test: T=59s, step=1, SHA1 -> 287082? No, that's for the test seed.
        // The test seed is "12345678901234567890" (ASCII), step=1
        // Let me just print it for manual verification

        // Step 7: Check otpauth URI
        String uri = String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s",
                "LoginSystem", "testuser", secret, "LoginSystem");
        System.out.println("\n=== OTPAuth URI ===");
        System.out.println(uri);

        // Step 8: Simulate the trim() behavior
        String codeWithSpaces = " " + code + " ";
        System.out.println("\n=== Input Sanitization ===");
        System.out.println("Code with spaces trimmed: [" + codeWithSpaces.trim() + "]");
        System.out.println("Verify trimmed code     : " + verifyCode(secret, codeWithSpaces.trim(), base32));

        System.out.println("\n=== DONE ===");
    }

    static String generateTOTP(String base32Secret, long timeStep, Base32 base32) throws Exception {
        byte[] key = base32.decode(base32Secret);
        byte[] data = ByteBuffer.allocate(8).putLong(timeStep).array();
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(key, "HmacSHA1"));
        byte[] hash = mac.doFinal(data);
        int offset = hash[hash.length - 1] & 0x0F;
        long truncated = ((hash[offset] & 0x7F) << 24)
                       | ((hash[offset + 1] & 0xFF) << 16)
                       | ((hash[offset + 2] & 0xFF) << 8)
                       | (hash[offset + 3] & 0xFF);
        return String.format("%06d", truncated % 1_000_000);
    }

    static boolean verifyCode(String base32Secret, String code, Base32 base32) throws Exception {
        long currentStep = System.currentTimeMillis() / 30_000L;
        for (int i = -2; i <= 2; i++) {
            if (code.equals(generateTOTP(base32Secret, currentStep + i, base32))) {
                return true;
            }
        }
        return false;
    }
}
