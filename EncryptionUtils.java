// EncryptionUtils.java

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.spec.KeySpec;
import java.security.SecureRandom;

public class EncryptionUtils {
    private static final String SALT = "P2PMessengerSalt";
    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH = 256;

    public static SecretKey deriveKey(String token) throws Exception {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(token.toCharArray(), SALT.getBytes(), ITERATIONS, KEY_LENGTH);
        SecretKey tmp = factory.generateSecret(spec);
        return new SecretKeySpec(tmp.getEncoded(), "AES");
    }

    public static byte[] encrypt(String plainText, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        byte[] iv = new byte[16];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);
        byte[] encrypted = cipher.doFinal(plainText.getBytes("UTF-8"));
        byte[] ivAndEncrypted = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, ivAndEncrypted, 0, iv.length);
        System.arraycopy(encrypted, 0, ivAndEncrypted, iv.length, encrypted.length);
        return ivAndEncrypted;
    }

    public static String decrypt(byte[] data, SecretKey key) throws Exception {
        if (data.length < 16) {
            throw new IllegalArgumentException("Invalid encrypted data");
        }
        byte[] iv = new byte[16];
        System.arraycopy(data, 0, iv, 0, iv.length);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        byte[] encrypted = new byte[data.length - iv.length];
        System.arraycopy(data, iv.length, encrypted, 0, encrypted.length);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);
        byte[] decrypted = cipher.doFinal(encrypted);
        return new String(decrypted, "UTF-8");
    }

    public static byte[] encryptBytes(byte[] data, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        byte[] iv = new byte[16];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);
        byte[] encrypted = cipher.doFinal(data);
        byte[] ivAndEncrypted = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, ivAndEncrypted, 0, iv.length);
        System.arraycopy(encrypted, 0, ivAndEncrypted, iv.length, encrypted.length);
        return ivAndEncrypted;
    }

    public static byte[] decryptBytes(byte[] data, SecretKey key) throws Exception {
        if (data.length < 16) {
            throw new IllegalArgumentException("Invalid encrypted data");
        }
        byte[] iv = new byte[16];
        System.arraycopy(data, 0, iv, 0, iv.length);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        byte[] encrypted = new byte[data.length - iv.length];
        System.arraycopy(data, iv.length, encrypted, 0, encrypted.length);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);
        return cipher.doFinal(encrypted);
    }
}
