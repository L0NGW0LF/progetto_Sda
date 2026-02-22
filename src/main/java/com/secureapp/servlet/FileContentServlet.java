package com.secureapp.servlet;

import com.secureapp.dao.FileDAO;
import com.secureapp.model.FileModel;
import com.secureapp.service.ConcurrentUploadService;
import org.owasp.encoder.Encode;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;

/**
 * File Content Servlet
 * Displays file content securely with:
 * - Output encoding to prevent XSS
 * - Content-Type set to text/plain to prevent execution
 * - Access control through session validation
 */
@WebServlet("/file-content")
public class FileContentServlet extends HttpServlet {

    private final FileDAO fileDAO = new FileDAO();
    private final ConcurrentUploadService uploadService = ConcurrentUploadService.getInstance();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        String storedFilename = request.getParameter("file");

        if (storedFilename == null || storedFilename.isEmpty()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing file parameter");
            return;
        }

        try {
            FileModel fileModel = fileDAO.getFileByStoredFilename(storedFilename);

            if (fileModel == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found");
                return;
            }

            String fileContent = uploadService.getFileContent(storedFilename);

            response.setContentType("text/plain; charset=UTF-8");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("X-Content-Type-Options", "nosniff");
            response.setHeader("Content-Disposition", "inline");

            // Encode output to prevent XSS
            String encodedContent = Encode.forHtml(fileContent);

            // Write content to response
            PrintWriter out = response.getWriter();
            out.println("=== File: " + Encode.forHtml(fileModel.getOriginalFilename()) + " ===");
            out.println("Uploaded by User ID: " + fileModel.getUserId());
            out.println("Upload Date: " + fileModel.getUploadDate());
            out.println("Size: " + fileModel.getFileSize() + " bytes");
            out.println("\n--- Content ---\n");
            out.println(encodedContent);

        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error retrieving file");
        } catch (IOException e) {
            System.err.println("File read error: " + e.getMessage());
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error reading file");
        } catch (Exception e) {
            System.err.println("File decryption error: " + e.getClass().getSimpleName());
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error processing file");
        }
    }
}
