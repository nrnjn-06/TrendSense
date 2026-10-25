<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Dashboard — TrendSense</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>

<header>
    <a href="${pageContext.request.contextPath}/dashboard" class="logo">
        <span>⚡</span>
        <span>TrendSense</span>
    </a>
    <nav>
        <a href="${pageContext.request.contextPath}/dashboard" class="active">Overview</a>
        <a href="${pageContext.request.contextPath}/posts">Posts Explorer</a>
        <a href="${pageContext.request.contextPath}/trends">Trends</a>
        <a href="${pageContext.request.contextPath}/sentiment">Sentiment</a>
        <a href="${pageContext.request.contextPath}/best-time">Best Time</a>
        <a href="${pageContext.request.contextPath}/recommendations">Recommendations</a>
        <a href="${pageContext.request.contextPath}/connect">Accounts</a>
    </nav>
    <div style="display: flex; align-items: center; gap: 1rem;">
        <c:choose>
            <c:when test="${currentMode == 'DEMO'}">
                <span class="badge-demo">DEMO MODE</span>
                <a href="${pageContext.request.contextPath}/dashboard?mode=LIVE" class="btn btn-secondary" style="padding: 0.35rem 0.75rem; color: #397653; border-color: #C5D9C5; background: #DCE7DC; font-weight: 600; font-size: 0.8rem; text-decoration: none;">🟢 Return to Live Mode</a>
            </c:when>
            <c:when test="${summary.liveDataAvailable}">
                <span class="badge-live">LIVE: @${summary.connectedAccountName}</span>
                <a href="${pageContext.request.contextPath}/dashboard?mode=DEMO" class="btn btn-secondary" style="padding: 0.35rem 0.75rem; color: #26372D; border-color: #C5D9C5; background: #DCE7DC; font-weight: 600; font-size: 0.8rem; text-decoration: none;">⚡ Explore Demo Mode</a>
            </c:when>
            <c:otherwise>
                <span class="badge-demo" style="background: #E0EFE0; color: #2D6042; border-color: #A3D4AB;">LIVE MODE</span>
                <a href="${pageContext.request.contextPath}/dashboard?mode=DEMO" class="btn btn-secondary" style="padding: 0.35rem 0.75rem; color: #26372D; border-color: #C5D9C5; background: #DCE7DC; font-weight: 600; font-size: 0.8rem; text-decoration: none;">⚡ Explore Demo Mode</a>
            </c:otherwise>
        </c:choose>
        <span style="font-size: 0.85rem; color: #26372D; font-weight: 600;">${user.username}</span>
        <a href="${pageContext.request.contextPath}/logout" class="btn btn-secondary" style="padding: 0.35rem 0.75rem;">Logout</a>
    </div>
</header>

<div class="container">

    <c:choose>
        <c:when test="${currentMode == 'LIVE' && !summary.liveDataAvailable && summary.totalPosts == 0}">
            <!-- Welcome / Empty State Panel -->
            <div style="background: #FFFFFF; border: 1px solid #C5D9C5; border-radius: 10px; padding: 3rem 2rem; text-align: center; margin: 2rem 0; box-shadow: 0 1px 3px rgba(38,55,45,0.05);">
                <h1 style="color: #2D6042; font-size: 2rem; margin-bottom: 0.75rem;">Welcome to TrendSense</h1>
                <p style="color: #26372D; font-size: 1.1rem; font-weight: 500; margin-bottom: 0.5rem;">
                    Turn social-media data into meaningful insights and actionable recommendations.
                </p>
                <p style="color: #657568; font-size: 0.95rem; margin-bottom: 2rem;">
                    No social accounts connected yet. Connect a platform to start analyzing your social-media data.
                </p>
                <div style="display: flex; justify-content: center; gap: 1.25rem; margin-bottom: 2.5rem;">
                    <a href="${pageContext.request.contextPath}/connect" class="btn btn-primary" style="padding: 0.75rem 1.75rem; font-size: 1rem; font-weight: 600; text-decoration: none;">
                        Connect Your Accounts
                    </a>
                    <a href="${pageContext.request.contextPath}/dashboard?mode=DEMO" class="btn btn-secondary" style="padding: 0.75rem 1.75rem; font-size: 1rem; font-weight: 600; text-decoration: none;">
                        Explore Demo Mode
                    </a>
                </div>
                <hr style="border: 0; border-top: 1px solid #C5D9C5; margin-bottom: 1.5rem;" />
                <div style="display: flex; justify-content: center; flex-wrap: wrap; gap: 1.5rem; font-size: 0.85rem; color: #657568;">
                    <span style="font-weight: bold; color: #26372D;">Official Integrations:</span>
                    <span style="color: #397653;">Instagram (Meta OAuth)</span>
                    <span style="color: #82966B;">Facebook (Pages API)</span>
                    <span style="color: #657568;">LinkedIn (Community API)</span>
                    <span style="color: #657568;">X / Twitter (API v2 PKCE)</span>
                </div>
            </div>
        </c:when>
        <c:otherwise>
            <!-- Banner for Active Data Mode -->
            <c:if test="${currentMode == 'DEMO'}">
                <div class="notice-banner" style="background: #DCE7DC; border-color: #C5D9C5;">
                    <div>
                        <strong style="color: #B88738;">DEMO MODE — SIMULATED DATA:</strong> Sample analytics preview (illustrative only, not retrieved from real accounts).
                    </div>
                    <div>
                        <a href="${pageContext.request.contextPath}/connect" class="btn btn-primary" style="padding: 0.35rem 0.85rem; text-decoration: none; font-weight: 600;">Connect Real Accounts</a>
                    </div>
                </div>
            </c:if>
            <c:if test="${currentMode == 'LIVE' && summary.liveDataAvailable}">
                <div class="notice-banner" style="background: #E0EFE0; border-color: #A3D4AB;">
                    <div>
                        <strong style="color: #2D6042;">✓ LIVE DATA:</strong> ${summary.dataSourceDescription}
                    </div>
                    <div>
                        <a href="${pageContext.request.contextPath}/connect" class="btn btn-primary" style="padding: 0.35rem 0.85rem; text-decoration: none; font-weight: 600;">Manage Accounts</a>
                    </div>
                </div>
            </c:if>

    <!-- KPI Grid -->
    <div class="kpi-grid">
        <div class="card">
            <div class="card-title">Total Posts</div>
            <div class="card-value">${summary.totalPosts}</div>
            <div class="card-sub">Multi-platform normalized</div>
        </div>
        <div class="card">
            <div class="card-title">Total Engagement</div>
            <div class="card-value"><fmt:formatNumber value="${summary.totalInteractions}" type="number" /></div>
            <div class="card-sub">Likes, comments, shares</div>
        </div>
        <div class="card">
            <div class="card-title">Avg Engagement Rate</div>
            <div class="card-value">
                <c:choose>
                    <c:when test="${summary.engagementRateAvailable && summary.averageEngagementRate != null}">
                        <fmt:formatNumber value="${summary.averageEngagementRate}" maxFractionDigits="2" />%
                    </c:when>
                    <c:otherwise>
                        N/A
                    </c:otherwise>
                </c:choose>
            </div>
            <div class="card-sub">
                <c:choose>
                    <c:when test="${summary.engagementRateAvailable}">
                        Based on ${summary.engagementRateBasis != null ? summary.engagementRateBasis.toLowerCase() : 'audience'}
                    </c:when>
                    <c:otherwise>
                        Audience reach unavailable via API
                    </c:otherwise>
                </c:choose>
            </div>
        </div>
        <div class="card">
            <div class="card-title">Dominant Sentiment</div>
            <div class="card-value">${summary.sentiment.dominantSentiment}</div>
            <div class="card-sub"><fmt:formatNumber value="${summary.sentiment.positivePercent}" maxFractionDigits="1" />% Positive</div>
        </div>
        <div class="card">
            <div class="card-title">Best Posting Window</div>
            <div class="card-value" style="font-size: 1.4rem;">${summary.bestPostingTimeWindow}</div>
            <div class="card-sub">Peak audience interaction</div>
        </div>
    </div>

    <!-- Top Performing Content Highlight -->
    <c:if test="${not empty summary.topPost}">
        <div class="card" style="margin-bottom: 2rem;">
            <div class="card-title">🌟 Top Performing Content Highlight</div>
            <p style="font-size: 1.15rem; font-weight: 500; margin: 0.5rem 0; color: #26372D;">"${summary.topPost.content}"</p>
            <div style="font-size: 0.85rem; color: var(--primary); font-weight: 600;">
                [${summary.topPost.platform.displayName}] Likes: <fmt:formatNumber value="${summary.topPost.likes}" /> |
                Comments: <fmt:formatNumber value="${summary.topPost.commentsCount}" /> |
                Shares: <c:choose><c:when test="${summary.topPost.sharesAvailable}"><fmt:formatNumber value="${summary.topPost.shares}" /></c:when><c:otherwise>N/A</c:otherwise></c:choose>
            </div>
        </div>
    </c:if>

    <!-- Top Posts Table Preview -->
    <div style="margin-bottom: 2.5rem;">
        <div style="display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 1rem;">
            <div>
                <h2 style="font-size: 1.25rem; margin-bottom: 0.25rem; color: #26372D;">Top Performing Posts</h2>
                <p style="color: var(--text-muted); font-size: 0.85rem;">Ranked by cumulative interactions across likes, comments, and shares.</p>
            </div>
            <a href="${pageContext.request.contextPath}/posts" class="btn btn-secondary" style="font-size: 0.8rem; padding: 0.4rem 0.85rem;">View Full Corpus →</a>
        </div>
        <div class="table-wrapper">
            <table>
                <thead>
                <tr>
                    <th>Platform</th>
                    <th>Type</th>
                    <th>Post Content</th>
                    <th>Likes</th>
                    <th>Comments</th>
                    <th>Shares</th>
                    <th>Views</th>
                    <th>Total</th>
                    <th>Link</th>
                </tr>
                </thead>
                <tbody>
                <c:forEach items="${summary.engagement.topPosts}" var="post">
                    <tr>
                        <td><strong>${post.platform.displayName}</strong></td>
                        <td><span style="font-size: 0.75rem; background: #DCE7DC; color: #2D6042; border: 1px solid #C5D9C5; padding: 0.2rem 0.5rem; border-radius: 4px; font-weight: 600;">${post.contentType != null ? post.contentType : 'POST'}</span></td>
                        <td style="max-width: 450px; line-height: 1.4; word-break: break-word;">${post.content}</td>
                        <td><fmt:formatNumber value="${post.likes}" /></td>
                        <td><fmt:formatNumber value="${post.commentsCount}" /></td>
                        <td>
                            <c:choose>
                                <c:when test="${post.sharesAvailable}">
                                    <fmt:formatNumber value="${post.shares}" />
                                </c:when>
                                <c:otherwise>
                                    <span style="color: #657568; font-style: italic;">N/A</span>
                                </c:otherwise>
                            </c:choose>
                        </td>
                        <td>
                            <c:choose>
                                <c:when test="${post.views != null}">
                                    <fmt:formatNumber value="${post.views}" />
                                </c:when>
                                <c:otherwise>
                                    <span style="color: #657568; font-style: italic;">N/A</span>
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

    <!-- Quick Recommendations Snippet -->
    <div>
        <h2 style="font-size: 1.25rem; margin-bottom: 1rem; color: #26372D;">Strategic Recommendations</h2>
        <c:forEach items="${summary.recommendations}" var="rec">
            <div class="rec-card ${rec.priority}">
                <div class="rec-title">[${rec.priority}] ${rec.title}</div>
                <div class="rec-desc">${rec.description}</div>
                <div class="rec-rule">Evaluated Rule: ${rec.ruleTriggered} | Confidence: <fmt:formatNumber value="${rec.confidenceScore}" maxFractionDigits="0" />%</div>
            </div>
        </c:forEach>
    </div>
        </c:otherwise>
    </c:choose>

</div>

<footer>
    Social Analytics – SIH26152 Course Project | Java 17/21, JDBC, Servlets, JSP, JavaFX
</footer>

</body>
</html>
