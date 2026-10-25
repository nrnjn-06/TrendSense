<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Social Analytics – Best Posting Time</title>
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
        <a href="${pageContext.request.contextPath}/best-time" class="active">Best Time</a>
        <a href="${pageContext.request.contextPath}/recommendations">Recommendations</a>
        <a href="${pageContext.request.contextPath}/connect">Accounts</a>
    </nav>
    <div>
        <a href="${pageContext.request.contextPath}/logout" class="btn btn-secondary" style="padding: 0.35rem 0.75rem;">Logout</a>
    </div>
</header>

<div class="container">
    <div style="margin-bottom: 2rem;">
        <h1 style="font-size: 1.6rem; margin-bottom: 0.25rem;">Best Posting Time Optimization</h1>
        <p style="color: var(--text-muted); font-size: 0.9rem;">
            Calculates average audience interaction rates grouped across 24 hourly buckets.
        </p>
    </div>

    <div class="card" style="margin-bottom: 2rem; border-left: 4px solid var(--primary);">
        <div class="card-title">Identified Peak Schedule Window</div>
        <div style="font-size: 1.85rem; font-weight: bold; color: var(--primary); margin: 0.4rem 0;">
            ${summary.bestPostingTimeWindow}
        </div>
        <p style="color: var(--text-main); font-size: 0.9rem;">
            Posts published in this window achieve significantly higher interaction rates than daytime or midnight baselines.
        </p>
    </div>

    <h2 style="font-size: 1.25rem; margin-bottom: 1rem; color: var(--text-main);">Average Interactions by Hour of Day</h2>
    <div class="table-wrapper">
        <table>
            <thead>
            <tr>
                <th>Hour (24h)</th>
                <th>Time Window</th>
                <th>Average Interactions Per Post</th>
                <th>Performance Status</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach items="${hourlyMap}" var="entry">
                <tr>
                    <td><strong>${entry.key}:00</strong></td>
                    <td>
                        <c:choose>
                            <c:when test="${entry.key == 0}">12:00 AM</c:when>
                            <c:when test="${entry.key < 12}">${entry.key}:00 AM</c:when>
                            <c:when test="${entry.key == 12}">12:00 PM</c:when>
                            <c:otherwise>${entry.key - 12}:00 PM</c:otherwise>
                        </c:choose>
                    </td>
                    <td><fmt:formatNumber value="${entry.value}" maxFractionDigits="1" /></td>
                    <td>
                        <c:choose>
                            <c:when test="${entry.key == summary.bestPostingHour}">
                                <span style="background: var(--success); color: #fff; padding: 0.2rem 0.6rem; border-radius: 4px; font-weight: bold; font-size: 0.8rem;">★ PEAK HOUR</span>
                            </c:when>
                            <c:when test="${entry.value > 0}">
                                <span style="color: var(--primary);">Active</span>
                            </c:when>
                            <c:otherwise>
                                <span style="color: var(--text-muted);">No Activity</span>
                            </c:otherwise>
                        </c:choose>
                    </td>
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
