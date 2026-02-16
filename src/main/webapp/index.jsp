<%@ page contentType="text/html;charset=UTF-8" language="java" %>
    <!DOCTYPE html>
    <html lang="en">

    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Welcome - Secure Web App</title>
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
            }

            .container {
                background: white;
                padding: 60px 40px;
                border-radius: 10px;
                box-shadow: 0 10px 25px rgba(0, 0, 0, 0.2);
                text-align: center;
                max-width: 600px;
            }

            h1 {
                color: #333;
                font-size: 36px;
                margin-bottom: 20px;
            }

            p {
                color: #666;
                margin-bottom: 30px;
                line-height: 1.6;
            }

            .buttons {
                display: flex;
                gap: 20px;
                justify-content: center;
            }

            .btn {
                padding: 12px 30px;
                border-radius: 5px;
                text-decoration: none;
                font-weight: 600;
                transition: transform 0.2s;
            }

            .btn-primary {
                background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                color: white;
            }

            .btn-secondary {
                background: white;
                color: #667eea;
                border: 2px solid #667eea;
            }

            .btn:hover {
                transform: translateY(-2px);
            }
        </style>
    </head>

    <body>
        <div class="container">
            <h1>🔒 Secure Web Application</h1>
            <p>
                Welcome to the Secure File Sharing Application.<br>
                This application demonstrates secure web development practices including
                authentication, session management, and secure file upload.
            </p>
            <div class="buttons">
                <a href="${pageContext.request.contextPath}/login" class="btn btn-primary">Login</a>
                <a href="${pageContext.request.contextPath}/register" class="btn btn-secondary">Register</a>
            </div>
        </div>
    </body>

    </html>