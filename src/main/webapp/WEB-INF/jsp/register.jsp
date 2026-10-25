<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Social Analytics – Register</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
    <style>
        .auth-container {
            max-width: 440px;
            margin: 4rem auto;
            background: var(--card-bg);
            border: 1px solid var(--card-border);
            border-radius: 12px;
            padding: 2.5rem;
        }
        .form-group {
            margin-bottom: 1.25rem;
        }
        .form-group label {
            display: block;
            margin-bottom: 0.4rem;
            color: var(--text-main);
            font-size: 0.85rem;
            font-weight: 600;
        }
        .form-group input {
            width: 100%;
            padding: 0.75rem 1rem;
            background: #FFFFFF;
            border: 1px solid var(--card-border);
            border-radius: 6px;
            color: var(--text-main);
            font-size: 0.95rem;
        }
        .form-group input:focus {
            outline: none;
            border-color: var(--primary);
        }
        .error-msg {
            color: var(--danger);
            font-size: 0.85rem;
            margin-bottom: 1rem;
        }
    </style>
</head>
<body>

<header>
    <a href="${pageContext.request.contextPath}/login" class="logo">
        <span>⚡ Social Analytics</span>
    </a>
</header>

<div class="auth-container">
    <h2 style="font-size: 1.5rem; margin-bottom: 0.5rem;">Create Account</h2>
    <p style="color: var(--text-muted); font-size: 0.9rem; margin-bottom: 1.75rem;">
        Register your local analyst workspace
    </p>

    <c:if test="${not empty errorMessage}">
        <div class="error-msg">${errorMessage}</div>
    </c:if>

    <form action="${pageContext.request.contextPath}/register" method="post">
        <div class="form-group">
            <label for="username">Username</label>
            <input type="text" id="username" name="username" required placeholder="Choose a username">
        </div>
        <div class="form-group">
            <label for="email">Email Address</label>
            <input type="email" id="email" name="email" required placeholder="name@example.com">
        </div>
        <div class="form-group">
            <label for="password">Password</label>
            <input type="password" id="password" name="password" required placeholder="Minimum 6 characters">
        </div>

        <button type="submit" class="btn btn-success" style="width: 100%; padding: 0.75rem; margin-bottom: 1rem;">Register &amp; Launch</button>
    </form>

    <div style="text-align: center; font-size: 0.85rem; color: var(--text-muted); margin-top: 1rem;">
        Already have an account? <a href="${pageContext.request.contextPath}/login" style="color: var(--primary);">Sign In</a>
    </div>
</div>

<footer>
    Social Analytics – SIH26152 Course Project
</footer>

</body>
</html>
