<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Social Analytics – Sentiment Analysis</title>
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
        <a href="${pageContext.request.contextPath}/sentiment" class="active">Sentiment</a>
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
        <h1 style="font-size: 1.6rem; margin-bottom: 0.25rem; color: var(--text-main);">Audience Sentiment Analysis</h1>
        <p style="color: var(--text-muted); font-size: 0.9rem;">
            Rule/Lexicon-based Java Sentiment Analyzer with negation flip detection and emotional polarity scaling.
        </p>
        <c:if test="${not empty sentiment.corpusDescription}">
            <div style="margin-top: 0.75rem; display: inline-block; background: #DCE7DC; border: 1px solid #C5D9C5; color: #2D6042; padding: 0.4rem 0.85rem; border-radius: 6px; font-size: 0.85rem; font-weight: 600;">
                📊 ${sentiment.corpusDescription}
            </div>
        </c:if>
    </div>

    <!-- Sentiment KPI Breakdown -->
    <div class="kpi-grid">
        <div class="card" style="border-top: 4px solid var(--success);">
            <div class="card-title">Positive Sentiment</div>
            <div class="card-value" style="color: var(--success);"><fmt:formatNumber value="${sentiment.positivePercent}" maxFractionDigits="1" />%</div>
            <div class="card-sub">${sentiment.positiveCount} comments / posts</div>
        </div>
        <div class="card" style="border-top: 4px solid var(--text-muted);">
            <div class="card-title">Neutral Sentiment</div>
            <div class="card-value" style="color: var(--text-muted);"><fmt:formatNumber value="${sentiment.neutralPercent}" maxFractionDigits="1" />%</div>
            <div class="card-sub">${sentiment.neutralCount} comments / posts</div>
        </div>
        <div class="card" style="border-top: 4px solid var(--danger);">
            <div class="card-title">Negative Sentiment</div>
            <div class="card-value" style="color: var(--danger);"><fmt:formatNumber value="${sentiment.negativePercent}" maxFractionDigits="1" />%</div>
            <div class="card-sub">${sentiment.negativeCount} comments / posts</div>
        </div>
        <div class="card" style="border-top: 4px solid var(--primary);">
            <div class="card-title">Average Net Polarity</div>
            <div class="card-value"><fmt:formatNumber value="${sentiment.averageScore}" maxFractionDigits="2" /></div>
            <div class="card-sub">Range: -1.0 to +1.0</div>
        </div>
    </div>

    <!-- Lexicon Keywords Columns -->
    <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 1.5rem;">
        <div class="card">
            <h3 style="color: var(--success); font-size: 1.1rem; margin-bottom: 1rem;">Top Positive Emotional Keywords</h3>
            <div class="table-wrapper">
                <table>
                    <thead><tr><th>Keyword</th><th>Occurrences</th></tr></thead>
                    <tbody>
                    <c:forEach items="${sentiment.topPositiveKeywords}" var="entry">
                        <tr>
                            <td><strong>${entry.key}</strong></td>
                            <td>${entry.value}</td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
        </div>

        <div class="card">
            <h3 style="color: var(--danger); font-size: 1.1rem; margin-bottom: 1rem;">Top Negative Complaint Keywords</h3>
            <div class="table-wrapper">
                <table>
                    <thead><tr><th>Keyword</th><th>Occurrences</th></tr></thead>
                    <tbody>
                    <c:forEach items="${sentiment.topNegativeKeywords}" var="entry">
                        <tr>
                            <td><strong>${entry.key}</strong></td>
                            <td>${entry.value}</td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
        </div>
    </div>
</div>

<footer>
    Social Analytics – SIH26152 Course Project
</footer>

</body>
</html>
