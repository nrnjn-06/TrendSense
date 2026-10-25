<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>TrendSense — Social Media Analytics &amp; Trend Engine</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
    <style>
        :root {
            --bg-main: #E8F0E8;
            --bg-secondary: #DCE7DC;
            --card-bg: #FFFFFF;
            --card-border: #C5D9C5;
            --text-main: #26372D;
            --text-muted: #657568;
            --primary: #397653;
            --primary-hover: #2D6042;
            --success: #3D8056;
            --warning: #B88738;
            --danger: #B65C52;
        }

        body {
            background-color: var(--bg-main);
            color: var(--text-main);
            min-height: 100vh;
            margin: 0;
            display: flex;
            flex-direction: column;
            overflow-x: hidden;
        }

        /* Clean Header */
        .floating-pill-nav {
            margin: 1.5rem auto 0;
            width: 90%;
            max-width: 1000px;
            background: var(--bg-secondary);
            border: 1px solid var(--card-border);
            border-radius: 9999px;
            padding: 0.75rem 1.75rem;
            display: flex;
            align-items: center;
            justify-content: space-between;
            box-shadow: 0 2px 8px rgba(38, 55, 45, 0.05);
        }

        .pill-brand {
            display: flex;
            align-items: center;
            gap: 0.5rem;
            text-decoration: none;
            color: var(--text-main);
            font-weight: 800;
            font-size: 1.15rem;
        }

        .pill-brand span.highlight {
            color: var(--primary);
        }

        .status-dot {
            width: 8px;
            height: 8px;
            background: var(--success);
            border-radius: 50%;
            display: inline-block;
        }

        .nav-links {
            display: flex;
            align-items: center;
            gap: 1.25rem;
        }

        .nav-pill-btn {
            background: #FFFFFF;
            border: 1px solid var(--card-border);
            color: var(--text-main);
            padding: 0.4rem 1rem;
            border-radius: 9999px;
            font-size: 0.85rem;
            font-weight: 600;
            text-decoration: none;
            transition: all 0.2s ease;
        }

        .nav-pill-btn:hover {
            background: #D2E3D2;
            color: var(--primary-hover);
        }

        /* Hero & Auth Section */
        .hero-layout {
            flex: 1;
            display: flex;
            align-items: center;
            justify-content: center;
            padding: 3rem 1.5rem;
            max-width: 1100px;
            margin: 0 auto;
            width: 100%;
            gap: 3.5rem;
        }

        @media (max-width: 860px) {
            .hero-layout {
                flex-direction: column;
                gap: 2rem;
                padding: 2rem 1rem;
            }
        }

        .hero-info {
            flex: 1;
        }

        .hero-tag {
            display: inline-flex;
            align-items: center;
            gap: 0.5rem;
            background: var(--bg-secondary);
            border: 1px solid var(--card-border);
            color: var(--primary-hover);
            padding: 0.35rem 0.85rem;
            border-radius: 9999px;
            font-size: 0.8rem;
            font-weight: 700;
            letter-spacing: 0.05em;
            text-transform: uppercase;
            margin-bottom: 1.25rem;
        }

        .hero-headline {
            font-size: 2.75rem;
            font-weight: 900;
            line-height: 1.15;
            letter-spacing: -0.03em;
            margin-bottom: 1.25rem;
            color: var(--text-main);
        }

        .hero-headline span.gradient-text {
            color: var(--primary-hover);
        }

        .hero-subhead {
            font-size: 1.1rem;
            color: var(--text-muted);
            line-height: 1.6;
            margin-bottom: 2rem;
            max-width: 480px;
        }

        .feature-pills {
            display: flex;
            flex-wrap: wrap;
            gap: 0.65rem;
        }

        .f-pill {
            background: #FFFFFF;
            border: 1px solid var(--card-border);
            padding: 0.45rem 0.9rem;
            border-radius: 8px;
            font-size: 0.85rem;
            color: var(--text-main);
            font-weight: 500;
        }

        /* Clean White Auth Card */
        .auth-glass-card {
            width: 100%;
            max-width: 420px;
            background: #FFFFFF;
            border: 1px solid var(--card-border);
            border-radius: 12px;
            padding: 2.25rem;
            box-shadow: 0 10px 25px -5px rgba(38, 55, 45, 0.06), 0 8px 10px -6px rgba(38, 55, 45, 0.02);
        }

        .form-group {
            margin-bottom: 1.25rem;
        }

        .form-group label {
            display: block;
            margin-bottom: 0.45rem;
            color: var(--text-main);
            font-size: 0.85rem;
            font-weight: 600;
        }

        .form-group input {
            width: 100%;
            padding: 0.75rem 0.95rem;
            background: #FFFFFF;
            border: 1px solid var(--card-border);
            border-radius: 6px;
            color: var(--text-main);
            font-size: 0.95rem;
            transition: all 0.2s ease;
        }

        .form-group input:focus {
            outline: none;
            border-color: var(--primary);
            box-shadow: 0 0 0 3px rgba(57, 118, 83, 0.2);
        }

        .btn-signin {
            width: 100%;
            padding: 0.8rem;
            background: var(--primary);
            border: none;
            border-radius: 6px;
            color: #fff;
            font-size: 0.95rem;
            font-weight: 700;
            cursor: pointer;
            transition: all 0.2s ease;
        }

        .btn-signin:hover {
            background: var(--primary-hover);
        }

        .btn-demo-quick {
            width: 100%;
            padding: 0.75rem;
            background: var(--bg-secondary);
            border: 1px solid var(--card-border);
            border-radius: 6px;
            color: var(--text-main);
            font-size: 0.9rem;
            font-weight: 700;
            cursor: pointer;
            display: block;
            text-align: center;
            text-decoration: none;
            transition: all 0.2s ease;
        }

        .btn-demo-quick:hover {
            background: #D2E3D2;
            color: var(--primary-hover);
        }

        .error-banner {
            background: #F9EAEA;
            border: 1px solid #E5BDB8;
            color: var(--danger);
            padding: 0.75rem 1rem;
            border-radius: 6px;
            font-size: 0.88rem;
            margin-bottom: 1.25rem;
        }

        .footer-note {
            text-align: center;
            padding: 1.5rem;
            color: var(--text-muted);
            font-size: 0.85rem;
            border-top: 1px solid var(--card-border);
            background: var(--bg-secondary);
        }
    </style>
</head>
<body>

    <!-- Floating Pill Header -->
    <header class="floating-pill-nav">
        <a href="${pageContext.request.contextPath}/login" class="pill-brand">
            <span>⚡</span>
            <span>Trend<span class="highlight">Sense</span></span>
        </a>
        <div class="nav-links">
            <span style="font-size: 0.8rem; color: #657568; display: flex; align-items: center; gap: 0.4rem;">
                <span class="status-dot"></span> System Live
            </span>
            <a href="${pageContext.request.contextPath}/login?demo=true" class="nav-pill-btn">⚡ Explore Demo</a>
        </div>
    </header>

    <!-- Hero + Sage Green Auth Card -->
    <div class="hero-layout">
        <div class="hero-info">
            <div class="hero-tag">Intelligent Social Analytics</div>
            <h1 class="hero-headline">
                Measure impact.<br>
                Discover trends.<br>
                <span class="gradient-text">Execute decisions.</span>
            </h1>
            <p class="hero-subhead">
                A high-precision social analytics engine powered by real Meta Graph API feeds, Google Trends search volumes, and multithreaded composite trend scoring.
            </p>
            <div class="feature-pills">
                <span class="f-pill">🔗 Meta Graph API</span>
                <span class="f-pill">🌐 Google Search Trends</span>
                <span class="f-pill">⚡ Multithreaded Engine</span>
                <span class="f-pill">📊 Sage Green Clean UI</span>
                <span class="f-pill">🔒 Strict Mode Isolation</span>
            </div>
        </div>

        <div class="auth-glass-card">
            <h2 style="font-size: 1.4rem; font-weight: 800; margin-bottom: 0.35rem; color: #26372D;">Analyst Sign In</h2>
            <p style="color: #657568; font-size: 0.88rem; margin-bottom: 1.5rem;">Access your intelligence workspace</p>

            <c:if test="${not empty errorMessage}">
                <div class="error-banner">${errorMessage}</div>
            </c:if>

            <form action="${pageContext.request.contextPath}/login" method="post">
                <div class="form-group">
                    <label for="username">Username or Email</label>
                    <input type="text" id="username" name="username" required autofocus placeholder="Enter your analyst handle">
                </div>
                <div class="form-group">
                    <label for="password">Password</label>
                    <input type="password" id="password" name="password" required placeholder="Enter your password">
                </div>

                <button type="submit" class="btn-signin">Sign In to Dashboard</button>
            </form>

            <div style="text-align: center; margin: 1.25rem 0; border-top: 1px solid var(--card-border); padding-top: 1.25rem;">
                <a href="${pageContext.request.contextPath}/login?demo=true" class="btn-demo-quick">
                    ⚡ Quick Guest Demo Access (Demo Mode)
                </a>
            </div>

            <div style="text-align: center; font-size: 0.85rem; color: #657568; margin-top: 1.25rem;">
                Need an account? <a href="${pageContext.request.contextPath}/register" style="color: var(--primary); text-decoration: none; font-weight: 600;">Create Account</a>
            </div>
        </div>
    </div>

    <footer class="footer-note">
        TrendSense &copy; 2026 Social Analytics Engine &bull; Academic Course Project SIH26152
    </footer>

</body>
</html>
