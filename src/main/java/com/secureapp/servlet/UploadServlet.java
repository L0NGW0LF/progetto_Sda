package com.secureapp.servlet;

import com.secureapp.service.ConcurrentUploadService;
import org.apache.tika.Tika;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Future;

/**
 * Upload Servlet
 * Handles secure file upload with:
 * - File type validation using Apache Tika
 * - Extension whitelist enforcement
 * - Concurrent upload processing with thread safety
 * - TOCTOU protection through ConcurrentUploadService
 */
@WebServlet("/upload")
@MultipartConfig(maxFileSize = 5 * 1024 * 1024, maxRequestSize = 10 * 1024 * 1024, fileSizeThreshold = 1024 * 1024)
public class UploadServlet extends HttpServlet {

    private final Tika tika = new Tika();
    private final ConcurrentUploadService uploadService = ConcurrentUploadService.getInstance();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        Integer userId = (Integer) session.getAttribute("userId");

        Part filePart = request.getPart("file");

        if (filePart == null || filePart.getSize() == 0) {
            response.sendRedirect(request.getContextPath() + "/dashboard?error=no_file");
            return;
        }

        String originalFilename = getFileName(filePart);
        if (originalFilename == null || originalFilename.isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/dashboard?error=invalid_filename");
            return;
        }

        if (!originalFilename.toLowerCase().endsWith(".txt")) {
            response.sendRedirect(request.getContextPath() + "/dashboard?error=invalid_extension");
            return;
        }

        try {
            byte[] fileContent;
            try (InputStream inputStream = filePart.getInputStream()) {
                java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
                int bytesRead;
                byte[] data = new byte[4096];
                while ((bytesRead = inputStream.read(data, 0, data.length)) != -1) {
                    buffer.write(data, 0, bytesRead);
                }
                fileContent = buffer.toByteArray();
            }

            // Validate file content using Apache Tika
            String detectedType = tika.detect(fileContent);

            if (!isTextFile(detectedType)) {
                response.sendRedirect(request.getContextPath() + "/dashboard?error=invalid_content_type");
                return;
            }

            // Process upload asynchronously using ConcurrentUploadService
            Future<String> uploadFuture = uploadService.processUploadAsync(
                    userId, originalFilename, fileContent, fileContent.length);

            uploadFuture.get();

            response.sendRedirect(request.getContextPath() + "/dashboard?success=upload_complete");

        } catch (Exception e) {
            System.err.println("Upload error: " + e.getMessage());
            e.printStackTrace();
            response.sendRedirect(request.getContextPath() + "/dashboard?error=upload_failed");
        }
    }

    // Extract filename from Part header.
    private String getFileName(Part part) {
        String contentDisposition = part.getHeader("content-disposition");
        if (contentDisposition == null) {
            return null;
        }

        for (String token : contentDisposition.split(";")) {
            if (token.trim().startsWith("filename")) {
                String filename = token.substring(token.indexOf('=') + 1).trim();
                return filename.replace("\"", "");
            }
        }
        return null;
    }

    // Check if detected MIME type is a text file..
    private boolean isTextFile(String mimeType) {
        if (mimeType == null) {
            return false;
        }

        return mimeType.equals("text/plain") ||
                mimeType.equals("application/octet-stream") ||
                mimeType.startsWith("text/");
    }
}
