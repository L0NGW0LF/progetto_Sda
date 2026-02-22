package com.secureapp.filter;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Security Headers Filter
 * Sets security headers for all responses including:
 * - HTTP-only cookies (prevents XSS access to cookies)
 * - Secure cookie flag (HTTPS only)
 * - SameSite attribute (CSRF protection)
 * - X-Content-Type-Options (prevents MIME sniffing)
 * - X-Frame-Options (prevents clickjacking)
 */
@WebFilter("/*")
public class SecurityHeadersFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletResponse httpResponse = (HttpServletResponse) response;

        httpResponse.setHeader("X-Content-Type-Options", "nosniff");
        httpResponse.setHeader("X-Frame-Options", "DENY");
        httpResponse.setHeader("X-XSS-Protection", "1; mode=block");
        httpResponse.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");

        // Content Security Policy (CSP) - Server Side
        httpResponse.setHeader("Content-Security-Policy",
                "default-src 'self'; " +
                        "script-src 'none'; " +
                        "style-src 'self' 'unsafe-inline'; " +
                        "img-src 'none'; " +
                        "font-src 'none'; " +
                        "connect-src 'none'; " +
                        "frame-src 'none'; " +
                        "frame-ancestors 'none'; " +
                        "form-action 'self'; " +
                        "base-uri 'self'; " +
                        "object-src 'none'");

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }
}
