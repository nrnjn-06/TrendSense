package com.socialanalytics.servlet;

import com.socialanalytics.model.Platform;
import com.socialanalytics.model.SocialAccount;
import com.socialanalytics.model.User;
import com.socialanalytics.service.OAuthService;
import com.socialanalytics.service.SocialAccountService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Servlet managing account connections (Official OAuth, Developer Testing, and Demo Mode).
 */
@WebServlet(name = "ConnectAccountServlet", urlPatterns = {"/connect"})
public class ConnectAccountServlet extends BaseServlet {
    private final SocialAccountService socialAccountService = SocialAccountService.getInstance();
    private final OAuthService oAuthService = OAuthService.getInstance();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = getAuthenticatedUser(req);
        if (user == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        List<SocialAccount> accounts = socialAccountService.getAccountsForUser(user.getId());
        req.setAttribute("user", user);
        req.setAttribute("accounts", accounts);
        req.setAttribute("platforms", Platform.values());

        // Platform config status mapping for JSP
        Map<String, Boolean> configStatus = new HashMap<>();
        Map<String, OAuthService.PlatformConfigGuide> configGuides = new HashMap<>();
        for (Platform p : Platform.values()) {
            configStatus.put(p.name(), oAuthService.isConfigured(p));
            configGuides.put(p.name(), oAuthService.getConfigGuide(p));
        }
        req.setAttribute("configStatus", configStatus);
        req.setAttribute("configGuides", configGuides);

        forward(req, resp, "/WEB-INF/jsp/connect.jsp");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = getAuthenticatedUser(req);
        if (user == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        String action = req.getParameter("action");
        String mode = req.getParameter("mode"); // "oauth_init", "developer_testing", "demo", "demo_all"
        String platformStr = req.getParameter("platform");

        try {
            // 1. Channel actions: disconnect or sync
            if ("disconnect".equalsIgnoreCase(action)) {
                String accIdStr = req.getParameter("accountId");
                if (accIdStr != null && !accIdStr.isBlank()) {
                    socialAccountService.disconnectAccount(Long.parseLong(accIdStr));
                    req.getSession().setAttribute("flashSuccess", "Channel disconnected.");
                }
                resp.sendRedirect(req.getContextPath() + "/connect");
                return;
            } else if ("sync".equalsIgnoreCase(action)) {
                String accIdStr = req.getParameter("accountId");
                if (accIdStr != null && !accIdStr.isBlank()) {
                    Long accId = Long.parseLong(accIdStr);
                    List<SocialAccount> userAccounts = socialAccountService.getAccountsForUser(user.getId());
                    Optional<SocialAccount> target = userAccounts.stream().filter(a -> a.getId().equals(accId)).findFirst();
                    if (target.isPresent()) {
                        socialAccountService.syncAccountData(target.get(), target.get().isDemo());
                        req.getSession().setAttribute("flashSuccess", "Successfully synced latest data for " + target.get().getPlatform().getDisplayName() + "!");
                    }
                }
                resp.sendRedirect(req.getContextPath() + "/connect");
                return;
            }

            // 2. Connection modes
            if ("oauth_init".equalsIgnoreCase(mode)) {
                Platform p = Platform.fromString(platformStr);
                if (!oAuthService.isConfigured(p)) {
                    OAuthService.PlatformConfigGuide guide = oAuthService.getConfigGuide(p);
                    socialAccountService.recordConnectionFailure(user.getId(), p, "Missing OAuth credentials in .env: " + guide.missingVariables());
                    req.getSession().setAttribute("flashError", "Cannot start OAuth for " + p.getDisplayName() +
                            ": Missing client credentials (" + guide.missingVariables() + ") in .env. Configure them or use Demo Mode / Developer Testing.");
                } else {
                    String state = oAuthService.generateState(p);
                    req.getSession().setAttribute("oauth_state_" + p.name(), state);
                    String authUrl = oAuthService.buildAuthorizationUrl(p, state);
                    resp.sendRedirect(authUrl);
                    return;
                }
            } else if ("demo_all".equalsIgnoreCase(mode)) {
                socialAccountService.connectAllDemoAccounts(user.getId());
                req.getSession().setAttribute("flashSuccess", "Connected all 4 platforms in Demo Mode!");
            } else if ("demo".equalsIgnoreCase(mode)) {
                Platform p = Platform.fromString(platformStr);
                socialAccountService.connectDemoAccount(user.getId(), p);
                req.getSession().setAttribute("flashSuccess", "Connected " + p.getDisplayName() + " in Demo Mode!");
            } else if ("developer_testing".equalsIgnoreCase(mode) || "live".equalsIgnoreCase(mode)) {
                Platform p = Platform.fromString(platformStr);
                String token = req.getParameter("token");
                if (token == null || token.isBlank()) {
                    req.getSession().setAttribute("flashError", "Developer Testing token cannot be empty.");
                } else {
                    socialAccountService.connectDeveloperTokenAccount(user.getId(), p, token.trim());
                    req.getSession().setAttribute("flashSuccess", "Connected " + p.getDisplayName() + " via Developer Testing token!");
                }
            }
        } catch (Exception e) {
            req.getSession().setAttribute("flashError", "Connection error: " + e.getMessage());
        }

        resp.sendRedirect(req.getContextPath() + "/connect");
    }
}
