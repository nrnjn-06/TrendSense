package com.socialanalytics.servlet;

import com.socialanalytics.model.Post;
import com.socialanalytics.model.User;
import com.socialanalytics.service.AnalyticsService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * Servlet presenting normalized social media posts table.
 */
@WebServlet(name = "PostServlet", urlPatterns = {"/posts"})
public class PostServlet extends BaseServlet {
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

        List<Post> posts = analyticsService.getPostsForUser(user.getId(), mode);
        req.setAttribute("user", user);
        req.setAttribute("posts", posts);
        req.setAttribute("currentMode", mode);
        forward(req, resp, "/WEB-INF/jsp/posts.jsp");
    }
}
