<%@ page contentType="text/html;charset=UTF-8" language="java" %>
    <%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
        <%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
            <!DOCTYPE html>
            <html lang="en">

            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Dashboard - Secure Web App</title>
                <style>
                    * {
                        margin: 0;
                        padding: 0;
                        box-sizing: border-box;
                    }

                    body {
                        font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                        background: #f5f7fa;
                        min-height: 100vh;
                    }

                    .header {
                        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                        color: white;
                        padding: 20px;
                        box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
                    }

                    .header-content {
                        max-width: 1200px;
                        margin: 0 auto;
                        display: flex;
                        justify-content: space-between;
                        align-items: center;
                    }

                    .header h1 {
                        font-size: 24px;
                    }

                    .header-info {
                        display: flex;
                        gap: 20px;
                        align-items: center;
                    }

                    .logout-btn {
                        background: rgba(255, 255, 255, 0.2);
                        color: white;
                        padding: 10px 20px;
                        border: 1px solid white;
                        border-radius: 5px;
                        text-decoration: none;
                        transition: background 0.3s;
                    }

                    .logout-btn:hover {
                        background: rgba(255, 255, 255, 0.3);
                    }

                    .container {
                        max-width: 1200px;
                        margin: 30px auto;
                        padding: 0 20px;
                    }

                    .card {
                        background: white;
                        border-radius: 10px;
                        padding: 30px;
                        box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
                        margin-bottom: 30px;
                    }

                    .card h2 {
                        color: #333;
                        margin-bottom: 20px;
                    }

                    .upload-form {
                        display: flex;
                        gap: 15px;
                        align-items: flex-end;
                    }

                    .form-group {
                        flex: 1;
                    }

                    label {
                        display: block;
                        margin-bottom: 5px;
                        color: #555;
                        font-weight: 500;
                    }

                    input[type="file"] {
                        width: 100%;
                        padding: 10px;
                        border: 2px dashed #ddd;
                        border-radius: 5px;
                        cursor: pointer;
                    }

                    .btn {
                        padding: 12px 30px;
                        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                        color: white;
                        border: none;
                        border-radius: 5px;
                        font-size: 14px;
                        font-weight: 600;
                        cursor: pointer;
                        transition: transform 0.2s;
                    }

                    .btn:hover {
                        transform: translateY(-2px);
                    }

                    .alert {
                        padding: 15px;
                        border-radius: 5px;
                        margin-bottom: 20px;
                    }

                    .alert-success {
                        background: #d4edda;
                        color: #155724;
                        border-left: 4px solid #28a745;
                    }

                    .alert-error {
                        background: #f8d7da;
                        color: #721c24;
                        border-left: 4px solid #dc3545;
                    }

                    .files-table {
                        width: 100%;
                        border-collapse: collapse;
                    }

                    .files-table th,
                    .files-table td {
                        padding: 12px;
                        text-align: left;
                        border-bottom: 1px solid #eee;
                    }

                    .files-table th {
                        background: #f8f9fa;
                        color: #333;
                        font-weight: 600;
                    }

                    .files-table tr:hover {
                        background: #f8f9fa;
                    }

                    .view-link {
                        color: #667eea;
                        text-decoration: none;
                        font-weight: 500;
                    }

                    .view-link:hover {
                        text-decoration: underline;
                    }

                    .empty-state {
                        text-align: center;
                        padding: 40px;
                        color: #999;
                    }
                </style>
            </head>

            <body>
                <div class="header">
                    <div class="header-content">
                        <h1>🔒 Secure File Sharing</h1>
                        <div class="header-info">
                            <span>👤
                                <c:out value="${sessionScope.userEmail}" />
                            </span>
                            <a href="${pageContext.request.contextPath}/logout" class="logout-btn">Logout</a>
                        </div>
                    </div>
                </div>

                <div class="container">
                    <c:if test="${param.success eq 'upload_complete'}">
                        <div class="alert alert-success">
                            ✓ File uploaded successfully!
                        </div>
                    </c:if>

                    <c:if test="${param.error eq 'no_file'}">
                        <div class="alert alert-error">✗ Please select a file to upload.</div>
                    </c:if>

                    <c:if test="${param.error eq 'invalid_filename'}">
                        <div class="alert alert-error">✗ Invalid filename.</div>
                    </c:if>

                    <c:if test="${param.error eq 'invalid_extension'}">
                        <div class="alert alert-error">✗ Only .txt files are allowed.</div>
                    </c:if>

                    <c:if test="${param.error eq 'invalid_content_type'}">
                        <div class="alert alert-error">✗ File content validation failed. Only text files are accepted.
                        </div>
                    </c:if>

                    <c:if test="${param.error eq 'upload_failed'}">
                        <div class="alert alert-error">✗ Upload failed. Please try again.</div>
                    </c:if>

                    <c:if test="${param.error eq 'invalid_request'}">
                        <div class="alert alert-error">✗ Invalid or expired request. Please try again.</div>
                    </c:if>

                    <div class="card">
                        <h2>📤 Upload File</h2>
                        <form method="post" action="${pageContext.request.contextPath}/upload"
                            enctype="multipart/form-data" class="upload-form">
                            <input type="hidden" name="csrf_token" value="${csrfToken}">

                            <div class="form-group">
                                <label for="file">Select File (.txt only)</label>
                                <input type="file" id="file" name="file" accept=".txt" required>
                            </div>
                            <button type="submit" class="btn">Upload</button>
                        </form>
                    </div>

                    <div class="card">
                        <h2>📁 Uploaded Files</h2>

                        <c:choose>
                            <c:when test="${not empty files}">
                                <table class="files-table">
                                    <thead>
                                        <tr>
                                            <th>Filename</th>
                                            <th>Uploaded By (User ID)</th>
                                            <th>Upload Date</th>
                                            <th>Size</th>
                                            <th>Action</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        <c:forEach items="${files}" var="file">
                                            <tr>
                                                <td>${fn:escapeXml(file.originalFilename)}</td>
                                                <td>User #${file.userId}</td>
                                                <td>${file.uploadDate}</td>
                                                <td>${file.fileSize} bytes</td>
                                                <td>
                                                    <a href="${pageContext.request.contextPath}/file-content?file=${file.storedFilename}"
                                                        class="view-link" target="_blank">View</a>
                                                </td>
                                            </tr>
                                        </c:forEach>
                                    </tbody>
                                </table>
                            </c:when>
                            <c:otherwise>
                                <div class="empty-state">
                                    <p>No files uploaded yet.</p>
                                </div>
                            </c:otherwise>
                        </c:choose>
                    </div>
                </div>
            </body>

            </html>