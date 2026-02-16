package com.secureapp.filter;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * Authentication Filter - Implements RF3
 * Ensures that only authenticated users with valid sessions can access
 * protected resources.
 * Prevents access to resources after session timeout or logout.
 */
@WebFilter(urlPatterns = { "/dashboard", "/upload", "/file-content", "/logout" })
public class AuthFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // No initialization needed
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Get session without creating a new one
        HttpSession session = httpRequest.getSession(false);

        // Check if session exists and user is authenticated
        if (session == null || session.getAttribute("userId") == null) {
            // Session is invalid or expired - redirect to login
            httpResponse.sendRedirect(httpRequest.getContextPath() + "/login?error=session_expired");
            return;
        }

        // Session is valid - continue to requested resource
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // No cleanup needed
    }
}
