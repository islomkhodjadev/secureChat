// MessageUtils.java

import java.security.SecureRandom;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class MessageUtils {
    public static String getCurrentTime() {
        return LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    public static String generateSecureToken() {
        SecureRandom random = new SecureRandom();
        byte[] tokenBytes = new byte[32];
        random.nextBytes(tokenBytes);
        return bytesToHex(tokenBytes);
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static String sanitizeInput(String input) {
        return input.replaceAll("[<>\"'%;()&+]", "");
    }

    public static String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[^a-zA-Z0-9\\.\\-_]", "_");
    }
}
