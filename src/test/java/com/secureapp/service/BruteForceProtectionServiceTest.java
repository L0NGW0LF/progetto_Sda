package com.secureapp.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BruteForceProtectionService.
 * Tests rate limiting, lockout logic, and dual-key tracking (IP + email).
 */
class BruteForceProtectionServiceTest {

    private BruteForceProtectionService service;

    @BeforeEach
    void setUp() {
        service = BruteForceProtectionService.getInstance();
    }

    @AfterEach
    void tearDown() {
        // Reset service state by recording successful logins
        // This clears any test data from tracking maps
        service.recordSuccessfulLogin("192.168.1.1", "test@example.com");
        service.recordSuccessfulLogin("10.0.0.1", "other@example.com");
        service.recordSuccessfulLogin("203.0.113.5", "victim@example.com");
    }

    // ==================== Basic Functionality Tests ====================

    @Test
    void testIsBlocked_WithNoAttempts_ReturnsFalse() {
        boolean blocked = service.isBlocked("192.168.1.100", "newuser@test.com");
        assertFalse(blocked, "New IP/email should not be blocked");
    }

    @Test
    void testRecordFailedAttempt_IncrementsCounter() {
        String ip = "192.168.1.1";
        String email = "test@test.com";

        // Record 3 failed attempts
        for (int i = 0; i < 3; i++) {
            service.recordFailedAttempt(ip, email);
        }

        // Should not be blocked yet (max is 5)
        assertFalse(service.isBlocked(ip, email),
                "Should not be blocked after 3 attempts");
    }

    @Test
    void testRecordFailedAttempt_withNullIp() {
        String email = "test@test.com";
        String ip = null;

        service.recordFailedAttempt(ip, email);

    }

    @Test
    void testRecordSuccessfulLogin_ResetsCounters() {
        String ip = "192.168.1.2";
        String email = "test2@test.com";

        // Record some failed attempts
        service.recordFailedAttempt(ip, email);
        service.recordFailedAttempt(ip, email);

        // Successful login resets
        service.recordSuccessfulLogin(ip, email);

        // Verify reset by checking not blocked
        assertFalse(service.isBlocked(ip, email),
                "Successful login should reset counters");
    }

    // ==================== Lockout Tests ====================

    @Test
    void testIsBlocked_AfterMaxAttempts_ReturnsTrue() {
        String ip = "192.168.1.3";
        String email = "test3@test.com";

        for (int i = 0; i < 5; i++) {
            service.recordFailedAttempt(ip, email);
        }

        assertTrue(service.isBlocked(ip, email),
                "Should be blocked after 5 failed attempts");
    }

    @Test
    void testIsBlocked_AfterMoreThanMaxAttempts_RemainsBlocked() {
        String ip = "192.168.1.4";
        String email = "test4@test.com";

        // Record 10 failed attempts (more than max)
        for (int i = 0; i < 10; i++) {
            service.recordFailedAttempt(ip, email);
        }

        // Should still be blocked
        assertTrue(service.isBlocked(ip, email),
                "Should remain blocked even after excessive attempts");
    }

    @Test
    void testGetRemainingLockoutTime_WhenBlocked_ReturnsPositiveValue() {
        String ip = "192.168.1.5";
        String email = "test5@test.com";

        // Trigger lockout
        for (int i = 0; i < 5; i++) {
            service.recordFailedAttempt(ip, email);
        }

        long remainingTime = service.getRemainingLockoutTime(ip, email);

        assertTrue(remainingTime > 0, "Should have positive remaining lockout time");
        assertTrue(remainingTime <= 15 * 60 * 1000,
                "Remaining time should be at most 15 minutes");
    }

    @Test
    void testGetRemainingLockoutTime_WhenNotBlocked_ReturnsZero() {
        String ip = "192.168.1.6";
        String email = "test6@test.com";

        long remainingTime = service.getRemainingLockoutTime(ip, email);

        assertEquals(0, remainingTime, "Not blocked should return 0 remaining time");
    }

    // ==================== Dual-Key Tracking Tests ====================

    @Test
    void testDualKey_IPBlocked_AffectsAllEmails() {
        String blockedIp = "203.0.113.10";
        String email1 = "user1@test.com";
        String email2 = "user2@test.com";

        // Block the IP using email1
        for (int i = 0; i < 5; i++) {
            service.recordFailedAttempt(blockedIp, email1);
        }

        // IP should be blocked for any email
        assertTrue(service.isBlocked(blockedIp, email1),
                "Original email should be blocked");
        assertTrue(service.isBlocked(blockedIp, email2),
                "Different email from same IP should also be blocked");
    }

    @Test
    void testDualKey_EmailBlocked_AffectsAllIPs() {
        String blockedEmail = "victim@test.com";
        String ip1 = "10.0.0.1";
        String ip2 = "10.0.0.2";

        // Block the email using ip1
        for (int i = 0; i < 5; i++) {
            service.recordFailedAttempt(ip1, blockedEmail);
        }

        // Email should be blocked from any IP
        assertTrue(service.isBlocked(ip1, blockedEmail),
                "Original IP should be blocked");
        assertTrue(service.isBlocked(ip2, blockedEmail),
                "Same email from different IP should also be blocked");
    }

    @Test
    void testDualKey_IndependentTracking() {
        String ip1 = "192.168.1.10";
        String ip2 = "192.168.1.11";
        String email1 = "user1@independent.com";
        String email2 = "user2@independent.com";

        // Record attempts for IP1+email1
        for (int i = 0; i < 3; i++) {
            service.recordFailedAttempt(ip1, email1);
        }

        // IP2+email2 should not be affected
        assertFalse(service.isBlocked(ip2, email2),
                "Different IP and email combination should not be blocked");

        // But IP1 or email1 with different counterparts might be
        // (depends on if they reached threshold)
    }

    // ==================== Edge Cases ====================

    @Test
    void testIsBlocked_WithNullIP_ReturnsFalse() {
        boolean blocked = service.isBlocked(null, "test@test.com");
        assertFalse(blocked, "Null IP should return false (graceful handling)");
    }

    @Test
    void testIsBlocked_WithNullEmail_ReturnsFalse() {
        boolean blocked = service.isBlocked("192.168.1.1", null);
        assertFalse(blocked, "Null email should return false (graceful handling)");
    }

    @Test
    void testRecordFailedAttempt_WithNullValues_NoException() {
        // Should not throw exception
        assertDoesNotThrow(() -> {
            service.recordFailedAttempt(null, "test@test.com");
            service.recordFailedAttempt("192.168.1.1", null);
            service.recordFailedAttempt(null, null);
        }, "Should handle null values gracefully");
    }

    // ==================== Security Tests ====================

    @Test
    void testLockout_PreventsValidCredentials() {
        String ip = "192.168.1.20";
        String email = "legit@test.com";

        // Simulate brute-force attack
        for (int i = 0; i < 5; i++) {
            service.recordFailedAttempt(ip, email);
        }

        // Even if user now has correct credentials, they're still blocked
        assertTrue(service.isBlocked(ip, email),
                "Valid credentials should not bypass lockout");
    }

    @Test
    void testLockout_AfterReset_AllowsNewAttempts() {
        String ip = "192.168.1.21";
        String email = "reset@test.com";

        // Trigger lockout
        for (int i = 0; i < 5; i++) {
            service.recordFailedAttempt(ip, email);
        }
        assertTrue(service.isBlocked(ip, email));

        // Simulate successful login (e.g., after lockout expires or admin unlock)
        service.recordSuccessfulLogin(ip, email);

        // Should now be unblocked
        assertFalse(service.isBlocked(ip, email),
                "After reset, should allow new attempts");
    }

    @Test
    void testConcurrentAccess_ThreadSafe() throws InterruptedException {
        // Test thread safety with concurrent access
        String ip = "192.168.1.100";
        String email = "concurrent@test.com";

        // Create multiple threads that record failed attempts
        Thread[] threads = new Thread[10];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                service.recordFailedAttempt(ip, email);
            });
        }

        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }

        // After 10 concurrent attempts, should be blocked
        assertTrue(service.isBlocked(ip, email),
                "Should handle concurrent access correctly");
    }
}
