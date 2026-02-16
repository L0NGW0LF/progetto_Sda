package com.secureapp.servlet;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

/**
 * Logout Servlet - Implements RF7
 * Handles secure session termination:
 * - Invalidates session server-side
 * - Removes session cookies
 * - Prevents session reuse
 */
@WebServlet("/logout")
public class LogoutServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        performLogout(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        performLogout(request, response);
    }

    private void performLogout(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        // Get current session
        HttpSession session = request.getSession(false);

        if (session != null) {
            // Invalidate session server-side
            session.invalidate();
        }

        // Remove session cookie by setting Max-Age to 0
        Cookie sessionCookie = new Cookie("JSESSIONID", "");
        sessionCookie.setPath(request.getContextPath() + "/");
        sessionCookie.setMaxAge(0); // Delete cookie immediately
        sessionCookie.setHttpOnly(true);
        response.addCookie(sessionCookie);

        // Redirect to login page with logout confirmation
        response.sendRedirect(request.getContextPath() + "/login?logout=true");
    }
}
