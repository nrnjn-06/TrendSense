package com.socialanalytics.servlet;

import com.socialanalytics.model.AnalyticsSummary;
import com.socialanalytics.model.AppMode;
import com.socialanalytics.model.Platform;
import com.socialanalytics.model.SocialAccount;
import com.socialanalytics.model.User;
import com.socialanalytics.service.AnalyticsService;
import com.socialanalytics.service.SocialAccountService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * Servlet handling main web analytics dashboard presentation.
 */
@WebServlet(name = "DashboardServlet", urlPatterns = {"/dashboard", ""})
public class DashboardServlet extends BaseServlet {
    private final AnalyticsService analyticsService = AnalyticsService.getInstance();
    private final SocialAccountService socialAccountService = SocialAccountService.getInstance();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = getAuthenticatedUser(req);
        if (user == null) {
            // Auto sign in as analyst for web workspace
            try {
                user = authService.login("demo_analyst", "demo123");
            } catch (Exception e) {
                user = authService.register("demo_analyst", "demo@socialanalytics.local", "demo123");
            }
            req.getSession(true).setAttribute(SESSION_USER, user);
        }

        Long userId = user.getId();

        // Manage AppMode from session or query param (Default is strictly LIVE mode)
        String modeParam = req.getParameter("mode");
        AppMode currentMode = (AppMode) req.getSession().getAttribute("appMode");
        if (currentMode == null) {
            currentMode = AppMode.LIVE;
        }

        if ("DEMO".equalsIgnoreCase(modeParam)) {
            currentMode = AppMode.DEMO;
            req.getSession().setAttribute("appMode", currentMode);
            analyticsService.invalidateCache();
        } else if ("LIVE".equalsIgnoreCase(modeParam)) {
            currentMode = AppMode.LIVE;
            req.getSession().setAttribute("appMode", currentMode);
            analyticsService.invalidateCache();
        } else if ("toggle".equalsIgnoreCase(modeParam)) {
            currentMode = (currentMode == AppMode.LIVE) ? AppMode.DEMO : AppMode.LIVE;
            req.getSession().setAttribute("appMode", currentMode);
            analyticsService.invalidateCache();
        }

        // Live mode does NOT automatically seed demo accounts
        List<SocialAccount> accounts = socialAccountService.getAccountsForUser(userId);

        String platformParam = req.getParameter("platform");
        Platform platformFilter = null;
        if (platformParam != null && !platformParam.isBlank() && !platformParam.equalsIgnoreCase("ALL")) {
            try {
                platformFilter = Platform.fromString(platformParam);
            } catch (Exception ignored) {
            }
        }

        AnalyticsSummary summary = analyticsService.runAnalyticsForUser(userId, platformFilter, currentMode, null);

        req.setAttribute("user", user);
        req.setAttribute("summary", summary);
        req.setAttribute("accounts", accounts);
        req.setAttribute("currentMode", currentMode);
        req.setAttribute("selectedPlatform", platformParam != null ? platformParam : "ALL");

        forward(req, resp, "/WEB-INF/jsp/dashboard.jsp");
    }
}
