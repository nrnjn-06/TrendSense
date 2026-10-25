<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Social Analytics – Actionable Recommendations</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>

<header>
    <a href="${pageContext.request.contextPath}/dashboard" class="logo">
        <span>⚡ Social Analytics</span>
    </a>
    <nav>
        <a href="${pageContext.request.contextPath}/dashboard">Overview</a>
        <a href="${pageContext.request.contextPath}/posts">Posts</a>
        <a href="${pageContext.request.contextPath}/trends">Trends</a>
        <a href="${pageContext.request.contextPath}/sentiment">Sentiment</a>
        <a href="${pageContext.request.contextPath}/best-time">Best Time</a>
        <a href="${pageContext.request.contextPath}/recommendations" class="active">Recommendations</a>
        <a href="${pageContext.request.contextPath}/connect">Accounts</a>
    </nav>
    <div>
        <a href="${pageContext.request.contextPath}/logout" class="btn btn-secondary" style="padding: 0.35rem 0.75rem;">Logout</a>
    </div>
</header>

<div class="container">
    <div style="margin-bottom: 2rem;">
        <h1 style="font-size: 1.6rem; margin-bottom: 0.25rem; color: var(--text-main);">Rule-Based Strategic Recommendations</h1>
        <p style="color: var(--text-muted); font-size: 0.9rem;">
            Generated directly from analyzed metrics and evaluated business heuristics. No simulated ML claims.
        </p>
    </div>

    <c:forEach items="${recommendations}" var="rec">
        <div class="rec-card ${rec.priority}">
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.5rem;">
                <div class="rec-title">${rec.title}</div>
                <div>
                    <span style="font-size: 0.75rem; font-weight: bold; background: #FFFFFF; color: var(--text-main); padding: 0.25rem 0.6rem; border-radius: 4px; border: 1px solid var(--card-border);">
                        ${rec.priority} PRIORITY
                    </span>
                    <span style="font-size: 0.75rem; color: var(--primary); margin-left: 0.5rem;">
                        ${rec.type}
                    </span>
                </div>
            </div>
            <div class="rec-desc">${rec.description}</div>
            <div class="rec-rule">
                Rule Evaluated: <code>${rec.ruleTriggered}</code> | Confidence Score: <strong><fmt:formatNumber value="${rec.confidenceScore}" maxFractionDigits="0" />%</strong>
            </div>
        </div>
    </c:forEach>
</div>

<footer>
    Social Analytics – SIH26152 Course Project
</footer>

</body>
</html>
