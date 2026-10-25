package com.socialanalytics.servlet;

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
 * Servlet triggering data synchronization and fresh multithreaded analytics re-run.
 */
@WebServlet(name = "SyncServlet", urlPatterns = {"/sync"})
public class SyncServlet extends BaseServlet {
    private final SocialAccountService socialAccountService = SocialAccountService.getInstance();
    private final AnalyticsService analyticsService = AnalyticsService.getInstance();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = getAuthenticatedUser(req);
        if (user == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        List<SocialAccount> accounts = socialAccountService.getAccountsForUser(user.getId());
        for (SocialAccount a : accounts) {
            if (a.isConnected()) {
                socialAccountService.syncAccountData(a, a.isDemo());
            }
        }

        // Recompute analytics
        analyticsService.runAnalyticsForUser(user.getId(), null, null);
        req.getSession().setAttribute("flashSuccess", "Synced permitted data and updated analytics via multithreaded engine.");
        resp.sendRedirect(req.getContextPath() + "/dashboard");
    }
}
