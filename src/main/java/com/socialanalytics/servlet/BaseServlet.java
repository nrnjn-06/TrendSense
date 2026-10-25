package com.socialanalytics.servlet;

import com.socialanalytics.model.User;
import com.socialanalytics.service.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

/**
 * Base abstract servlet providing session validation and request dispatching helpers.
 */
public abstract class BaseServlet extends HttpServlet {
    public static final String SESSION_USER = "LOGGED_IN_USER";
    protected final AuthService authService = AuthService.getInstance();

    protected User getAuthenticatedUser(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session != null) {
            Object obj = session.getAttribute(SESSION_USER);
            if (obj instanceof User) {
                return (User) obj;
            }
        }
        // Fallback for demo browsing if session not established
        User current = authService.getCurrentUser();
        if (current != null) {
            req.getSession(true).setAttribute(SESSION_USER, current);
            return current;
        }
        return null;
    }

    protected void forward(HttpServletRequest req, HttpServletResponse resp, String jspPath)
            throws ServletException, IOException {
        req.getRequestDispatcher(jspPath).forward(req, resp);
    }
}
