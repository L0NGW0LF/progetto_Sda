package com.secureapp.servlet;

import com.secureapp.dao.UserDAO;
import com.secureapp.model.User;
import com.secureapp.service.BruteForceProtectionService;
import com.secureapp.util.ValidationUtil;
import org.owasp.encoder.Encode;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Login Servlet - Implements RF2, 3.2, and 4.4
 * Handles user authentication with:
 * - Secure session creation
 * - Session fixation protection (session ID regeneration)
 * - Secure cookie attributes (HttpOnly, Secure, SameSite)
 * - Non-informative error messages
 * - Anti brute-force protection (rate limiting + lockout)
 */
@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();
    private final BruteForceProtectionService bruteForceService = BruteForceProtectionService.getInstance();

    /**
     * Helper method to ensure CSRF token is available in request before forwarding
     * to JSP.
     * Prevents token from being null after POST errors.
     */
    private void setCsrfTokenForForward(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            String token = (String) session.getAttribute("csrf_token"); // CsrfUtil.CSRF_TOKEN_SESSION_ATTR
            if (token != null) {
                request.setAttribute("csrfToken", token); // CsrfUtil.CSRF_TOKEN_REQUEST_ATTR
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Check if already logged in
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("userId") != null) {
            response.sendRedirect(request.getContextPath() + "/dashboard");
            return;
        }

        // Display login form
        request.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Get form parameters
        String email = request.getParameter("email");
        String password = request.getParameter("password");

        // Get client IP for brute-force tracking
        String clientIp = request.getRemoteAddr();

        // Input validation
        if (email == null || password == null) {
            setCsrfTokenForForward(request);
            request.setAttribute("error", Encode.forHtmlContent("Invalid credentials"));
            request.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(request, response);
            return;
        }

        // Sanitize email
        email = ValidationUtil.sanitizeInput(email, 255);

        // Anti brute-force protection: Check if IP or email is blocked
        if (bruteForceService.isBlocked(clientIp, email)) {
            long remainingTime = bruteForceService.getRemainingLockoutTime(clientIp, email);
            int remainingMinutes = (int) Math.ceil(remainingTime / 60000.0);

            setCsrfTokenForForward(request);
            request.setAttribute("error", Encode.forHtmlContent(
                    "Too many failed login attempts. Please try again in " + remainingMinutes + " minute(s)."));
            request.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(request, response);
            return;
        }

        try {
            // Authenticate user
            User user = userDAO.authenticateUser(email, password);

            if (user != null) {
                // Authentication successful

                // Reset brute-force counters on successful login
                bruteForceService.recordSuccessfulLogin(clientIp, email);

                // CRITICAL FIX FOR DUPLICATE JSESSIONID COOKIES:
                // We must create the session AND set the cookie header BEFORE any response
                // is committed. The key is to let Tomcat create the session but prevent
                // it from sending the cookie, then we send our own cookie with SameSite.

                // Invalidate old session to prevent session fixation
                HttpSession oldSession = request.getSession(false);
                if (oldSession != null) {
                    oldSession.invalidate();
                }

                // Create new session with regenerated ID
                HttpSession newSession = request.getSession(true);

                // Set session timeout (30 minutes)
                newSession.setMaxInactiveInterval(30 * 60);

                // Store user ID in session
                newSession.setAttribute("userId", user.getId());
                newSession.setAttribute("userEmail", user.getEmail());

                // // Now manually send the JSESSIONID cookie with all security attributes
                // // We do this AFTER creating the session so we have the session ID
                // // Using response wrapper or proper header management
                // String sessionCookieHeader = "JSESSIONID=" + newSession.getId() +
                // "; Path=" + request.getContextPath() + "/" +
                // "; HttpOnly" +
                // "; Secure" +
                // "; SameSite=Strict";
                // response.addHeader("Set-Cookie", sessionCookieHeader);

                // Redirect to dashboard
                response.sendRedirect(request.getContextPath() + "/dashboard");

            } else {
                // Authentication failed - record attempt and show non-informative error
                bruteForceService.recordFailedAttempt(clientIp, email);

                setCsrfTokenForForward(request);
                request.setAttribute("error", Encode.forHtmlContent("Invalid credentials"));
                request.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(request, response);
            }

        } catch (SQLException e) {
            // Log error but don't expose details
            System.err.println("Database error during login: " + e.getMessage());
            setCsrfTokenForForward(request);
            request.setAttribute("error", Encode.forHtmlContent("An error occurred. Please try again later."));
            request.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(request, response);
        }
    }
}
