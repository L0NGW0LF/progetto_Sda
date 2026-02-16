package com.secureapp.service;

import com.secureapp.dao.UserDAO;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;

/**
 * Test class for ConcurrentUploadService - RF3.8 Concurrency Verification.
 * 
 * This test explicitly demonstrates:
 * 1. Multiple threads accessing shared file resources concurrently
 * 2. Proper synchronization preventing race conditions
 * 3. No file overwrites or interference between concurrent operations
 * 4. Consistent state maintenance across concurrent uploads
 * 
 * Educational Purpose: Verify understanding of concurrency problems and
 * proper use of synchronization mechanisms in Java.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConcurrentUploadServiceTest {

    private ConcurrentUploadService uploadService;
    private static final int CONCURRENT_UPLOADS = 10; // Reduced for more stable testing
    private static final int USER_ID = 1;
    private static final String UPLOAD_DIR = System.getProperty("user.home") + File.separator + "secure-app-uploads";

    @BeforeAll
    void setupService() throws SQLException {
        uploadService = ConcurrentUploadService.getInstance();

        // Create test user in database to satisfy foreign key constraint
        // This user is required for file uploads to work
        UserDAO userDAO = new UserDAO();
        try {
            // Create test user with ID=1 for concurrent upload tests
            userDAO.createUser("test@concurrency.test", "TestPassword123!");
        } catch (SQLException e) {
            // User might already exist, which is fine
            if (!e.getMessage().contains("Unique index or primary key violation")) {
                throw e;
            }
        }
    }

    @AfterAll
    void teardownService() {
        uploadService.shutdown();
    }

    @BeforeEach
    void cleanupBeforeTest() {
        // Clean up upload directory before each test
        File uploadDir = new File(UPLOAD_DIR);
        if (uploadDir.exists()) {
            File[] files = uploadDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
        }
    }

    /**
     * TEST 1: Concurrent Upload - No Race Conditions
     * 
     * Verifies that multiple threads uploading files simultaneously do not
     * produce race conditions or file name collisions.
     * 
     * Expected: All files have unique names, no overwrites occur.
     */
    @Test
    @DisplayName("Concurrent uploads produce unique filenames with no collisions")
    void testConcurrentUploads_NoRaceConditions() throws Exception {
        // Prepare test data
        int numThreads = CONCURRENT_UPLOADS;
        CountDownLatch startLatch = new CountDownLatch(1); // Synchronize start
        CountDownLatch doneLatch = new CountDownLatch(numThreads); // Wait for completion
        ConcurrentHashMap<String, Integer> filenameOccurrences = new ConcurrentHashMap<>();
        List<Future<String>> futures = new ArrayList<>();

        // Launch concurrent upload threads
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);

        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            Future<String> future = executor.submit(() -> {
                try {
                    // Wait for all threads to be ready (synchronize start)
                    startLatch.await();

                    // Prepare file content
                    String content = "Test file content from thread " + threadId;
                    byte[] fileContent = content.getBytes();
                    String originalFilename = "test_file_" + threadId + ".txt";

                    // Upload file concurrently
                    Future<String> uploadFuture = uploadService.processUploadAsync(
                            USER_ID,
                            originalFilename,
                            fileContent,
                            fileContent.length);

                    String storedFilename = uploadFuture.get(30, TimeUnit.SECONDS);

                    // Track filename occurrences (should be unique)
                    filenameOccurrences.merge(storedFilename, 1, Integer::sum);

                    return storedFilename;

                } catch (Exception e) {
                    // Print full exception for debugging
                    e.printStackTrace();
                    String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getName();
                    if (e.getCause() != null) {
                        errorMsg += " | Cause: " + e.getCause().toString();
                    }
                    fail("Upload failed in thread " + threadId + ": " + errorMsg);
                    return null;
                } finally {
                    doneLatch.countDown();
                }
            });

            futures.add(future);
        }

        // Start all threads simultaneously
        startLatch.countDown();

        // Wait for all uploads to complete (max 30 seconds)
        boolean completed = doneLatch.await(30, TimeUnit.SECONDS);
        assertTrue(completed, "Not all uploads completed in time");

        // Collect results
        Set<String> storedFilenames = new HashSet<>();
        for (Future<String> future : futures) {
            String filename = future.get(1, TimeUnit.SECONDS);
            assertNotNull(filename, "Stored filename should not be null");
            storedFilenames.add(filename);
        }

        // VERIFICATION 1: All filenames are unique (no race condition on naming)
        assertEquals(numThreads, storedFilenames.size(),
                "All uploaded files should have unique filenames - race condition detected!");

        // VERIFICATION 2: No filename was generated more than once
        for (Map.Entry<String, Integer> entry : filenameOccurrences.entrySet()) {
            assertEquals(1, entry.getValue(),
                    "Filename " + entry.getKey() + " was used " + entry.getValue() + " times - collision detected!");
        }

        // VERIFICATION 3: All files physically exist on disk
        File uploadDir = new File(UPLOAD_DIR);
        File[] filesOnDisk = uploadDir.listFiles();
        assertNotNull(filesOnDisk, "Upload directory should exist");
        assertEquals(numThreads, filesOnDisk.length,
                "Number of files on disk should match number of uploads");

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
    }

    /**
     * TEST 2: No File Overwrites
     * 
     * Verifies that concurrent operations do not overwrite each other's files.
     * Each thread uploads different content, and we verify all content is
     * preserved.
     */
    @Test
    @DisplayName("Concurrent uploads preserve all file contents without overwrites")
    void testConcurrentUploads_NoOverwrites() throws Exception {
        int numThreads = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numThreads);
        ConcurrentHashMap<String, String> expectedContent = new ConcurrentHashMap<>();

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);

        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();

                    // Each thread has unique content
                    String uniqueContent = "UNIQUE_CONTENT_FROM_THREAD_" + threadId + "_" + UUID.randomUUID();
                    byte[] fileContent = uniqueContent.getBytes();

                    Future<String> uploadFuture = uploadService.processUploadAsync(
                            USER_ID,
                            "overwrite_test_" + threadId + ".txt",
                            fileContent,
                            fileContent.length);

                    String storedFilename = uploadFuture.get(30, TimeUnit.SECONDS);
                    expectedContent.put(storedFilename, uniqueContent);

                } catch (Exception e) {
                    fail("Upload failed: " + e.getMessage());
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(30, TimeUnit.SECONDS), "Uploads did not complete in time");

        // VERIFICATION: Read back all files and verify content matches
        for (Map.Entry<String, String> entry : expectedContent.entrySet()) {
            String storedFilename = entry.getKey();
            String expectedText = entry.getValue();

            String actualContent = uploadService.getFileContent(storedFilename);
            assertTrue(actualContent.contains(expectedText),
                    "File content was overwritten or corrupted! Expected: " + expectedText);
        }

        executor.shutdown();
    }

    /**
     * TEST 3: Consistent State Under Concurrent Load
     * 
     * Verifies that the file system and internal state remain consistent
     * even under heavy concurrent load with rapid successive uploads.
     */
    @Test
    @DisplayName("System maintains consistent state under concurrent load")
    void testConcurrentUploads_ConsistentState() throws Exception {
        int numIterations = 5;
        int uploadsPerIteration = 5;

        for (int iteration = 0; iteration < numIterations; iteration++) {
            CountDownLatch iterationLatch = new CountDownLatch(uploadsPerIteration);
            Set<String> iterationFilenames = ConcurrentHashMap.newKeySet();

            ExecutorService executor = Executors.newFixedThreadPool(uploadsPerIteration);

            for (int i = 0; i < uploadsPerIteration; i++) {
                final int uploadId = iteration * uploadsPerIteration + i;
                executor.submit(() -> {
                    try {
                        byte[] content = ("Iteration content " + uploadId).getBytes();
                        Future<String> future = uploadService.processUploadAsync(
                                USER_ID,
                                "consistency_test_" + uploadId + ".txt",
                                content,
                                content.length);

                        String filename = future.get(30, TimeUnit.SECONDS);
                        iterationFilenames.add(filename);

                    } catch (Exception e) {
                        fail("Upload failed: " + e.getMessage());
                    } finally {
                        iterationLatch.countDown();
                    }
                });
            }

            assertTrue(iterationLatch.await(20, TimeUnit.SECONDS),
                    "Iteration " + iteration + " did not complete");

            // VERIFICATION: Each iteration produces unique filenames
            assertEquals(uploadsPerIteration, iterationFilenames.size(),
                    "Not all uploads in iteration " + iteration + " produced unique files");

            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }

        // FINAL VERIFICATION: Total file count
        File uploadDir = new File(UPLOAD_DIR);
        File[] allFiles = uploadDir.listFiles();
        assertNotNull(allFiles);
        assertEquals(numIterations * uploadsPerIteration, allFiles.length,
                "Total number of files should match total uploads across all iterations");
    }
}
