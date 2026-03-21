package com.loginbruteforce.totp;

import java.awt.image.BufferedImage;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

/**
 * Generates QR code images for TOTP account setup.
 *
 * <p>Renders the standard {@code otpauth://} URI as a scannable QR image
 * compatible with Google Authenticator, Authy, and similar applications.</p>
 */
public final class QRCodeHelper {

    private static final String ISSUER = "LoginSystem";
    private static final String CHARSET = "UTF-8";
    private static final int COLOR_BLACK = 0xFF000000;
    private static final int COLOR_WHITE = 0xFFFFFFFF;

    private QRCodeHelper() {
        // Utility class — prevent instantiation
    }

    /**
     * Builds a standard {@code otpauth://totp/} URI.
     *
     * @param username     the account holder's username
     * @param base32Secret the Base32-encoded TOTP secret
     * @return the fully formed otpauth URI string
     */
    public static String buildOtpAuthUri(String username, String base32Secret) {
        try {
            String encodedIssuer = URLEncoder.encode(ISSUER, CHARSET).replace("+", "%20");
            String encodedUser = URLEncoder.encode(username, CHARSET).replace("+", "%20");
            return String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s",
                    encodedIssuer, encodedUser, base32Secret, encodedIssuer);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("UTF-8 encoding not available", e);
        }
    }

    /**
     * Generates a QR code {@link BufferedImage} that can be displayed in a Swing component.
     *
     * @param username     the account holder's username
     * @param base32Secret the Base32-encoded TOTP secret
     * @param width        desired image width in pixels
     * @param height       desired image height in pixels
     * @return the rendered QR image, or {@code null} on encoding failure
     */
    public static BufferedImage generateQRCodeImage(String username, String base32Secret,
                                                    int width, int height) {
        String uri = buildOtpAuthUri(username, base32Secret);
        try {
            BitMatrix bitMatrix = new QRCodeWriter().encode(uri, BarcodeFormat.QR_CODE, width, height);
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    image.setRGB(x, y, bitMatrix.get(x, y) ? COLOR_BLACK : COLOR_WHITE);
                }
            }
            return image;
        } catch (WriterException e) {
            e.printStackTrace();
            return null;
        }
    }
}
