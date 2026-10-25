package com.socialanalytics.dao.impl;

import com.socialanalytics.dao.UserDAO;
import com.socialanalytics.database.DatabaseManager;
import com.socialanalytics.exception.DatabaseException;
import com.socialanalytics.model.User;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Direct JDBC implementation of UserDAO using PreparedStatements.
 */
public class JdbcUserDAO implements UserDAO {
    private final DatabaseManager dbManager;

    public JdbcUserDAO() {
        this.dbManager = DatabaseManager.getInstance();
    }

    public JdbcUserDAO(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public User create(User user) {
        String sql = "INSERT INTO users (username, email, password_hash, salt, created_at) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            LocalDateTime now = (user.getCreatedAt() != null) ? user.getCreatedAt() : LocalDateTime.now();
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPasswordHash());
            ps.setString(4, user.getSalt());
            ps.setTimestamp(5, Timestamp.valueOf(now));

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    user.setId(rs.getLong(1));
                }
            }
            user.setCreatedAt(now);
            return user;
        } catch (SQLException e) {
            throw new DatabaseException("Error creating user: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<User> findById(Long id) {
        String sql = "SELECT id, username, email, password_hash, salt, created_at FROM users WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DatabaseException("Error querying user by ID: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<User> findByUsername(String username) {
        String sql = "SELECT id, username, email, password_hash, salt, created_at FROM users WHERE username = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DatabaseException("Error querying user by username: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<User> findByEmail(String email) {
        String sql = "SELECT id, username, email, password_hash, salt, created_at FROM users WHERE email = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DatabaseException("Error querying user by email: " + e.getMessage(), e);
        }
    }

    @Override
    public void updatePassword(Long userId, String passwordHash, String salt) {
        String sql = "UPDATE users SET password_hash = ?, salt = ? WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, passwordHash);
            ps.setString(2, salt);
            ps.setLong(3, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error updating user password: " + e.getMessage(), e);
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("created_at");
        return new User(
                rs.getLong("id"),
                rs.getString("username"),
                rs.getString("email"),
                rs.getString("password_hash"),
                rs.getString("salt"),
                ts != null ? ts.toLocalDateTime() : null
        );
    }
}
