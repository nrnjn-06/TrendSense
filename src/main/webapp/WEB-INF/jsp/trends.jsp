<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Social Analytics – Trending Topics &amp; Scores</title>
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
        <a href="${pageContext.request.contextPath}/trends" class="active">Trends</a>
        <a href="${pageContext.request.contextPath}/sentiment">Sentiment</a>
        <a href="${pageContext.request.contextPath}/best-time">Best Time</a>
        <a href="${pageContext.request.contextPath}/recommendations">Recommendations</a>
        <a href="${pageContext.request.contextPath}/connect">Accounts</a>
    </nav>
    <div>
        <a href="${pageContext.request.contextPath}/logout" class="btn btn-secondary" style="padding: 0.35rem 0.75rem;">Logout</a>
    </div>
</header>

<div class="container">
    <div style="margin-bottom: 2rem;">
        <h1 style="font-size: 1.6rem; margin-bottom: 0.25rem; color: var(--text-main);">Trending Topics Analysis</h1>
        <p style="color: var(--text-muted); font-size: 0.9rem;">
            Transparent Trend Score calculated via weighted combination of normalized Mention, Engagement, Growth, and Recency factors.
        </p>
    </div>

    <!-- Trend Formula Documentation Card -->
    <div class="card" style="margin-bottom: 2rem; border-left: 4px solid var(--primary);">
        <div class="card-title">Documented Trend Scoring Formula</div>
        <p style="font-family: monospace; font-size: 0.95rem; color: var(--primary-hover); margin: 0.4rem 0; font-weight: bold;">
            Trend Score = (0.25 * S_mention) + (0.35 * S_engagement) + (0.25 * S_growth) + (0.15 * S_recency)
        </p>
        <p style="font-size: 0.85rem; color: var(--text-muted);">
            Each sub-factor S is normalized strictly between 0 and 100 relative to corpus maxima.
        </p>
    </div>

    <div class="table-wrapper">
        <table>
            <thead>
            <tr>
                <th>Topic / Keyword</th>
                <th>Trend Score</th>
                <th>Growth %</th>
                <th>Mentions</th>
                <th>Status</th>
                <th>Mathematical Explanation (Why this score?)</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach items="${trends.allTrends}" var="trend">
                <tr>
                    <td><strong>${trend.topic}</strong></td>
                    <td><span style="font-size: 0.95rem; font-weight: bold; color: var(--primary);"><fmt:formatNumber value="${trend.trendScore}" maxFractionDigits="1" />/100</span></td>
                    <td style="color: ${trend.growthPercentage >= 0 ? 'var(--success)' : 'var(--danger)'}; font-weight: 600;">
                        <fmt:formatNumber value="${trend.growthPercentage}" maxFractionDigits="1" />%
                    </td>
                    <td><fmt:formatNumber value="${trend.currentMentions}" /></td>
                    <td>
                        <span style="font-weight: 600; color: ${trend.status == 'RISING' ? 'var(--success)' : (trend.status == 'FALLING' ? 'var(--danger)' : 'var(--warning)')}">
                            ${trend.status.symbol} ${trend.status.label}
                        </span>
                    </td>
                    <td style="font-size: 0.85rem; color: var(--text-main);">${trend.explanation}</td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
    </div>
</div>

<footer>
    Social Analytics – SIH26152 Course Project
</footer>

</body>
</html>
