// EncryptionUtils.java

// Utility class for encryption and decryption using AES with CBC mode and PKCS5Padding
import javax.crypto.Cipher; // For performing encryption and decryption
import javax.crypto.SecretKey; // Represents the AES key
import javax.crypto.SecretKeyFactory; // For generating keys from a password
import javax.crypto.spec.IvParameterSpec; // Represents the initialization vector (IV)
import javax.crypto.spec.PBEKeySpec; // Specifies the key derivation parameters
import javax.crypto.spec.SecretKeySpec; // Converts a key to AES format
import java.security.spec.KeySpec; // Interface for key specifications
import java.security.SecureRandom; // For generating secure random numbers

public class EncryptionUtils {

    // Constants for key derivation
    private static final String SALT = "P2PMessengerSalt"; // Salt for key derivation
    private static final int ITERATIONS = 65536; // Number of iterations for PBKDF2
    private static final int KEY_LENGTH = 256; // AES key length in bits

    /**
     * Derives a SecretKey from a given token using PBKDF2 with HMAC-SHA256.
     *
     * @param token The input string (e.g., a password or shared token).
     * @return A SecretKey derived from the token.
     * @throws Exception If the key derivation fails.
     */
    public static SecretKey deriveKey(String token) throws Exception {
        // Use PBKDF2 with HMAC-SHA256 to derive a key from the token
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(token.toCharArray(), SALT.getBytes(), ITERATIONS, KEY_LENGTH);
        SecretKey tmp = factory.generateSecret(spec);
        // Convert the generated key to AES format
        return new SecretKeySpec(tmp.getEncoded(), "AES");
    }

    /**
     * Encrypts a plaintext string using AES encryption with CBC mode and PKCS5Padding.
     *
     * @param plainText The text to encrypt.
     * @param key       The AES SecretKey used for encryption.
     * @return A byte array containing the IV followed by the encrypted data.
     * @throws Exception If encryption fails.
     */
    public static byte[] encrypt(String plainText, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding"); // Initialize AES cipher
        byte[] iv = new byte[16]; // Create a 16-byte IV
        SecureRandom random = new SecureRandom(); // Generate secure random bytes for IV
        random.nextBytes(iv);
        IvParameterSpec ivSpec = new IvParameterSpec(iv); // Wrap the IV in an IvParameterSpec
        cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec); // Initialize cipher for encryption
        byte[] encrypted = cipher.doFinal(plainText.getBytes("UTF-8")); // Encrypt plaintext
        byte[] ivAndEncrypted = new byte[iv.length + encrypted.length]; // Combine IV and encrypted data
        System.arraycopy(iv, 0, ivAndEncrypted, 0, iv.length); // Copy IV
        System.arraycopy(encrypted, 0, ivAndEncrypted, iv.length, encrypted.length); // Copy encrypted data
        return ivAndEncrypted;
    }

    /**
     * Decrypts a byte array containing an IV and encrypted data.
     *
     * @param data The byte array containing the IV and encrypted data.
     * @param key  The AES SecretKey used for decryption.
     * @return The decrypted plaintext as a String.
     * @throws Exception If decryption fails or data is invalid.
     */
    public static String decrypt(byte[] data, SecretKey key) throws Exception {
        if (data.length < 16) { // Ensure data contains at least the IV
            throw new IllegalArgumentException("Invalid encrypted data");
        }
        byte[] iv = new byte[16]; // Extract the IV
        System.arraycopy(data, 0, iv, 0, iv.length);
        IvParameterSpec ivSpec = new IvParameterSpec(iv); // Wrap the IV in an IvParameterSpec
        byte[] encrypted = new byte[data.length - iv.length]; // Extract the encrypted data
        System.arraycopy(data, iv.length, encrypted, 0, encrypted.length);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding"); // Initialize AES cipher
        cipher.init(Cipher.DECRYPT_MODE, key, ivSpec); // Initialize cipher for decryption
        byte[] decrypted = cipher.doFinal(encrypted); // Decrypt the data
        return new String(decrypted, "UTF-8"); // Convert decrypted bytes to string
    }

    /**
     * Encrypts a byte array using AES encryption with CBC mode and PKCS5Padding.
     *
     * @param data The byte array to encrypt.
     * @param key  The AES SecretKey used for encryption.
     * @return A byte array containing the IV followed by the encrypted data.
     * @throws Exception If encryption fails.
     */
    public static byte[] encryptBytes(byte[] data, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding"); // Initialize AES cipher
        byte[] iv = new byte[16]; // Create a 16-byte IV
        SecureRandom random = new SecureRandom(); // Generate secure random bytes for IV
        random.nextBytes(iv);
        IvParameterSpec ivSpec = new IvParameterSpec(iv); // Wrap the IV in an IvParameterSpec
        cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec); // Initialize cipher for encryption
        byte[] encrypted = cipher.doFinal(data); // Encrypt the data
        byte[] ivAndEncrypted = new byte[iv.length + encrypted.length]; // Combine IV and encrypted data
        System.arraycopy(iv, 0, ivAndEncrypted, 0, iv.length); // Copy IV
        System.arraycopy(encrypted, 0, ivAndEncrypted, iv.length, encrypted.length); // Copy encrypted data
        return ivAndEncrypted;
    }

    /**
     * Decrypts a byte array containing an IV and encrypted data.
     *
     * @param data The byte array containing the IV and encrypted data.
     * @param key  The AES SecretKey used for decryption.
     * @return The decrypted data as a byte array.
     * @throws Exception If decryption fails or data is invalid.
     */
    public static byte[] decryptBytes(byte[] data, SecretKey key) throws Exception {
        if (data.length < 16) { // Ensure data contains at least the IV
            throw new IllegalArgumentException("Invalid encrypted data");
        }
        byte[] iv = new byte[16]; // Extract the IV
        System.arraycopy(data, 0, iv, 0, iv.length);
        IvParameterSpec ivSpec = new IvParameterSpec(iv); // Wrap the IV in an IvParameterSpec
        byte[] encrypted = new byte[data.length - iv.length]; // Extract the encrypted data
        System.arraycopy(data, iv.length, encrypted, 0, encrypted.length);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding"); // Initialize AES cipher
        cipher.init(Cipher.DECRYPT_MODE, key, ivSpec); // Initialize cipher for decryption
        return cipher.doFinal(encrypted); // Return decrypted bytes
    }
}
