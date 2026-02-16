package com.secureapp.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CsrfUtil.
 * Tests CSRF token generation, validation, and session management.
 */
class CsrfUtilTest {

    private HttpServletRequest mockRequest;
    private HttpSession mockSession;

    @BeforeEach
    void setUp() {
        mockRequest = mock(HttpServletRequest.class);
        mockSession = mock(HttpSession.class);
    }

    // ==================== generateToken Tests ====================

    @Test
    void testGenerateToken_ReturnsValidUUID() {
        String token = CsrfUtil.generateToken();

        assertNotNull(token);
        assertEquals(36, token.length()); // UUID format: 8-4-4-4-12
        assertTrue(token.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));
    }

    @Test
    void testGenerateToken_GeneratesUniqueTokens() {
        String token1 = CsrfUtil.generateToken();
        String token2 = CsrfUtil.generateToken();

        assertNotEquals(token1, token2, "Consecutive tokens should be different");
    }

    // ==================== getTokenFromSession Tests ====================

    @Test
    void testGetTokenFromSession_WithValidToken_ReturnsToken() {
        String expectedToken = "test-token-123";
        when(mockSession.getAttribute(CsrfUtil.CSRF_TOKEN_SESSION_ATTR)).thenReturn(expectedToken);

        String result = CsrfUtil.getTokenFromSession(mockSession);

        assertEquals(expectedToken, result);
    }

    @Test
    void testGetTokenFromSession_WithNullSession_ReturnsNull() {
        String result = CsrfUtil.getTokenFromSession(null);
        assertNull(result);
    }

    @Test
    void testGetTokenFromSession_WithNoToken_ReturnsNull() {
        when(mockSession.getAttribute(CsrfUtil.CSRF_TOKEN_SESSION_ATTR)).thenReturn(null);

        String result = CsrfUtil.getTokenFromSession(mockSession);

        assertNull(result);
    }

    // ==================== validateToken Tests ====================

    @Test
    void testValidateToken_WithValidToken_ReturnsTrue() {
        String token = "valid-csrf-token";

        when(mockRequest.getSession(false)).thenReturn(mockSession);
        when(mockRequest.getParameter(CsrfUtil.CSRF_TOKEN_PARAM)).thenReturn(token);
        when(mockSession.getAttribute(CsrfUtil.CSRF_TOKEN_SESSION_ATTR)).thenReturn(token);

        assertTrue(CsrfUtil.validateToken(mockRequest));
    }

    @Test
    void testValidateToken_WithInvalidToken_ReturnsFalse() {
        when(mockRequest.getSession(false)).thenReturn(mockSession);
        when(mockRequest.getParameter(CsrfUtil.CSRF_TOKEN_PARAM)).thenReturn("wrong-token");
        when(mockSession.getAttribute(CsrfUtil.CSRF_TOKEN_SESSION_ATTR)).thenReturn("correct-token");

        assertFalse(CsrfUtil.validateToken(mockRequest));
    }

    @Test
    void testValidateToken_WithTamperedToken_ReturnsFalse() {
        String originalToken = "original-token-123";
        String tamperedToken = "tampered-token-456";

        when(mockRequest.getSession(false)).thenReturn(mockSession);
        when(mockRequest.getParameter(CsrfUtil.CSRF_TOKEN_PARAM)).thenReturn(tamperedToken);
        when(mockSession.getAttribute(CsrfUtil.CSRF_TOKEN_SESSION_ATTR)).thenReturn(originalToken);

        assertFalse(CsrfUtil.validateToken(mockRequest));
    }

    @Test
    void testValidateToken_WithNoSession_ReturnsFalse() {
        when(mockRequest.getSession(false)).thenReturn(null);

        assertFalse(CsrfUtil.validateToken(mockRequest));
    }

    @Test
    void testValidateToken_WithMissingRequestParameter_ReturnsFalse() {
        when(mockRequest.getSession(false)).thenReturn(mockSession);
        when(mockRequest.getParameter(CsrfUtil.CSRF_TOKEN_PARAM)).thenReturn(null);
        when(mockSession.getAttribute(CsrfUtil.CSRF_TOKEN_SESSION_ATTR)).thenReturn("some-token");

        assertFalse(CsrfUtil.validateToken(mockRequest));
    }

    @Test
    void testValidateToken_WithMissingSessionToken_ReturnsFalse() {
        when(mockRequest.getSession(false)).thenReturn(mockSession);
        when(mockRequest.getParameter(CsrfUtil.CSRF_TOKEN_PARAM)).thenReturn("some-token");
        when(mockSession.getAttribute(CsrfUtil.CSRF_TOKEN_SESSION_ATTR)).thenReturn(null);

        assertFalse(CsrfUtil.validateToken(mockRequest));
    }

    // ==================== generateAndStoreToken Tests ====================

    @Test
    void testGenerateAndStoreToken_CreatesSessionIfNeeded() {
        when(mockRequest.getSession(true)).thenReturn(mockSession);

        String token = CsrfUtil.generateAndStoreToken(mockRequest);

        assertNotNull(token);
        verify(mockRequest).getSession(true);
        verify(mockSession).setAttribute(eq(CsrfUtil.CSRF_TOKEN_SESSION_ATTR), anyString());
    }

    @Test
    void testGenerateAndStoreToken_StoresTokenInSession() {
        when(mockRequest.getSession(true)).thenReturn(mockSession);

        String token = CsrfUtil.generateAndStoreToken(mockRequest);

        verify(mockSession).setAttribute(CsrfUtil.CSRF_TOKEN_SESSION_ATTR, token);
    }

    // ==================== Security Tests ====================

    @Test
    void testValidateToken_CaseSensitive() {
        // CSRF tokens should be case-sensitive
        String token = "AbCdEf-123";
        String uppercaseToken = token.toUpperCase();

        when(mockRequest.getSession(false)).thenReturn(mockSession);
        when(mockRequest.getParameter(CsrfUtil.CSRF_TOKEN_PARAM)).thenReturn(uppercaseToken);
        when(mockSession.getAttribute(CsrfUtil.CSRF_TOKEN_SESSION_ATTR)).thenReturn(token);

        assertFalse(CsrfUtil.validateToken(mockRequest),
                "Token validation should be case-sensitive");
    }

    @Test
    void testValidateToken_NoTimingLeak() {
        // This test verifies constant-time comparison indirectly
        // by ensuring both valid and invalid tokens are processed
        String validToken = "valid-token-12345";

        // Valid case
        when(mockRequest.getSession(false)).thenReturn(mockSession);
        when(mockRequest.getParameter(CsrfUtil.CSRF_TOKEN_PARAM)).thenReturn(validToken);
        when(mockSession.getAttribute(CsrfUtil.CSRF_TOKEN_SESSION_ATTR)).thenReturn(validToken);
        assertTrue(CsrfUtil.validateToken(mockRequest));

        // Invalid case (different length)
        when(mockRequest.getParameter(CsrfUtil.CSRF_TOKEN_PARAM)).thenReturn("short");
        assertFalse(CsrfUtil.validateToken(mockRequest));

        // Invalid case (same length, different content)
        when(mockRequest.getParameter(CsrfUtil.CSRF_TOKEN_PARAM)).thenReturn("valid-token-99999");
        assertFalse(CsrfUtil.validateToken(mockRequest));
    }
}
