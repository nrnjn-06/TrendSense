<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Social Media Integrations — TrendSense</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
    <style>
        .channel-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
            gap: 1.5rem;
            margin-bottom: 2rem;
        }
        .channel-card {
            background: var(--card-bg);
            border: 1px solid var(--card-border);
            border-radius: 10px;
            padding: 1.25rem;
            display: flex;
            flex-direction: column;
            justify-content: space-between;
        }
        .notice-box {
            background: var(--bg-secondary);
            border: 1px solid var(--card-border);
            border-radius: 8px;
            padding: 1rem;
            margin-top: 1rem;
            margin-bottom: 1rem;
        }
        .dev-testing-card {
            background: var(--bg-secondary);
            border: 1px dashed var(--card-border);
            border-radius: 8px;
            padding: 1.25rem;
            margin-top: 1.5rem;
        }
        .btn-sm {
            padding: 0.35rem 0.75rem;
            font-size: 0.85rem;
            border-radius: 6px;
            cursor: pointer;
            text-decoration: none;
            display: inline-block;
            border: none;
        }
    </style>
</head>
<body>

<header>
    <a href="${pageContext.request.contextPath}/dashboard" class="logo">
        <span>⚡ TrendSense</span>
    </a>
    <nav>
        <a href="${pageContext.request.contextPath}/dashboard">Overview</a>
        <a href="${pageContext.request.contextPath}/posts">Posts</a>
        <a href="${pageContext.request.contextPath}/trends">Trends</a>
        <a href="${pageContext.request.contextPath}/sentiment">Sentiment</a>
        <a href="${pageContext.request.contextPath}/best-time">Best Time</a>
        <a href="${pageContext.request.contextPath}/recommendations">Recommendations</a>
        <a href="${pageContext.request.contextPath}/connect" class="active">Accounts</a>
    </nav>
    <div>
        <a href="${pageContext.request.contextPath}/logout" class="btn btn-secondary" style="padding: 0.35rem 0.75rem;">Logout</a>
    </div>
</header>

<div class="container">
    <c:if test="${not empty sessionScope.flashSuccess}">
        <div style="background: #E0EFE0; border: 1px solid #A3D4AB; color: #2D6042; padding: 0.75rem 1.25rem; border-radius: 8px; margin-bottom: 1.5rem;">
            ✓ ${sessionScope.flashSuccess}
        </div>
        <c:remove var="flashSuccess" scope="session" />
    </c:if>

    <c:if test="${not empty sessionScope.flashError}">
        <div style="background: #F9EAEA; border: 1px solid #E5BDB8; color: #B65C52; padding: 0.75rem 1.25rem; border-radius: 8px; margin-bottom: 1.5rem;">
            ⚠ ${sessionScope.flashError}
        </div>
        <c:remove var="flashError" scope="session" />
    </c:if>

    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 2rem;">
        <div>
            <h1 style="font-size: 1.6rem; margin-bottom: 0.25rem; color: var(--text-main);">Authorized Social Media Connections</h1>
            <p style="color: var(--text-muted); font-size: 0.9rem;">
                Official OAuth 2.0 authorization across Instagram, Facebook, LinkedIn, and X with Demo Mode fallback.
            </p>
        </div>
        <div style="display: flex; gap: 0.75rem;">
            <form action="${pageContext.request.contextPath}/connect" method="post" style="display: inline;">
                <input type="hidden" name="mode" value="demo_all">
                <button type="submit" class="btn btn-primary">⚡ Connect All in Demo Mode</button>
            </form>
        </div>
    </div>

    <!-- Active Connections Channels -->
    <h2 style="font-size: 1.25rem; margin-bottom: 1rem; color: var(--text-main);">Connected Channels Status</h2>
    <div class="channel-grid">
        <c:forEach items="${platforms}" var="plt">
            <c:set var="matchedAcc" value="${null}" />
            <c:forEach items="${accounts}" var="a">
                <c:if test="${a.platform == plt}">
                    <c:set var="matchedAcc" value="${a}" />
                </c:if>
            </c:forEach>

            <div class="channel-card">
                <div>
                    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.75rem;">
                        <span style="font-size: 1.15rem; font-weight: bold; color: var(--text-main);">${plt.displayName}</span>
                        <c:choose>
                            <c:when test="${matchedAcc != null && matchedAcc.connected && matchedAcc.demo}">
                                <span class="badge-demo">DEMO MODE</span>
                            </c:when>
                            <c:when test="${matchedAcc != null && matchedAcc.connected && !matchedAcc.demo}">
                                <span class="badge-live">OFFICIAL API</span>
                            </c:when>
                            <c:when test="${matchedAcc != null && matchedAcc.connectionFailed}">
                                <span style="background: var(--danger); color: white; padding: 2px 8px; border-radius: 4px; font-size: 0.75rem; font-weight: bold;">FAILED</span>
                            </c:when>
                            <c:otherwise>
                                <span style="background: var(--bg-secondary); color: var(--text-muted); padding: 2px 8px; border-radius: 4px; font-size: 0.75rem; font-weight: 600;">DISCONNECTED</span>
                            </c:otherwise>
                        </c:choose>
                    </div>

                    <!-- Connection State Details -->
                    <div style="font-size: 0.9rem; margin-bottom: 0.5rem;">
                        <c:choose>
                            <c:when test="${matchedAcc != null && matchedAcc.connected}">
                                <div style="color: var(--success); font-weight: bold; margin-bottom: 0.25rem;">✓ Connected</div>
                                <div style="color: var(--text-main); font-weight: 600;">Account: <strong>@${matchedAcc.username}</strong></div>
                                <div style="color: var(--text-muted); font-size: 0.8rem;">${matchedAcc.displayName} &bull; <fmt:formatNumber value="${matchedAcc.followerCount}" /> followers</div>
                                <div style="color: var(--text-muted); font-size: 0.8rem; margin-top: 0.25rem;">
                                    Last Synced: <span style="color: var(--text-main);">${matchedAcc.lastSyncedAt != null ? matchedAcc.lastSyncedAt : 'Just now'}</span>
                                </div>
                            </c:when>
                            <c:when test="${matchedAcc != null && matchedAcc.connectionFailed}">
                                <div style="color: var(--danger); font-weight: bold; margin-bottom: 0.25rem;">⚠ Connection Failed</div>
                                <div style="color: var(--danger); font-size: 0.8rem; background: #F9EAEA; padding: 6px 8px; border-radius: 4px; border: 1px solid #E5BDB8; margin-bottom: 0.5rem;">
                                    <strong>Reason:</strong> ${matchedAcc.statusReason}
                                </div>
                            </c:when>
                            <c:otherwise>
                                <div style="color: var(--text-muted); margin-bottom: 0.25rem;">Not connected</div>
                                <div style="color: var(--text-muted); font-size: 0.8rem;">Requires: ${plt.requiredAccountType}</div>
                            </c:otherwise>
                        </c:choose>
                    </div>
                </div>

                <!-- Action Buttons -->
                <div style="display: flex; gap: 0.5rem; margin-top: 1rem; border-top: 1px solid var(--card-border); padding-top: 0.75rem;">
                    <c:choose>
                        <c:when test="${matchedAcc != null && matchedAcc.connected}">
                            <form action="${pageContext.request.contextPath}/connect" method="post" style="display: inline;">
                                <input type="hidden" name="action" value="sync">
                                <input type="hidden" name="accountId" value="${matchedAcc.id}">
                                <button type="submit" class="btn-sm btn-primary">↻ Sync Data</button>
                            </form>
                            <form action="${pageContext.request.contextPath}/connect" method="post" style="display: inline;">
                                <input type="hidden" name="action" value="disconnect">
                                <input type="hidden" name="accountId" value="${matchedAcc.id}">
                                <button type="submit" class="btn-sm btn-secondary" style="color: var(--danger);">Disconnect</button>
                            </form>
                        </c:when>
                        <c:when test="${matchedAcc != null && matchedAcc.connectionFailed}">
                            <form action="${pageContext.request.contextPath}/connect" method="post" style="display: inline;">
                                <input type="hidden" name="mode" value="oauth_init">
                                <input type="hidden" name="platform" value="${plt.name()}">
                                <button type="submit" class="btn-sm" style="background: var(--warning); color: white;">Try Again</button>
                            </form>
                            <form action="${pageContext.request.contextPath}/connect" method="post" style="display: inline;">
                                <input type="hidden" name="mode" value="demo">
                                <input type="hidden" name="platform" value="${plt.name()}">
                                <button type="submit" class="btn-sm btn-primary">Use Demo Mode</button>
                            </form>
                        </c:when>
                        <c:otherwise>
                            <form action="${pageContext.request.contextPath}/connect" method="post" style="display: inline;">
                                <input type="hidden" name="mode" value="oauth_init">
                                <input type="hidden" name="platform" value="${plt.name()}">
                                <button type="submit" class="btn-sm btn-success">Connect</button>
                            </form>
                            <form action="${pageContext.request.contextPath}/connect" method="post" style="display: inline;">
                                <input type="hidden" name="mode" value="demo">
                                <input type="hidden" name="platform" value="${plt.name()}">
                                <button type="submit" class="btn-sm btn-secondary">Demo</button>
                            </form>
                        </c:otherwise>
                    </c:choose>
                </div>
            </div>
        </c:forEach>
    </div>

    <!-- MAIN CONNECT SOCIAL MEDIA ACCOUNT DIALOG/CARD -->
    <div style="display: grid; grid-template-columns: 1fr; max-width: 680px; margin: 0 auto;">
        <div class="card">
            <div style="text-align: center; border-bottom: 1px solid var(--card-border); padding-bottom: 1rem; margin-bottom: 1.25rem;">
                <h2 style="font-size: 1.35rem; color: var(--text-main); margin-bottom: 0.25rem;">CONNECT SOCIAL MEDIA ACCOUNT</h2>
                <p style="color: var(--text-muted); font-size: 0.85rem;">Official OAuth 2.0 Platform Connection</p>
            </div>

            <form id="oauthForm" action="${pageContext.request.contextPath}/connect" method="post">
                <input type="hidden" name="mode" value="oauth_init">

                <!-- 1. Select Platform -->
                <div style="margin-bottom: 1.25rem;">
                    <label style="display:block; margin-bottom: 0.4rem; color: var(--text-main); font-weight: 600; font-size: 0.9rem;">
                        Select Platform:
                    </label>
                    <select id="platformSelect" name="platform" onchange="updatePlatformDetails()"
                            style="width: 100%; padding: 0.75rem; background: #FFFFFF; color: var(--text-main); border: 1px solid var(--card-border); border-radius: 6px; font-size: 1rem; font-weight: 500;">
                        <option value="INSTAGRAM" ${param.platform == 'INSTAGRAM' ? 'selected' : ''}>Instagram</option>
                        <option value="FACEBOOK" ${param.platform == 'FACEBOOK' ? 'selected' : ''}>Facebook</option>
                        <option value="LINKEDIN" ${param.platform == 'LINKEDIN' ? 'selected' : ''}>LinkedIn</option>
                        <option value="X" ${param.platform == 'X' ? 'selected' : ''}>X (Twitter)</option>
                    </select>
                </div>

                <!-- 2. Account Type Notice -->
                <div style="margin-bottom: 1.25rem; background: var(--bg-secondary); border: 1px solid var(--card-border); border-radius: 8px; padding: 0.85rem;">
                    <div style="color: var(--primary-hover); font-size: 0.8rem; text-transform: uppercase; font-weight: bold; letter-spacing: 0.5px; margin-bottom: 0.25rem;">
                        Account Type Requirement:
                    </div>
                    <div id="accountTypeDesc" style="color: var(--text-main); font-size: 0.9rem;">
                        Professional / Business / Creator account linked to a Facebook Page
                    </div>
                </div>

                <!-- 3. OAuth App Configuration Status Badge & Warning -->
                <div id="configStatusBox" style="margin-bottom: 1.25rem; font-size: 0.85rem;">
                    <!-- Updated dynamically via JS -->
                </div>

                <!-- 4. Dynamic Action Button -->
                <button type="submit" id="connectButton" class="btn btn-success" style="width: 100%; padding: 0.85rem; font-size: 1.05rem; font-weight: bold; margin-bottom: 1.25rem;">
                    Connect Instagram
                </button>

                <!-- 5. Transparency & Security Disclaimer -->
                <div class="notice-box" style="text-align: center;">
                    <p style="color: var(--text-muted); font-size: 0.85rem; margin-bottom: 0.35rem;">
                        ℹ You will be redirected to the official platform authorization page.
                    </p>
                    <p style="color: var(--primary-hover); font-weight: 600; font-size: 0.85rem; margin: 0;">
                        🔒 We never ask for your social-media password.
                    </p>
                </div>

                <!-- Alternative: Demo Mode Button -->
                <div style="display: flex; gap: 0.75rem; justify-content: space-between; margin-top: 1rem;">
                    <button type="button" onclick="connectSelectedDemo()" class="btn btn-secondary" style="flex: 1;">
                        ⚡ Connect in Demo Mode
                    </button>
                    <a href="${pageContext.request.contextPath}/dashboard" class="btn btn-secondary" style="padding: 0.65rem 1.25rem; text-align: center;">
                        Cancel
                    </a>
                </div>
            </form>

            <!-- DEVELOPER TESTING ONLY / FALLBACK MODE -->
            <div class="dev-testing-card">
                <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.5rem;">
                    <span style="font-size: 0.9rem; font-weight: bold; color: var(--warning);">
                        🛠 Developer Testing Only
                    </span>
                    <span style="font-size: 0.75rem; background: #FAF2E4; color: var(--warning); padding: 2px 6px; border-radius: 4px; font-weight: 600;">
                        Manual Token Fallback
                    </span>
                </div>
                <p style="color: var(--text-muted); font-size: 0.8rem; margin-bottom: 0.85rem;">
                    Normal users should use official OAuth connection above. Use this fallback only for developer testing when a platform OAuth application is not yet registered.
                </p>

                <form action="${pageContext.request.contextPath}/connect" method="post">
                    <input type="hidden" name="mode" value="developer_testing">
                    <input type="hidden" id="devPlatformInput" name="platform" value="INSTAGRAM">

                    <div style="margin-bottom: 0.75rem;">
                        <label style="display:block; margin-bottom: 0.25rem; color: var(--text-main); font-size: 0.8rem; font-weight: 500;">
                            API Access Token / Bearer Token:
                        </label>
                        <input type="password" name="token" required placeholder="Paste official access token / bearer key"
                                style="width: 100%; padding: 0.6rem; background: #FFFFFF; color: var(--text-main); border: 1px solid var(--card-border); border-radius: 6px; font-size: 0.85rem;">
                    </div>
                    <button type="submit" class="btn btn-secondary" style="width: 100%; font-size: 0.85rem; padding: 0.5rem;">
                        Authorize with Developer Token
                    </button>
                </form>
            </div>
        </div>
    </div>
</div>

<footer>
    Social Analytics – SIH26152 Course Project
</footer>

<script>
    const platformData = {
        'INSTAGRAM': {
            name: 'Instagram',
            accountType: 'Professional / Business / Creator account linked to a Facebook Page',
            configured: ${configStatus['INSTAGRAM'] ? 'true' : 'false'},
            missing: '${configGuides["INSTAGRAM"].missingVariables()}',
            portal: 'https://developers.facebook.com/apps/'
        },
        'FACEBOOK': {
            name: 'Facebook',
            accountType: 'Facebook Page with Page Admin / Editor privileges',
            configured: ${configStatus['FACEBOOK'] ? 'true' : 'false'},
            missing: '${configGuides["FACEBOOK"].missingVariables()}',
            portal: 'https://developers.facebook.com/apps/'
        },
        'LINKEDIN': {
            name: 'LinkedIn',
            accountType: 'LinkedIn Organization / Company Page Admin role',
            configured: ${configStatus['LINKEDIN'] ? 'true' : 'false'},
            missing: '${configGuides["LINKEDIN"].missingVariables()}',
            portal: 'https://www.linkedin.com/developers/apps'
        },
        'X': {
            name: 'X',
            accountType: 'X Developer App with OAuth 2.0 User Context (tweet.read, users.read)',
            configured: ${configStatus['X'] ? 'true' : 'false'},
            missing: '${configGuides["X"].missingVariables()}',
            portal: 'https://developer.x.com/en/portal/dashboard'
        }
    };

    function updatePlatformDetails() {
        const sel = document.getElementById('platformSelect').value;
        const info = platformData[sel];

        // Update button text
        document.getElementById('connectButton').innerText = 'Connect ' + info.name;

        // Update account type
        document.getElementById('accountTypeDesc').innerText = info.accountType;

        // Update dev platform input
        document.getElementById('devPlatformInput').value = sel;

        // Update config status box
        const statusBox = document.getElementById('configStatusBox');
        if (info.configured) {
            statusBox.innerHTML = '<span style="color: var(--success); font-weight: 600;">✓ Official OAuth application configured in .env</span>';
        } else {
            statusBox.innerHTML = '<div style="background: #FAF2E4; border: 1px solid #E4D3B0; color: #875A1C; padding: 0.6rem; border-radius: 6px;">' +
                '<strong style="color: var(--warning);">⚠ OAuth App Not Yet Configured in .env</strong><br>' +
                '<span style="font-size: 0.8rem; color: #875A1C;">Missing: ' + (info.missing || 'Client ID / Secret') + '</span><br>' +
                '<span style="font-size: 0.75rem; color: var(--text-muted);">Register app at <a href="' + info.portal + '" target="_blank" style="color: var(--primary-hover);">' + info.name + ' Developer Portal</a> or use Demo Mode below.</span>' +
                '</div>';
        }
    }

    function connectSelectedDemo() {
        const sel = document.getElementById('platformSelect').value;
        const form = document.createElement('form');
        form.method = 'POST';
        form.action = '${pageContext.request.contextPath}/connect';
        const modeInput = document.createElement('input');
        modeInput.type = 'hidden';
        modeInput.name = 'mode';
        modeInput.value = 'demo';
        const platformInput = document.createElement('input');
        platformInput.type = 'hidden';
        platformInput.name = 'platform';
        platformInput.value = sel;
        form.appendChild(modeInput);
        form.appendChild(platformInput);
        document.body.appendChild(form);
        form.submit();
    }

    // Initialize on page load
    document.addEventListener('DOMContentLoaded', updatePlatformDetails);
</script>

</body>
</html>
