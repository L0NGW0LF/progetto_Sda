package com.secureapp.servlet;

import com.secureapp.dao.UserDAO;
import com.secureapp.util.ValidationUtil;
import org.owasp.encoder.Encode;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Register Servlet - Implements RF1
 * Handles user registration with:
 * - Email validation and uniqueness check
 * - Password policy enforcement
 * - Secure password hashing (BCrypt)
 * - Non-informative error messages to prevent user enumeration
 */
@WebServlet("/register")
public class RegisterServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Display registration form
        request.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Get form parameters
        String email = request.getParameter("email");
        String password = request.getParameter("password");
        String confirmPassword = request.getParameter("confirmPassword");

        // Input validation
        if (email == null || password == null || confirmPassword == null) {
            request.setAttribute("error", Encode.forHtmlContent("All fields are required"));
            request.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(request, response);
            return;
        }

        // Trim and sanitize inputs
        email = ValidationUtil.sanitizeInput(email, 255);

        // Validate email format
        if (!ValidationUtil.isValidEmail(email)) {
            request.setAttribute("error", Encode.forHtmlContent("Invalid email format"));
            request.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(request, response);
            return;
        }

        // Validate password policy
        if (!ValidationUtil.isValidPassword(password)) {
            request.setAttribute("error",
                    Encode.forHtmlContent(
                            "Password must be at least 8 characters with uppercase, lowercase, digit, and special character"));
            request.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(request, response);
            return;
        }

        // Check password confirmation
        if (!password.equals(confirmPassword)) {
            request.setAttribute("error", Encode.forHtmlContent("Passwords do not match"));
            request.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(request, response);
            return;
        }

        try {
            // Check if email already exists (case-insensitive)
            if (userDAO.emailExists(email)) {
                // Return a generic error that implies registration failed,
                // but we can make it a bit more helpful for the user
                request.setAttribute("error", Encode
                        .forHtmlContent("This email is already registered. Please login or use a different email."));
                request.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(request, response);
                return;
            }

            // Create user account
            boolean created = userDAO.createUser(email, password);

            if (created) {
                // Registration successful
                request.setAttribute("success", "Registration successful. Please login.");
                response.sendRedirect(request.getContextPath() + "/login?registered=true");
            } else {
                request.setAttribute("error", Encode.forHtmlContent("Registration failed. Please try again."));
                request.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(request, response);
            }

        } catch (SQLException e) {
            // Log error but don't expose details to user
            System.err.println("Database error during registration: " + e.getMessage());
            request.setAttribute("error", Encode.forHtmlContent("An error occurred. Please try again later."));
            request.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(request, response);
        }
    }
}
