package com.secureapp.dao;

import com.secureapp.model.FileModel;
import com.secureapp.util.DatabaseUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for File operations.
 * Uses PreparedStatements to prevent SQL injection.
 */
public class FileDAO {

    /**
     * Save file metadata to database.
     * 
     * @param file FileModel containing metadata
     * @return true if saved successfully
     * @throws SQLException if database error occurs
     */
    public boolean saveFile(FileModel file) throws SQLException {
        String sql = "INSERT INTO files (user_id, original_filename, stored_filename, file_size) " +
                     "VALUES (?, ?, ?, ?)";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, file.getUserId());
            pstmt.setString(2, file.getOriginalFilename());
            pstmt.setString(3, file.getStoredFilename());
            pstmt.setLong(4, file.getFileSize());
            
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        }
    }

    /**
     * Get all files for a specific user.
     * 
     * @param userId User ID
     * @return List of FileModel objects
     * @throws SQLException if database error occurs
     */
    public List<FileModel> getFilesByUserId(int userId) throws SQLException {
        List<FileModel> files = new ArrayList<>();
        String sql = "SELECT id, user_id, original_filename, stored_filename, file_size, upload_date " +
                     "FROM files WHERE user_id = ? ORDER BY upload_date DESC";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    FileModel file = new FileModel(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getString("original_filename"),
                        rs.getString("stored_filename"),
                        rs.getLong("file_size"),
                        rs.getTimestamp("upload_date")
                    );
                    files.add(file);
                }
            }
        }
        return files;
    }

    /**
     * Get all files from all users.
     * 
     * @return List of all FileModel objects
     * @throws SQLException if database error occurs
     */
    public List<FileModel> getAllFiles() throws SQLException {
        List<FileModel> files = new ArrayList<>();
        String sql = "SELECT id, user_id, original_filename, stored_filename, file_size, upload_date " +
                     "FROM files ORDER BY upload_date DESC";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    FileModel file = new FileModel(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getString("original_filename"),
                        rs.getString("stored_filename"),
                        rs.getLong("file_size"),
                        rs.getTimestamp("upload_date")
                    );
                    files.add(file);
                }
            }
        }
        return files;
    }

    /**
     * Get file by stored filename.
     * 
     * @param storedFilename Stored filename
     * @return FileModel or null if not found
     * @throws SQLException if database error occurs
     */
    public FileModel getFileByStoredFilename(String storedFilename) throws SQLException {
        String sql = "SELECT id, user_id, original_filename, stored_filename, file_size, upload_date " +
                     "FROM files WHERE stored_filename = ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, storedFilename);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new FileModel(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getString("original_filename"),
                        rs.getString("stored_filename"),
                        rs.getLong("file_size"),
                        rs.getTimestamp("upload_date")
                    );
                }
            }
        }
        return null;
    }
}
