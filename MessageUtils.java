// MessageUtils.java

// Utility class for message-related operations such as timestamp formatting,
// token generation, and sanitization of input data.
import java.security.SecureRandom; // For secure random number generation
import java.time.LocalTime; // For getting the current time
import java.time.format.DateTimeFormatter; // For formatting the time

public class MessageUtils {

    /**
     * Gets the current time formatted as "HH:mm:ss".
     * Useful for timestamping messages.
     *
     * @return A String representing the current time in "HH:mm:ss" format.
     */
    public static String getCurrentTime() {
        // Use LocalTime to fetch the current time and format it using DateTimeFormatter
        return LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    /**
     * Generates a secure random token of 32 bytes, converted to a hexadecimal string.
     * This can be used as a secure token for identifying or authenticating connections.
     *
     * @return A 64-character hexadecimal String representing the secure token.
     */
    public static String generateSecureToken() {
        SecureRandom random = new SecureRandom(); // Instantiate a secure random number generator
        byte[] tokenBytes = new byte[32]; // 32 bytes = 256 bits
        random.nextBytes(tokenBytes); // Fill the byte array with random values
        return bytesToHex(tokenBytes); // Convert the byte array to a hexadecimal string
    }

    /**
     * Converts a byte array to a hexadecimal string.
     * Each byte is represented by two hexadecimal characters.
     *
     * @param bytes The byte array to convert.
     * @return A String containing the hexadecimal representation of the byte array.
     */
    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(); // StringBuilder for efficient string concatenation
        for (byte b : bytes) {
            sb.append(String.format("%02x", b)); // Convert each byte to a 2-character hexadecimal string
        }
        return sb.toString(); // Return the resulting hexadecimal string
    }

    /**
     * Sanitizes user input by removing potentially harmful characters.
     * Prevents injection attacks and other vulnerabilities.
     *
     * @param input The input string to sanitize.
     * @return A sanitized String with special characters removed.
     */
    public static String sanitizeInput(String input) {
        // Replace potentially dangerous characters with an empty string
        return input.replaceAll("[<>\"'%;()&+]", "");
    }

    /**
     * Sanitizes a file name by replacing invalid characters with underscores.
     * Ensures the file name is safe for use in the file system.
     *
     * @param fileName The file name to sanitize.
     * @return A sanitized String where invalid characters are replaced by underscores.
     */
    public static String sanitizeFileName(String fileName) {
        // Replace any character not allowed in file names with an underscore
        return fileName.replaceAll("[^a-zA-Z0-9\\.\\-_]", "_");
    }
}
