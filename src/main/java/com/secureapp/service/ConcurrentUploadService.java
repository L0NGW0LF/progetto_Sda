package com.secureapp.service;

import com.secureapp.dao.FileDAO;
import com.secureapp.model.FileModel;
import com.secureapp.util.AesEncryptionUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Concurrent file upload service with explicit thread management and
 * synchronization.
 * Implements RF3.8 - Manages concurrent access to file resources using:
 * - ReentrantLock for synchronized access to shared resources
 * - ExecutorService for concurrent file processing
 * - AtomicLong for thread-safe file naming counter
 * 
 * This service prevents race conditions, file overwrites, and inconsistent
 * states
 * during concurrent upload operations.
 */
public class ConcurrentUploadService {

    // Upload directory path
    private static final String UPLOAD_DIR = System.getProperty("user.home") + File.separator + "secure-app-uploads";

    // Thread pool for concurrent file processing
    private final ExecutorService executorService;

    // Lock for synchronizing access to file system resources
    private final ReentrantLock fileSystemLock;

    // Atomic counter for generating unique filenames
    private final AtomicLong fileCounter;

    // Singleton instance
    private static volatile ConcurrentUploadService instance;

    /**
     * Private constructor implementing singleton pattern.
     */
    private ConcurrentUploadService() {
        // Create thread pool with fixed number of threads
        this.executorService = Executors.newFixedThreadPool(5);
        this.fileSystemLock = new ReentrantLock(true); // Fair lock
        this.fileCounter = new AtomicLong(System.currentTimeMillis());

        // Initialize upload directory
        initializeUploadDirectory();
    }

    /**
     * Get singleton instance.
     * Uses double-checked locking for thread-safe lazy initialization.
     */
    public static ConcurrentUploadService getInstance() {
        if (instance == null) {
            synchronized (ConcurrentUploadService.class) {
                if (instance == null) {
                    instance = new ConcurrentUploadService();
                }
            }
        }
        return instance;
    }

    /**
     * Initialize upload directory if it doesn't exist.
     * Synchronized to prevent race condition during directory creation.
     */
    private void initializeUploadDirectory() {
        fileSystemLock.lock();
        try {
            File uploadDir = new File(UPLOAD_DIR);
            if (!uploadDir.exists()) {
                boolean created = uploadDir.mkdirs();
                if (!created) {
                    throw new RuntimeException("Failed to create upload directory: " + UPLOAD_DIR);
                }
            }
        } finally {
            fileSystemLock.unlock();
        }
    }

    /**
     * Process file upload concurrently.
     * This method executes the file save operation in a separate thread,
     * with proper synchronization to prevent TOCTOU and race conditions.
     * 
     * @param userId           User ID
     * @param originalFilename Original filename
     * @param fileContent      File content as bytes
     * @param fileSize         File size
     * @return Future<String> containing stored filename
     */
    public Future<String> processUploadAsync(int userId, String originalFilename,
            byte[] fileContent, long fileSize) {
        return executorService.submit(() -> {
            return saveFileSecurely(userId, originalFilename, fileContent, fileSize);
        });
    }

    /**
     * Save file securely with proper synchronization.
     * Critical section protected by ReentrantLock to prevent:
     * - Concurrent access to file naming
     * - Race conditions in file creation
     * - TOCTOU vulnerabilities
     * 
     * @param userId           User ID
     * @param originalFilename Original filename
     * @param fileContent      File content
     * @param fileSize         File size
     * @return Stored filename
     * @throws Exception if save fails
     */
    private String saveFileSecurely(int userId, String originalFilename,
            byte[] fileContent, long fileSize) throws Exception {
        String storedFilename = null;
        File targetFile = null;

        // CRITICAL SECTION: Lock to prevent race conditions
        fileSystemLock.lock();
        try {
            // Generate unique filename using atomic counter
            long uniqueId = fileCounter.incrementAndGet();
            storedFilename = "file_" + uniqueId + ".txt";

            // Create file path
            Path filePath = Paths.get(UPLOAD_DIR, storedFilename);
            targetFile = filePath.toFile();

            // Check if file already exists (should not happen with atomic counter)
            if (targetFile.exists()) {
                throw new IOException("File already exists: " + storedFilename);
            }

            // **AES ENCRYPTION: Encrypt file content before saving**
            // Follows GEMINI.md 4.8: "AES per cifrare dati quando richiesto"
            byte[] encryptedContent = AesEncryptionUtil.encrypt(fileContent);

            // Write encrypted content to disk
            try (FileOutputStream fos = new FileOutputStream(targetFile)) {
                fos.write(encryptedContent);
                fos.flush();
            }

            // Verify file was written correctly
            // Note: Encrypted file size will be larger than original (IV + auth tag
            // overhead)
            if (!targetFile.exists()) {
                throw new IOException("File write failed - file does not exist");
            }

        } finally {
            fileSystemLock.unlock();
        }

        // Save metadata to database (outside critical section to minimize lock time)
        FileModel fileModel = new FileModel(userId, originalFilename, storedFilename, fileSize);
        FileDAO fileDAO = new FileDAO();

        try {
            boolean saved = fileDAO.saveFile(fileModel);
            if (!saved) {
                // Rollback: delete file if database save fails
                if (targetFile != null && targetFile.exists()) {
                    targetFile.delete();
                }
                throw new SQLException("Failed to save file metadata to database");
            }
        } catch (SQLException e) {
            // Rollback: delete file if database error occurs
            if (targetFile != null && targetFile.exists()) {
                targetFile.delete();
            }
            throw e;
        }

        return storedFilename;
    }

    /**
     * Get file content for viewing.
     * Reads encrypted file from disk and decrypts it before returning.
     * 
     * @param storedFilename Stored filename
     * @return Decrypted file content as string
     * @throws Exception if file read or decryption fails
     */
    public String getFileContent(String storedFilename) throws Exception {
        Path filePath = Paths.get(UPLOAD_DIR, storedFilename);
        File file = filePath.toFile();

        if (!file.exists()) {
            throw new IOException("File not found: " + storedFilename);
        }

        // Read encrypted file content from disk
        byte[] encryptedContent = Files.readAllBytes(filePath);
        // **AES DECRYPTION: Decrypt file content after reading**
        byte[] decryptedContent = AesEncryptionUtil.decrypt(encryptedContent);
        return new String(decryptedContent, "UTF-8");
    }

    /**
     * Shutdown the executor service gracefully.
     */
    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}
