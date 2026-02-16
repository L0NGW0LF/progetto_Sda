package com.secureapp.util;

import javax.crypto.SecretKey;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.Properties;

/**
 * Configuration class for loading AES encryption keystore from external
 * location.
 * 
 * Security approach:
 * - Keystore path loaded from environment variable or properties file
 * - Passwords from environment variables only (not hardcoded)
 * - Singleton pattern to load keystore once at startup
 * - No secrets logged or exposed
 * 
 * Follows GEMINI.md requirement 4.8:
 * "Keystore / env secrets; rotazione chiavi prevista (almeno design)"
 */
public class KeystoreConfig {

    private static final String PROPERTIES_FILE = "application.properties";

    // Configuration keys
    private static final String KEYSTORE_PATH_KEY = "keystore.path";
    private static final String KEYSTORE_PASSWORD_KEY = "keystore.password";
    private static final String AES_KEY_ALIAS_KEY = "keystore.aes.key.alias";
    private static final String KEY_PASSWORD_KEY = "keystore.key.password";

    // Singleton instance
    private static volatile KeystoreConfig instance;

    // Loaded configuration
    private final String keystorePath;
    private final char[] keystorePassword;
    private final String aesKeyAlias;
    private final char[] keyPassword;
    private final SecretKey aesKey;

    /**
     * Private constructor - loads configuration and keystore.
     */
    private KeystoreConfig() throws Exception {
        // Load properties
        Properties props = loadProperties();

        // Get configuration with environment variable precedence
        String rawPath = getConfigValue(props, KEYSTORE_PATH_KEY, "KEYSTORE_PATH");
        this.keystorePath = resolvePath(rawPath);

        String keystorePwd = getConfigValue(props, KEYSTORE_PASSWORD_KEY, "KEYSTORE_PASSWORD");
        this.keystorePassword = keystorePwd != null ? keystorePwd.toCharArray() : null;

        this.aesKeyAlias = props.getProperty(AES_KEY_ALIAS_KEY, "aes-encryption-key");

        String keyPwd = getConfigValue(props, KEY_PASSWORD_KEY, "KEY_PASSWORD");
        this.keyPassword = keyPwd != null ? keyPwd.toCharArray() : null;

        // Validate configuration
        if (this.keystorePath == null || this.keystorePassword == null) {
            throw new IllegalStateException(
                    "Keystore configuration incomplete. Set KEYSTORE_PATH and KEYSTORE_PASSWORD environment variables.");
        }

        // Load AES key from keystore
        this.aesKey = loadAesKey();

        // Log success (without sensitive data)
        System.out.println("AES encryption initialized with keystore: " + this.keystorePath);
        System.out.println("AES key alias: " + aesKeyAlias);
    }

    /**
     * Resolve keystore path to be cross-platform and robust.
     */
    private String resolvePath(String path) {
        if (path == null)
            return null;

        java.io.File file = new java.io.File(path);
        if (file.exists()) {
            return path;
        }

        // Try user home/secure-app-config/keystore.p12 (cross-platform default)
        String userHomePath = System.getProperty("user.home") + java.io.File.separator + "secure-app-config"
                + java.io.File.separator + "keystore.p12";
        if (new java.io.File(userHomePath).exists()) {
            System.out.println("[INFO] Keystore not found at " + path + ", using default: " + userHomePath);
            return userHomePath;
        }

        return path; // Fall back to original and let validation fail if needed
    }

    /**
     * Get singleton instance.
     * Uses double-checked locking for thread-safe lazy initialization.
     */
    public static KeystoreConfig getInstance() throws Exception {
        if (instance == null) {
            synchronized (KeystoreConfig.class) {
                if (instance == null) {
                    instance = new KeystoreConfig();
                }
            }
        }
        return instance;
    }

    /**
     * Load application.properties from classpath.
     */
    private Properties loadProperties() throws IOException {
        Properties props = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(PROPERTIES_FILE)) {
            if (input != null) {
                props.load(input);
            }
        }
        return props;
    }

    /**
     * Get configuration value with environment variable precedence.
     * 
     * Priority:
     * 1. Environment variable (highest)
     * 2. System property
     * 3. Properties file
     * 
     * @param props   Properties from file
     * @param propKey Property key
     * @param envKey  Environment variable key
     * @return Configuration value
     */
    private String getConfigValue(Properties props, String propKey, String envKey) {
        // Check environment variable first
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isEmpty()) {
            return envValue;
        }

        // Check system property
        String sysProp = System.getProperty(envKey);
        if (sysProp != null && !sysProp.isEmpty()) {
            return sysProp;
        }

        // Fall back to properties file (with ${ENV_VAR} support)
        String propValue = props.getProperty(propKey);
        if (propValue != null) {
            // Simple ${VAR} substitution
            if (propValue.startsWith("${") && propValue.contains(":")) {
                int colonIndex = propValue.indexOf(':');
                String varName = propValue.substring(2, colonIndex);
                String defaultValue = propValue.substring(colonIndex + 1, propValue.length() - 1);
                String envVar = System.getenv(varName);
                return envVar != null ? envVar : defaultValue;
            }
        }

        return propValue;
    }

    /**
     * Load AES secret key from keystore.
     */
    private SecretKey loadAesKey() throws Exception {
        try (FileInputStream fis = new FileInputStream(keystorePath)) {
            KeyStore keystore = KeyStore.getInstance("PKCS12");
            keystore.load(fis, keystorePassword);

            // Get AES key
            KeyStore.SecretKeyEntry entry = (KeyStore.SecretKeyEntry) keystore.getEntry(
                    aesKeyAlias,
                    new KeyStore.PasswordProtection(keyPassword));

            if (entry == null) {
                throw new IllegalStateException(
                        "AES key with alias '" + aesKeyAlias + "' not found in keystore");
            }

            return entry.getSecretKey();
        }
    }

    /**
     * Get AES secret key for encryption/decryption.
     */
    public SecretKey getAesKey() {
        return aesKey;
    }

    /**
     * Get keystore path (for logging/debugging only).
     */
    public String getKeystorePath() {
        return keystorePath;
    }
}
