package com.secureapp.util;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.UUID;

/**
 * Utility class for CSRF (Cross-Site Request Forgery) protection.
 * Implements token-based CSRF protection using session-bound tokens.
 * 
 * Security approach:
 * - Generates cryptographically strong random UUID tokens
 * - Stores token in HTTP session (server-side)
 * - Validates incoming requests by comparing request parameter with session
 * token
 * - Tokens are bound to user session, preventing cross-session attacks
 */
public class CsrfUtil {

    /**
     * Session attribute name for storing CSRF token.
     */
    public static final String CSRF_TOKEN_SESSION_ATTR = "csrf_token";

    /**
     * Request parameter name for CSRF token in forms.
     */
    public static final String CSRF_TOKEN_PARAM = "csrf_token";

    /**
     * Request attribute name for passing token to JSP.
     */
    public static final String CSRF_TOKEN_REQUEST_ATTR = "csrfToken";

    /**
     * Generate a new CSRF token using cryptographically strong random UUID.
     * 
     * @return A new random UUID token as string
     */
    public static String generateToken() {
        return UUID.randomUUID().toString();
    }

    /**
     * Get CSRF token from session. If no token exists, returns null.
     * 
     * @param session HTTP session
     * @return CSRF token from session, or null if not present
     */
    public static String getTokenFromSession(HttpSession session) {
        if (session == null) {
            return null;
        }
        Object token = session.getAttribute(CSRF_TOKEN_SESSION_ATTR);
        return token != null ? token.toString() : null;
    }

    /**
     * Store CSRF token in session.
     * 
     * @param session HTTP session
     * @param token   CSRF token to store
     */
    public static void setTokenInSession(HttpSession session, String token) {
        if (session != null && token != null) {
            session.setAttribute(CSRF_TOKEN_SESSION_ATTR, token);
        }
    }

    /**
     * Validate CSRF token from request against session token.
     * 
     * Validation rules:
     * - Request must have csrf_token parameter
     * - Session must exist and contain csrf_token attribute
     * - Request token must exactly match session token
     * 
     * @param request HTTP request containing token parameter
     * @return true if token is valid, false otherwise
     */
    public static boolean validateToken(HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        if (session == null) {
            return false;
        }

        String requestToken = request.getParameter(CSRF_TOKEN_PARAM);

        String sessionToken = getTokenFromSession(session);
        if (requestToken == null || sessionToken == null) {
            return false;
        }

        return requestToken.equals(sessionToken);
    }

    /**
     * Generate and store a new CSRF token in the session.
     * If session doesn't exist, creates a new one.
     * 
     * @param request HTTP request
     * @return The generated token
     */
    public static String generateAndStoreToken(HttpServletRequest request) {
        HttpSession session = request.getSession(true);
        String token = generateToken();
        setTokenInSession(session, token);
        return token;
    }
}
