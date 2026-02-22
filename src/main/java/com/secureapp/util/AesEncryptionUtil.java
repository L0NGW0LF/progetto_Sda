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
 */
public class AesEncryptionUtil {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    private static final SecureRandom secureRandom = new SecureRandom();

    private static SecretKey aesKey;

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

        byte[] iv = new byte[GCM_IV_LENGTH];
        secureRandom.nextBytes(iv);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, getAesKey(), parameterSpec);

        byte[] ciphertext = cipher.doFinal(plaintext);

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

        byte[] iv = Arrays.copyOfRange(encryptedData, 0, GCM_IV_LENGTH);

        byte[] ciphertext = Arrays.copyOfRange(encryptedData, GCM_IV_LENGTH, encryptedData.length);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, getAesKey(), parameterSpec);

        return cipher.doFinal(ciphertext);
    }

    // Encrypt string to bytes.
    public static byte[] encryptString(String plaintext) throws Exception {
        return encrypt(plaintext.getBytes("UTF-8"));
    }

    // Decrypt bytes to string.
    public static String decryptString(byte[] encryptedData) throws Exception {
        byte[] decrypted = decrypt(encryptedData);
        return new String(decrypted, "UTF-8");
    }
}
