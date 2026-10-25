package com.socialanalytics.servlet;

import com.socialanalytics.exception.ValidationException;
import com.socialanalytics.model.User;
import com.socialanalytics.service.SocialAccountService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

/**
 * Servlet handling user registration.
 */
@WebServlet(name = "RegisterServlet", urlPatterns = {"/register"})
public class RegisterServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        forward(req, resp, "/WEB-INF/jsp/register.jsp");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String username = req.getParameter("username");
        String email = req.getParameter("email");
        String password = req.getParameter("password");

        try {
            User user = authService.register(username, email, password);
            // Pre-seed demo accounts
            SocialAccountService.getInstance().connectAllDemoAccounts(user.getId());

            HttpSession session = req.getSession(true);
            session.setAttribute(SESSION_USER, user);
            resp.sendRedirect(req.getContextPath() + "/dashboard");
        } catch (ValidationException e) {
            req.setAttribute("errorMessage", e.getMessage());
            forward(req, resp, "/WEB-INF/jsp/register.jsp");
        } catch (Exception e) {
            req.setAttribute("errorMessage", "Registration failed: " + e.getMessage());
            forward(req, resp, "/WEB-INF/jsp/register.jsp");
        }
    }
}
