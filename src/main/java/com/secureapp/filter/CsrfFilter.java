package com.secureapp.filter;

import com.secureapp.util.CsrfUtil;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/*
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
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String method = httpRequest.getMethod();
        // Ensure CSRF token exists in session with GET and HEAD requests
        if ("GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method)) {

            handleGetRequest(httpRequest);

            // Token validation for POST requests
        } else if ("POST".equalsIgnoreCase(method)) {
            if (!validateCsrfToken(httpRequest)) {
                handleCsrfFailure(httpRequest, httpResponse);
                return;
            }
        }

        chain.doFilter(request, response);
    }

    private void handleGetRequest(HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        String token;

        if (session == null || CsrfUtil.getTokenFromSession(session) == null) {
            token = CsrfUtil.generateAndStoreToken(request);
        } else {
            token = CsrfUtil.getTokenFromSession(session);
        }

        request.setAttribute(CsrfUtil.CSRF_TOKEN_REQUEST_ATTR, token);
    }

    /*
     * Validate CSRF token for POST requests.
     * 
     * @return true if token is valid, false otherwise
     */
    private boolean validateCsrfToken(HttpServletRequest request) {
        return CsrfUtil.validateToken(request);
    }

    /*
     * Handle CSRF validation failure.
     * Redirects to appropriate page based on authentication status:
     * - Authenticated users: redirect to dashboard with error
     * - Unauthenticated users: redirect to login with error
     * 
     * Generic error message prevents information leakage about
     * whether session exists, token exists, or token is invalid.
     */
    private void handleCsrfFailure(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String clientIp = request.getRemoteAddr();
        String requestUri = request.getRequestURI();
        System.err.println("CSRF validation failed - IP: " + clientIp +
                ", URI: " + requestUri);

        HttpSession session = request.getSession(false);
        boolean isAuthenticated = (session != null && session.getAttribute("userId") != null);

        if (isAuthenticated) {
            response.sendRedirect(request.getContextPath() + "/dashboard?error=invalid_request");
        } else {
            response.sendRedirect(request.getContextPath() + "/login?error=invalid_request");
        }
    }

    @Override
    public void destroy() {
    }
}
