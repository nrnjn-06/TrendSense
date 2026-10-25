package com.socialanalytics.servlet;

import com.socialanalytics.model.AnalyticsSummary;
import com.socialanalytics.model.User;
import com.socialanalytics.service.AnalyticsService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Servlet presenting audience sentiment breakdown and emotional keyword lexicon maps.
 */
@WebServlet(name = "SentimentServlet", urlPatterns = {"/sentiment"})
public class SentimentServlet extends BaseServlet {
    private final AnalyticsService analyticsService = AnalyticsService.getInstance();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = getAuthenticatedUser(req);
        if (user == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        com.socialanalytics.model.AppMode mode = (com.socialanalytics.model.AppMode) req.getSession().getAttribute("appMode");
        if (mode == null) mode = com.socialanalytics.model.AppMode.LIVE;

        AnalyticsSummary summary = analyticsService.getCachedSummary(user.getId(), mode);
        req.setAttribute("user", user);
        req.setAttribute("sentiment", summary.getSentiment());
        req.setAttribute("currentMode", mode);
        forward(req, resp, "/WEB-INF/jsp/sentiment.jsp");
    }
}
