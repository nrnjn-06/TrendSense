package com.socialanalytics.service;

import com.socialanalytics.dao.UserDAO;
import com.socialanalytics.dao.impl.JdbcUserDAO;
import com.socialanalytics.exception.AuthenticationException;
import com.socialanalytics.exception.ValidationException;
import com.socialanalytics.model.User;
import com.socialanalytics.util.SecurityUtil;
import com.socialanalytics.util.ValidationUtil;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Service managing user registration, secure authentication, and credentials.
 */
public class AuthService {
    private static final AuthService INSTANCE = new AuthService();
    private final UserDAO userDAO;
    private User currentUser;

    public AuthService() {
        this.userDAO = new JdbcUserDAO();
    }

    public AuthService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    public static AuthService getInstance() {
        return INSTANCE;
    }

    public synchronized User register(String username, String email, String password) throws ValidationException, AuthenticationException {
        ValidationUtil.validateUsername(username);
        ValidationUtil.validateEmail(email);
        ValidationUtil.validatePassword(password);

        if (userDAO.findByUsername(username).isPresent()) {
            throw new ValidationException("Username '" + username + "' is already registered.");
        }
        if (userDAO.findByEmail(email).isPresent()) {
            throw new ValidationException("Email '" + email + "' is already registered.");
        }

        String salt = SecurityUtil.generateSalt();
        String hash = SecurityUtil.hashPassword(password, salt);

        User user = new User();
        user.setUsername(username.trim());
        user.setEmail(email.trim().toLowerCase());
        user.setPasswordHash(hash);
        user.setSalt(salt);
        user.setCreatedAt(LocalDateTime.now());

        User created = userDAO.create(user);
        this.currentUser = created;
        return created;
    }

    public synchronized User login(String usernameOrEmail, String password) throws AuthenticationException {
        if (usernameOrEmail == null || usernameOrEmail.isBlank() || password == null || password.isBlank()) {
            throw new AuthenticationException("Username and password are required.");
        }

        Optional<User> userOpt = userDAO.findByUsername(usernameOrEmail.trim());
        if (userOpt.isEmpty()) {
            userOpt = userDAO.findByEmail(usernameOrEmail.trim().toLowerCase());
        }

        if (userOpt.isEmpty()) {
            throw new AuthenticationException("Invalid username or password.");
        }

        User user = userOpt.get();
        boolean valid = SecurityUtil.verifyPassword(password, user.getSalt(), user.getPasswordHash());
        if (!valid) {
            throw new AuthenticationException("Invalid username or password.");
        }

        this.currentUser = user;
        return user;
    }

    public synchronized User getCurrentUser() {
        return currentUser;
    }

    public synchronized void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public synchronized void logout() {
        this.currentUser = null;
    }
}
