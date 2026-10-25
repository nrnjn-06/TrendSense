<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Social Analytics – Normalized Posts Corpus</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>

<header>
    <a href="${pageContext.request.contextPath}/dashboard" class="logo">
        <span>⚡ Social Analytics</span>
    </a>
    <nav>
        <a href="${pageContext.request.contextPath}/dashboard">Overview</a>
        <a href="${pageContext.request.contextPath}/posts" class="active">Posts</a>
        <a href="${pageContext.request.contextPath}/trends">Trends</a>
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
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.5rem;">
        <div>
            <h1 style="font-size: 1.6rem; margin-bottom: 0.25rem; color: var(--text-main);">Normalized Post Corpus</h1>
            <p style="color: var(--text-muted); font-size: 0.9rem;">
                Official multi-platform posts synchronized into common domain models in MySQL.
            </p>
        </div>
        <form action="${pageContext.request.contextPath}/sync" method="post">
            <button type="submit" class="btn btn-primary">⟳ Re-sync Permitted Data</button>
        </form>
    </div>

    <div class="table-wrapper">
        <table>
            <thead>
            <tr>
                <th>Post ID</th>
                <th>Platform</th>
                <th>Content Preview</th>
                <th>Type</th>
                <th>Timestamp</th>
                <th>Likes</th>
                <th>Comments</th>
                <th>Shares</th>
                <th>Views</th>
                <th>Total</th>
                <th>Link</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach items="${posts}" var="post">
                <tr>
                    <td style="font-family: monospace; font-size: 0.8rem; color: var(--text-muted);">${post.id}</td>
                    <td><strong>${post.platform.displayName}</strong></td>
                    <td style="max-width: 450px;">${post.content}</td>
                    <td><span style="font-size: 0.75rem; background: #DCE7DC; color: #2D6042; border: 1px solid #C5D9C5; padding: 0.2rem 0.5rem; border-radius: 4px; font-weight: 600;">${post.contentType}</span></td>
                    <td style="font-size: 0.85rem; color: var(--text-muted);">${post.timestamp}</td>
                    <td><fmt:formatNumber value="${post.likes}" /></td>
                    <td><fmt:formatNumber value="${post.commentsCount}" /></td>
                    <td>
                        <c:choose>
                            <c:when test="${post.sharesAvailable}">
                                <fmt:formatNumber value="${post.shares}" />
                            </c:when>
                            <c:otherwise>
                                <span style="color: var(--text-muted); font-style: italic;">N/A</span>
                            </c:otherwise>
                        </c:choose>
                    </td>
                    <td>
                        <c:choose>
                            <c:when test="${post.views != null}">
                                <fmt:formatNumber value="${post.views}" />
                            </c:when>
                            <c:otherwise>
                                <span style="color: var(--text-muted); font-style: italic;">N/A</span>
                            </c:otherwise>
                        </c:choose>
                    </td>
                    <td><strong><fmt:formatNumber value="${post.totalInteractions}" /></strong></td>
                    <td>
                        <c:if test="${not empty post.permalink}">
                            <a href="${post.permalink}" target="_blank" rel="noopener noreferrer" class="btn btn-secondary" style="padding: 0.2rem 0.5rem; font-size: 0.75rem;">Link ↗</a>
                        </c:if>
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
