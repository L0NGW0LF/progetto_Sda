package com.secureapp.util;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * AES-256-GCM encryption/decryption utility.
 * 
 * Security features:
 * - AES-256-GCM (authenticated encryption)
 * - Random IV generated for each encryption (never reused)
 * - 128-bit authentication tag
 * - Key loaded from external keystore (not hardcoded)
 * 
 * Format: [12-byte IV][Ciphertext + 16-byte Auth Tag]
 * 
 * Follows GEMINI.md requirement 4.8:
 * "AES per cifrare dati quando richiesto; IV casuale; modalità sicura (es.
 * GCM)"
 */
public class AesEncryptionUtil {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12; // 96 bits (recommended for GCM)
    private static final int GCM_TAG_LENGTH = 128; // 128 bits

    private static final SecureRandom secureRandom = new SecureRandom();

    // Lazy-loaded secret key
    private static SecretKey aesKey;

    /**
     * Get AES key from keystore (lazy loaded).
     */
    private static SecretKey getAesKey() throws Exception {
        if (aesKey == null) {
            synchronized (AesEncryptionUtil.class) {
                if (aesKey == null) {
                    aesKey = KeystoreConfig.getInstance().getAesKey();
                }
            }
        }
        return aesKey;
    }

    /**
     * Encrypt plaintext bytes with AES-256-GCM.
     * 
     * Output format: [IV][Ciphertext+Tag]
     * - IV: 12 bytes (random, unique for each encryption)
     * - Ciphertext+Tag: Variable length (plaintext length + 16 bytes tag)
     * 
     * @param plaintext Plaintext bytes to encrypt
     * @return Encrypted bytes (IV prepended)
     * @throws Exception if encryption fails
     */
    public static byte[] encrypt(byte[] plaintext) throws Exception {
        if (plaintext == null) {
            throw new IllegalArgumentException("Plaintext cannot be null");
        }

        // Generate random IV
        byte[] iv = new byte[GCM_IV_LENGTH];
        secureRandom.nextBytes(iv);

        // Initialize cipher
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, getAesKey(), parameterSpec);

        // Encrypt
        byte[] ciphertext = cipher.doFinal(plaintext);

        // Prepend IV to ciphertext: [IV][Ciphertext+Tag]
        byte[] encryptedData = new byte[GCM_IV_LENGTH + ciphertext.length];
        System.arraycopy(iv, 0, encryptedData, 0, GCM_IV_LENGTH);
        System.arraycopy(ciphertext, 0, encryptedData, GCM_IV_LENGTH, ciphertext.length);

        return encryptedData;
    }

    /**
     * Decrypt ciphertext bytes with AES-256-GCM.
     * 
     * Input format: [IV][Ciphertext+Tag]
     * - IV: First 12 bytes
     * - Ciphertext+Tag: Remaining bytes
     * 
     * @param encryptedData Encrypted bytes (IV prepended)
     * @return Decrypted plaintext bytes
     * @throws Exception if decryption fails (wrong key, tampered data, etc.)
     */
    public static byte[] decrypt(byte[] encryptedData) throws Exception {
        if (encryptedData == null || encryptedData.length <= GCM_IV_LENGTH) {
            throw new IllegalArgumentException("Invalid encrypted data");
        }

        // Extract IV from first 12 bytes
        byte[] iv = Arrays.copyOfRange(encryptedData, 0, GCM_IV_LENGTH);

        // Extract ciphertext+tag from remaining bytes
        byte[] ciphertext = Arrays.copyOfRange(encryptedData, GCM_IV_LENGTH, encryptedData.length);

        // Initialize cipher
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, getAesKey(), parameterSpec);

        // Decrypt (will throw exception if authentication tag verification fails)
        return cipher.doFinal(ciphertext);
    }

    /**
     * Encrypt string to bytes.
     * Convenience method for encrypting text data.
     */
    public static byte[] encryptString(String plaintext) throws Exception {
        return encrypt(plaintext.getBytes("UTF-8"));
    }

    /**
     * Decrypt bytes to string.
     * Convenience method for decrypting text data.
     */
    public static String decryptString(byte[] encryptedData) throws Exception {
        byte[] decrypted = decrypt(encryptedData);
        return new String(decrypted, "UTF-8");
    }
}
