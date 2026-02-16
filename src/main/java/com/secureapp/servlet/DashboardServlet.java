package com.secureapp.servlet;

import com.secureapp.dao.FileDAO;
import com.secureapp.model.FileModel;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * Dashboard Servlet
 * Displays user dashboard with file list and upload functionality.
 */
@WebServlet("/dashboard")
public class DashboardServlet extends HttpServlet {

    private final FileDAO fileDAO = new FileDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        try {
            // Get all files from all users (RF6 - users can view files from others)
            List<FileModel> files = fileDAO.getAllFiles();
            request.setAttribute("files", files);

            // Forward to dashboard view
            request.getRequestDispatcher("/WEB-INF/views/dashboard.jsp").forward(request, response);

        } catch (SQLException e) {
            System.err.println("Error loading files: " + e.getMessage());
            request.setAttribute("error", "Error loading files");
            request.getRequestDispatcher("/WEB-INF/views/dashboard.jsp").forward(request, response);
        }
    }
}
