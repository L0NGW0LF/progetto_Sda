package com.secureapp.service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Brute-Force Protection Service - Implements requirement 4.4
 * Provides rate limiting and account lockout to prevent credential guessing
 * attacks.
 * 
 * Features:
 * - Dual-key tracking: IP address AND email address
 * - Configurable max attempts and lockout duration
 * - Automatic cleanup of old entries
 * - Thread-safe concurrent access
 * 
 * Security Strategy:
 * - Blocks attacks from single IP trying multiple accounts
 * - Blocks attacks from multiple IPs targeting single account (botnet)
 * - Temporary lockout (15 min) instead of permanent ban (usability)
 */
public class BruteForceProtectionService {

    // Configuration constants
    private static final int MAX_ATTEMPTS = 5; // Max failed login attempts before lockout
    private static final long LOCKOUT_DURATION_MS = 15 * 60 * 1000; // 15 minutes lockout
    private static final long CLEANUP_INTERVAL_MS = 60 * 60 * 1000; // Cleanup every 1 hour

    // Tracking maps for failed attempts
    private final ConcurrentHashMap<String, LoginAttemptInfo> ipAttempts;
    private final ConcurrentHashMap<String, LoginAttemptInfo> emailAttempts;

    // Scheduler for automatic cleanup
    private final ScheduledExecutorService cleanupScheduler;

    // Singleton instance
    private static volatile BruteForceProtectionService instance;

    /**
     * Get singleton instance.
     */
    public static BruteForceProtectionService getInstance() {
        if (instance == null) {
            synchronized (BruteForceProtectionService.class) {
                if (instance == null) {
                    instance = new BruteForceProtectionService();
                }
            }
        }
        return instance;
    }

    /**
     * Private constructor for singleton.
     */
    private BruteForceProtectionService() {
        this.ipAttempts = new ConcurrentHashMap<>();
        this.emailAttempts = new ConcurrentHashMap<>();

        // Start cleanup scheduler
        this.cleanupScheduler = Executors.newSingleThreadScheduledExecutor();
        this.cleanupScheduler.scheduleAtFixedRate(
                this::cleanupOldEntries,
                CLEANUP_INTERVAL_MS,
                CLEANUP_INTERVAL_MS,
                TimeUnit.MILLISECONDS);
    }

    /**
     * Check if IP or email is currently blocked due to too many failed attempts.
     * 
     * @param ip    Client IP address
     * @param email Email address attempting to login
     * @return true if blocked (either IP or email is locked out)
     */
    public boolean isBlocked(String ip, String email) {
        if (ip == null || email == null) {
            return false;
        }

        return isIpBlocked(ip) || isEmailBlocked(email);
    }

    /**
     * Check if specific IP is blocked.
     */
    private boolean isIpBlocked(String ip) {
        LoginAttemptInfo info = ipAttempts.get(ip);
        if (info == null) {
            return false;
        }

        long now = System.currentTimeMillis();

        // Check if lockout period has expired
        if (info.lockoutUntil > 0 && now < info.lockoutUntil) {
            return true; // Still locked out
        }

        // Lockout expired, reset
        if (info.lockoutUntil > 0 && now >= info.lockoutUntil) {
            ipAttempts.remove(ip);
            return false;
        }

        return false;
    }

    /**
     * Check if specific email is blocked.
     */
    private boolean isEmailBlocked(String email) {
        LoginAttemptInfo info = emailAttempts.get(email);
        if (info == null) {
            return false;
        }

        long now = System.currentTimeMillis();

        // Check if lockout period has expired
        if (info.lockoutUntil > 0 && now < info.lockoutUntil) {
            return true; // Still locked out
        }

        // Lockout expired, reset
        if (info.lockoutUntil > 0 && now >= info.lockoutUntil) {
            emailAttempts.remove(email);
            return false;
        }

        return false;
    }

    /**
     * Record a failed login attempt.
     * Increments attempt counter and triggers lockout if max attempts exceeded.
     * 
     * @param ip    Client IP address
     * @param email Email address that failed authentication
     */
    public void recordFailedAttempt(String ip, String email) {
        if (ip == null || email == null) {
            return;
        }

        long now = System.currentTimeMillis();

        // Record for IP
        LoginAttemptInfo ipInfo = ipAttempts.computeIfAbsent(ip, k -> new LoginAttemptInfo());
        ipInfo.failedAttempts++;
        ipInfo.lastAttemptTime = now;

        // Record for email
        LoginAttemptInfo emailInfo = emailAttempts.computeIfAbsent(email, k -> new LoginAttemptInfo());
        emailInfo.failedAttempts++;
        emailInfo.lastAttemptTime = now;

        // Check if lockout should be triggered
        if (ipInfo.failedAttempts >= MAX_ATTEMPTS) {
            ipInfo.lockoutUntil = now + LOCKOUT_DURATION_MS;
            System.err.println(String.format(
                    "SECURITY ALERT: IP locked out - IP: %s, Attempts: %d, Lockout until: %d min from now",
                    ip, ipInfo.failedAttempts, LOCKOUT_DURATION_MS / 60000));
        }

        if (emailInfo.failedAttempts >= MAX_ATTEMPTS) {
            emailInfo.lockoutUntil = now + LOCKOUT_DURATION_MS;
            System.err.println(String.format(
                    "SECURITY ALERT: Email locked out - Email: %s, Attempts: %d, Lockout until: %d min from now",
                    email, emailInfo.failedAttempts, LOCKOUT_DURATION_MS / 60000));
        }

        // Log failed attempt (always, for monitoring)
        System.err.println(String.format(
                "Failed login attempt - IP: %s, Email: %s, IP Attempts: %d/%d, Email Attempts: %d/%d",
                ip, email, ipInfo.failedAttempts, MAX_ATTEMPTS, emailInfo.failedAttempts, MAX_ATTEMPTS));
    }

    /**
     * Record a successful login.
     * Resets attempt counters for IP and email.
     * 
     * @param ip    Client IP address
     * @param email Email address that authenticated successfully
     */
    public void recordSuccessfulLogin(String ip, String email) {
        if (ip == null || email == null) {
            return;
        }

        // Remove from tracking maps (reset counters)
        ipAttempts.remove(ip);
        emailAttempts.remove(email);

        System.out.println(String.format(
                "Successful login - IP: %s, Email: %s (counters reset)",
                ip, email));
    }

    /**
     * Cleanup old entries that are no longer needed.
     * Removes entries where lockout has expired and no recent activity.
     * Runs periodically via scheduled task.
     */
    private void cleanupOldEntries() {
        long now = System.currentTimeMillis();
        int removedIp = 0;
        int removedEmail = 0;

        // Remove old IP entries
        removedIp = ipAttempts.size();
        ipAttempts.entrySet().removeIf(entry -> {
            LoginAttemptInfo info = entry.getValue();
            // Remove if lockout expired and no activity for lockout duration
            return (info.lockoutUntil > 0 && now > info.lockoutUntil) ||
                    (now - info.lastAttemptTime > LOCKOUT_DURATION_MS);
        });
        removedIp -= ipAttempts.size();

        // Remove old email entries
        removedEmail = emailAttempts.size();
        emailAttempts.entrySet().removeIf(entry -> {
            LoginAttemptInfo info = entry.getValue();
            return (info.lockoutUntil > 0 && now > info.lockoutUntil) ||
                    (now - info.lastAttemptTime > LOCKOUT_DURATION_MS);
        });
        removedEmail -= emailAttempts.size();

        if (removedIp > 0 || removedEmail > 0) {
            System.out.println(String.format(
                    "Brute-force protection cleanup: Removed %d IP entries, %d email entries",
                    removedIp, removedEmail));
        }
    }

    /**
     * Get remaining lockout time for IP.
     * 
     * @return milliseconds until lockout expires, or 0 if not locked
     */
    public long getRemainingLockoutTime(String ip, String email) {
        long now = System.currentTimeMillis();
        long maxLockout = 0;

        if (ip != null) {
            LoginAttemptInfo ipInfo = ipAttempts.get(ip);
            if (ipInfo != null && ipInfo.lockoutUntil > now) {
                maxLockout = Math.max(maxLockout, ipInfo.lockoutUntil - now);
            }
        }

        if (email != null) {
            LoginAttemptInfo emailInfo = emailAttempts.get(email);
            if (emailInfo != null && emailInfo.lockoutUntil > now) {
                maxLockout = Math.max(maxLockout, emailInfo.lockoutUntil - now);
            }
        }

        return maxLockout;
    }

    /**
     * Shutdown cleanup scheduler.
     * Should be called when application is shutting down.
     */
    public void shutdown() {
        if (cleanupScheduler != null && !cleanupScheduler.isShutdown()) {
            cleanupScheduler.shutdown();
            try {
                if (!cleanupScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    cleanupScheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                cleanupScheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Inner class to track login attempt information.
     */
    private static class LoginAttemptInfo {
        int failedAttempts = 0;
        long lastAttemptTime = System.currentTimeMillis();
        long lockoutUntil = 0; // 0 means not locked out
    }
}
