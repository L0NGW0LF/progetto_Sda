<%@ page contentType="text/html;charset=UTF-8" language="java" %>
    <%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
        <!DOCTYPE html>
        <html lang="en">

        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Register - Secure Web App</title>
            <style>
                * {
                    margin: 0;
                    padding: 0;
                    box-sizing: border-box;
                }

                body {
                    font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                    min-height: 100vh;
                    display: flex;
                    justify-content: center;
                    align-items: center;
                    padding: 20px;
                }

                .container {
                    background: white;
                    padding: 40px;
                    border-radius: 10px;
                    box-shadow: 0 10px 25px rgba(0, 0, 0, 0.2);
                    width: 100%;
                    max-width: 450px;
                }

                h1 {
                    color: #333;
                    margin-bottom: 30px;
                    text-align: center;
                }

                .form-group {
                    margin-bottom: 20px;
                }

                label {
                    display: block;
                    margin-bottom: 5px;
                    color: #555;
                    font-weight: 500;
                }

                input[type="email"],
                input[type="password"] {
                    width: 100%;
                    padding: 12px;
                    border: 1px solid #ddd;
                    border-radius: 5px;
                    font-size: 14px;
                    transition: border-color 0.3s;
                }

                input[type="email"]:focus,
                input[type="password"]:focus {
                    outline: none;
                    border-color: #667eea;
                }

                .btn {
                    width: 100%;
                    padding: 12px;
                    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                    color: white;
                    border: none;
                    border-radius: 5px;
                    font-size: 16px;
                    font-weight: 600;
                    cursor: pointer;
                    transition: transform 0.2s;
                }

                .btn:hover {
                    transform: translateY(-2px);
                }

                .error {
                    background: #fee;
                    color: #c33;
                    padding: 10px;
                    border-radius: 5px;
                    margin-bottom: 20px;
                    border-left: 4px solid #c33;
                }

                .info {
                    background: #e3f2fd;
                    color: #1976d2;
                    padding: 10px;
                    border-radius: 5px;
                    margin-bottom: 20px;
                    font-size: 13px;
                    border-left: 4px solid #1976d2;
                }

                .link {
                    text-align: center;
                    margin-top: 20px;
                    color: #666;
                }

                .link a {
                    color: #667eea;
                    text-decoration: none;
                    font-weight: 500;
                }

                .link a:hover {
                    text-decoration: underline;
                }
            </style>
        </head>

        <body>
            <div class="container">
                <h1>📝 Register</h1>

                <c:if test="${not empty error}">
                    <div class="error">
                        <c:out value="${error}" />
                    </div>
                </c:if>

                <div class="info">
                    <strong>Password Requirements:</strong><br>
                    • Minimum 8 characters<br>
                    • At least 1 uppercase letter<br>
                    • At least 1 lowercase letter<br>
                    • At least 1 digit<br>
                    • At least 1 special character (@$!%*?&)
                </div>

                <form method="post" action="${pageContext.request.contextPath}/register">
                    <input type="hidden" name="csrf_token" value="${csrfToken}">

                    <div class="form-group">
                        <label for="email">Email</label>
                        <input type="email" id="email" name="email" required autofocus>
                    </div>

                    <div class="form-group">
                        <label for="password">Password</label>
                        <input type="password" id="password" name="password" required>
                    </div>

                    <div class="form-group">
                        <label for="confirmPassword">Confirm Password</label>
                        <input type="password" id="confirmPassword" name="confirmPassword" required>
                    </div>

                    <button type="submit" class="btn">Register</button>
                </form>

                <div class="link">
                    Already have an account? <a href="${pageContext.request.contextPath}/login">Login here</a>
                </div>
            </div>
        </body>

        </html>