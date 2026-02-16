<%@ page contentType="text/html;charset=UTF-8" language="java" isErrorPage="true" %>
    <!DOCTYPE html>
    <html lang="en">

    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Error - Secure Web App</title>
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
                padding: 40px;
                border-radius: 10px;
                box-shadow: 0 10px 25px rgba(0, 0, 0, 0.2);
                text-align: center;
                max-width: 500px;
            }

            h1 {
                color: #c33;
                font-size: 48px;
                margin-bottom: 20px;
            }

            p {
                color: #666;
                margin-bottom: 30px;
            }

            .btn {
                display: inline-block;
                padding: 12px 30px;
                background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                color: white;
                text-decoration: none;
                border-radius: 5px;
                font-weight: 600;
                transition: transform 0.2s;
            }

            .btn:hover {
                transform: translateY(-2px);
            }
        </style>
    </head>

    <body>
        <div class="container">
            <h1>⚠️ Error</h1>
            <p>An error occurred while processing your request.</p>
            <a href="${pageContext.request.contextPath}/login" class="btn">Return to Login</a>
        </div>
    </body>

    </html>