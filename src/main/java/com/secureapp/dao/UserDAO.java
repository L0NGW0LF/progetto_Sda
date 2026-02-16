package com.secureapp.dao;

import com.secureapp.model.User;
import com.secureapp.util.DatabaseUtil;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Data Access Object for User operations.
 * Implements secure database access using PreparedStatements to prevent SQL injection.
 * Uses BCrypt for password hashing with automatic salt generation.
 */
public class UserDAO {
    
    /**
     * Create a new user account.
     * Password is automatically hashed with BCrypt including salt.
     * 
     * @param email User email (must be unique)
     * @param plainPassword Plain text password (will be hashed)
     * @return true if user created successfully
     * @throws SQLException if database error occurs
     */
    public boolean createUser(String email, String plainPassword) throws SQLException {
        // Hash password with BCrypt (automatically generates and includes salt)
        String passwordHash = BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));
        
        String sql = "INSERT INTO users (email, password_hash) VALUES (?, ?)";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, email.toLowerCase().trim());
            pstmt.setString(2, passwordHash);
            
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        }
    }

    /**
     * Authenticate a user with email and password.
     * Uses BCrypt's secure comparison to verify password.
     * 
     * @param email User email
     * @param plainPassword Plain text password to verify
     * @return User object if authentication successful, null otherwise
     * @throws SQLException if database error occurs
     */
    public User authenticateUser(String email, String plainPassword) throws SQLException {
        String sql = "SELECT id, email, password_hash, created_at FROM users WHERE email = ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, email.toLowerCase().trim());
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password_hash");
                    
                    // Verify password using BCrypt (timing-attack safe comparison)
                    if (BCrypt.checkpw(plainPassword, storedHash)) {
                        User user = new User();
                        user.setId(rs.getInt("id"));
                        user.setEmail(rs.getString("email"));
                        user.setPasswordHash(storedHash);
                        user.setCreatedAt(rs.getTimestamp("created_at"));
                        return user;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Check if an email already exists in the database.
     * 
     * @param email Email to check
     * @return true if email exists
     * @throws SQLException if database error occurs
     */
    public boolean emailExists(String email) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE email = ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, email.toLowerCase().trim());
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    /**
     * Get user by ID.
     * 
     * @param userId User ID
     * @return User object or null if not found
     * @throws SQLException if database error occurs
     */
    public User getUserById(int userId) throws SQLException {
        String sql = "SELECT id, email, password_hash, created_at FROM users WHERE id = ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    User user = new User();
                    user.setId(rs.getInt("id"));
                    user.setEmail(rs.getString("email"));
                    user.setPasswordHash(rs.getString("password_hash"));
                    user.setCreatedAt(rs.getTimestamp("created_at"));
                    return user;
                }
            }
        }
        return null;
    }
}
