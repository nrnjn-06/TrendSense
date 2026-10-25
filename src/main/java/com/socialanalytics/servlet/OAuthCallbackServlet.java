package com.socialanalytics.servlet;

import com.socialanalytics.model.Platform;
import com.socialanalytics.model.User;
import com.socialanalytics.service.SocialAccountService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Servlet handling incoming OAuth 2.0 authorization callbacks from official platforms
 * (Instagram, Facebook, LinkedIn, X).
 */
@WebServlet(name = "OAuthCallbackServlet", urlPatterns = {"/oauth/callback", "/oauth/callback/*"})
public class OAuthCallbackServlet extends BaseServlet {
    private final SocialAccountService socialAccountService = SocialAccountService.getInstance();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = getAuthenticatedUser(req);
        if (user == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        // Determine platform from path or parameter
        Platform platform = determinePlatform(req);
        if (platform == null) {
            req.getSession().setAttribute("flashError", "Invalid or missing platform in OAuth callback.");
            resp.sendRedirect(req.getContextPath() + "/connect");
            return;
        }

        // Check for error returned by platform
        String error = req.getParameter("error");
        String errorDesc = req.getParameter("error_description");
        if (error != null && !error.isBlank()) {
            String msg = (errorDesc != null && !errorDesc.isBlank()) ? errorDesc : error;
            socialAccountService.recordConnectionFailure(user.getId(), platform, "Authorization denied by user or platform: " + msg);
            req.getSession().setAttribute("flashError", "Authorization denied on " + platform.getDisplayName() + ": " + msg);
            resp.sendRedirect(req.getContextPath() + "/connect");
            return;
        }

        // Extract authorization code
        String code = req.getParameter("code");
        if (code == null || code.isBlank()) {
            req.getSession().setAttribute("flashError", "No authorization code received from " + platform.getDisplayName() + ".");
            resp.sendRedirect(req.getContextPath() + "/connect");
            return;
        }

        try {
            com.socialanalytics.model.SocialAccount acc = socialAccountService.connectOAuthAccount(user.getId(), platform, code.trim());
            int count = new com.socialanalytics.dao.impl.JdbcPostDAO().findByAccountId(acc.getId()).size();
            String postMsg = (count > 0)
                    ? " (" + count + " live posts synchronized)"
                    : " (0 live posts accessible; Live Mode will reflect 0 posts until published)";
            req.getSession().setAttribute("flashSuccess", "Successfully connected " + platform.getDisplayName() + " @" + acc.getUsername() + " via official OAuth 2.0!" + postMsg);
        } catch (Exception e) {
            req.getSession().setAttribute("flashError", "Failed to complete " + platform.getDisplayName() + " OAuth: " + e.getMessage());
        }

        resp.sendRedirect(req.getContextPath() + "/connect");
    }

    private Platform determinePlatform(HttpServletRequest req) {
        String pathInfo = req.getPathInfo();
        if (pathInfo != null && pathInfo.length() > 1) {
            String sub = pathInfo.substring(1).trim();
            for (Platform p : Platform.values()) {
                if (p.name().equalsIgnoreCase(sub) || p.getDisplayName().equalsIgnoreCase(sub)) {
                    return p;
                }
            }
        }

        String platformParam = req.getParameter("platform");
        if (platformParam != null && !platformParam.isBlank()) {
            try {
                return Platform.fromString(platformParam);
            } catch (IllegalArgumentException ignored) {
            }
        }

        String state = req.getParameter("state");
        if (state != null && !state.isBlank()) {
            for (Platform p : Platform.values()) {
                if (state.toLowerCase().startsWith(p.name().toLowerCase())) {
                    return p;
                }
            }
        }

        return null;
    }
}
