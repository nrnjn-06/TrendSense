package com.socialanalytics.servlet;

import com.socialanalytics.exception.AuthenticationException;
import com.socialanalytics.model.User;
import com.socialanalytics.service.SocialAccountService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

/**
 * Servlet handling user login and session initialization.
 */
@WebServlet(name = "LoginServlet", urlPatterns = {"/login"})
public class LoginServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String quickDemo = req.getParameter("demo");
        if ("true".equalsIgnoreCase(quickDemo)) {
            handleQuickDemoLogin(req, resp);
            return;
        }
        forward(req, resp, "/WEB-INF/jsp/login.jsp");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String username = req.getParameter("username");
        String password = req.getParameter("password");

        try {
            User user = authService.login(username, password);
            HttpSession session = req.getSession(true);
            session.setAttribute(SESSION_USER, user);
            resp.sendRedirect(req.getContextPath() + "/dashboard");
        } catch (AuthenticationException e) {
            req.setAttribute("errorMessage", e.getMessage());
            forward(req, resp, "/WEB-INF/jsp/login.jsp");
        } catch (Exception e) {
            req.setAttribute("errorMessage", "Login failed: " + e.getMessage());
            forward(req, resp, "/WEB-INF/jsp/login.jsp");
        }
    }

    private void handleQuickDemoLogin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User user;
        try {
            user = authService.login("demo_analyst", "demo123");
        } catch (Exception ex) {
            user = authService.register("demo_analyst", "demo@socialanalytics.local", "demo123");
            SocialAccountService.getInstance().connectAllDemoAccounts(user.getId());
        }
        HttpSession session = req.getSession(true);
        session.setAttribute(SESSION_USER, user);
        resp.sendRedirect(req.getContextPath() + "/dashboard");
    }
}
