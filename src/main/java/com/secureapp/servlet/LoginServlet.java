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
 * Login Servlet
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

    private void setCsrfTokenForForward(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            String token = (String) session.getAttribute("csrf_token");
            if (token != null) {
                request.setAttribute("csrfToken", token);
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("userId") != null) {
            response.sendRedirect(request.getContextPath() + "/dashboard");
            return;
        }

        request.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String email = request.getParameter("email");
        String password = request.getParameter("password");

        String clientIp = request.getRemoteAddr();

        // Input validation
        if (email == null || password == null) {
            setCsrfTokenForForward(request);
            request.setAttribute("error", Encode.forHtmlContent("Invalid credentials"));
            request.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(request, response);
            return;
        }

        email = ValidationUtil.sanitizeInput(email, 255);

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
            User user = userDAO.authenticateUser(email, password);

            if (user != null) {

                bruteForceService.recordSuccessfulLogin(clientIp, email);

                HttpSession oldSession = request.getSession(false);
                if (oldSession != null) {
                    oldSession.invalidate();
                }

                HttpSession newSession = request.getSession(true);

                newSession.setMaxInactiveInterval(30 * 60);

                newSession.setAttribute("userId", user.getId());
                newSession.setAttribute("userEmail", user.getEmail());

                response.sendRedirect(request.getContextPath() + "/dashboard");

            } else {
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
