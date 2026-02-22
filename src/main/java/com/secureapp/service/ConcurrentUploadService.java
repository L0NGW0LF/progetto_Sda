package com.secureapp.service;

import com.secureapp.dao.FileDAO;
import com.secureapp.model.FileModel;
import com.secureapp.util.AesEncryptionUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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
 * Manages concurrent access to file resources using:
 * - ReentrantLock for synchronized access to shared resources
 * - ExecutorService for concurrent file processing
 * - AtomicLong for thread-safe file naming counter
 * 
 * This service prevents race conditions, file overwrites, and inconsistent
 * states during concurrent upload operations.
 */
public class ConcurrentUploadService {

    private static final String UPLOAD_DIR = System.getProperty("user.home") + File.separator + "secure-app-uploads";

    private final ExecutorService executorService;
    private final ReentrantLock fileSystemLock;
    private final AtomicLong fileCounter;
    private static volatile ConcurrentUploadService instance;

    private ConcurrentUploadService() {
        this.executorService = Executors.newFixedThreadPool(5);
        this.fileSystemLock = new ReentrantLock(true);
        this.fileCounter = new AtomicLong(System.currentTimeMillis());

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

        fileSystemLock.lock();
        try {
            long uniqueId = fileCounter.incrementAndGet();
            storedFilename = "file_" + uniqueId + ".txt";

            Path filePath = Paths.get(UPLOAD_DIR, storedFilename);
            targetFile = filePath.toFile();

            if (targetFile.exists()) {
                throw new IOException("File already exists: " + storedFilename);
            }
            byte[] encryptedContent = AesEncryptionUtil.encrypt(fileContent);

            try (FileOutputStream fos = new FileOutputStream(targetFile)) {
                fos.write(encryptedContent);
                fos.flush();
            }

            if (!targetFile.exists()) {
                throw new IOException("File write failed - file does not exist");
            }

        } finally {
            fileSystemLock.unlock();
        }

        FileModel fileModel = new FileModel(userId, originalFilename, storedFilename, fileSize);
        FileDAO fileDAO = new FileDAO();

        try {
            boolean saved = fileDAO.saveFile(fileModel);
            if (!saved) {
                if (targetFile != null && targetFile.exists()) {
                    targetFile.delete();
                }
                throw new SQLException("Failed to save file metadata to database");
            }
        } catch (SQLException e) {
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

        byte[] encryptedContent = Files.readAllBytes(filePath);
        byte[] decryptedContent = AesEncryptionUtil.decrypt(encryptedContent);
        return new String(decryptedContent, "UTF-8");
    }

    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}
