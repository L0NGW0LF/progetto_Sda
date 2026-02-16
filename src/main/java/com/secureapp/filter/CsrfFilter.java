package com.secureapp.filter;

import com.secureapp.util.CsrfUtil;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * CSRF (Cross-Site Request Forgery) Protection Filter.
 * 
 * This filter implements CSRF protection for all HTTP requests:
 * - GET requests: Generates CSRF token if not present in session, makes it
 * available to JSP
 * - POST requests: Validates CSRF token before allowing request to proceed
 * 
 * Security rationale:
 * - Prevents attackers from forging state-changing requests from malicious
 * websites
 * - Token is session-bound, so attacker cannot reuse tokens from other sessions
 * - Combined with SameSite=Strict cookies for defense-in-depth
 * 
 * Applied to all URLs (/*) to protect all forms including login and
 * registration.
 */
@WebFilter("/*")
public class CsrfFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // No initialization needed
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String method = httpRequest.getMethod();

        if ("GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method)) {
            // For GET requests: ensure CSRF token exists in session
            handleGetRequest(httpRequest);

        } else if ("POST".equalsIgnoreCase(method)) {
            // For POST requests: validate CSRF token
            if (!validateCsrfToken(httpRequest)) {
                // CSRF validation failed - redirect to login with error
                handleCsrfFailure(httpRequest, httpResponse);
                return; // Stop filter chain
            }
        }

        // Continue to next filter or servlet
        chain.doFilter(request, response);
    }

    /**
     * Handle GET requests: ensure CSRF token exists and make it available to JSP.
     */
    private void handleGetRequest(HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        String token;

        if (session == null || CsrfUtil.getTokenFromSession(session) == null) {
            // No session or no token - generate new one
            token = CsrfUtil.generateAndStoreToken(request);
        } else {
            // Token already exists in session - reuse it
            token = CsrfUtil.getTokenFromSession(session);
        }

        // Make token available to JSP via request attribute
        request.setAttribute(CsrfUtil.CSRF_TOKEN_REQUEST_ATTR, token);
    }

    /**
     * Validate CSRF token for POST requests.
     * 
     * @return true if token is valid, false otherwise
     */
    private boolean validateCsrfToken(HttpServletRequest request) {
        return CsrfUtil.validateToken(request);
    }

    /**
     * Handle CSRF validation failure.
     * Redirects to appropriate page based on authentication status:
     * - Authenticated users: redirect to dashboard with error
     * - Unauthenticated users: redirect to login with error
     * 
     * Security note: Generic error message prevents information leakage about
     * whether session exists, token exists, or token is invalid.
     */
    private void handleCsrfFailure(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        // Log the failure (for security monitoring)
        String clientIp = request.getRemoteAddr();
        String requestUri = request.getRequestURI();
        System.err.println("CSRF validation failed - IP: " + clientIp +
                ", URI: " + requestUri);

        // Check if user is authenticated
        HttpSession session = request.getSession(false);
        boolean isAuthenticated = (session != null && session.getAttribute("userId") != null);

        if (isAuthenticated) {
            // User is logged in - redirect to dashboard with error
            response.sendRedirect(request.getContextPath() + "/dashboard?error=invalid_request");
        } else {
            // User is not logged in - redirect to login with error
            response.sendRedirect(request.getContextPath() + "/login?error=invalid_request");
        }
    }

    @Override
    public void destroy() {
        // No cleanup needed
    }
}
