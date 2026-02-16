package com.secureapp.model;

import java.sql.Timestamp;

/**
 * FileModel representing uploaded file metadata.
 * Implements information hiding principles.
 */
public class FileModel {
    private int id;
    private int userId;
    private String originalFilename;
    private String storedFilename;
    private long fileSize;
    private Timestamp uploadDate;

    // Default constructor
    public FileModel() {
    }

    // Constructor for new file upload
    public FileModel(int userId, String originalFilename, String storedFilename, long fileSize) {
        this.userId = userId;
        this.originalFilename = originalFilename;
        this.storedFilename = storedFilename;
        this.fileSize = fileSize;
    }

    // Constructor with all fields
    public FileModel(int id, int userId, String originalFilename, String storedFilename, 
                     long fileSize, Timestamp uploadDate) {
        this.id = id;
        this.userId = userId;
        this.originalFilename = originalFilename;
        this.storedFilename = storedFilename;
        this.fileSize = fileSize;
        this.uploadDate = uploadDate;
    }

    // Getters
    public int getId() {
        return id;
    }

    public int getUserId() {
        return userId;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getStoredFilename() {
        return storedFilename;
    }

    public long getFileSize() {
        return fileSize;
    }

    public Timestamp getUploadDate() {
        return uploadDate != null ? new Timestamp(uploadDate.getTime()) : null;
    }

    // Setters
    public void setId(int id) {
        this.id = id;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public void setStoredFilename(String storedFilename) {
        this.storedFilename = storedFilename;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public void setUploadDate(Timestamp uploadDate) {
        this.uploadDate = uploadDate != null ? new Timestamp(uploadDate.getTime()) : null;
    }
}
