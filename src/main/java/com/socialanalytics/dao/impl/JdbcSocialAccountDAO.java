package com.socialanalytics.dao.impl;

import com.socialanalytics.dao.SocialAccountDAO;
import com.socialanalytics.database.DatabaseManager;
import com.socialanalytics.exception.DatabaseException;
import com.socialanalytics.model.Platform;
import com.socialanalytics.model.SocialAccount;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Direct JDBC implementation of SocialAccountDAO using PreparedStatements.
 */
public class JdbcSocialAccountDAO implements SocialAccountDAO {
    private final DatabaseManager dbManager;

    public JdbcSocialAccountDAO() {
        this.dbManager = DatabaseManager.getInstance();
    }

    public JdbcSocialAccountDAO(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public SocialAccount save(SocialAccount account) {
        String sql = "INSERT INTO social_accounts (user_id, platform, platform_account_id, username, display_name, " +
                "profile_url, follower_count, following_count, is_connected, is_demo, access_token, " +
                "token_expires_at, last_synced_at, connection_status, status_reason) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            setAccountStatementParams(ps, account);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    account.setId(rs.getLong(1));
                }
            }
            return account;
        } catch (SQLException e) {
            throw new DatabaseException("Error saving social account: " + e.getMessage(), e);
        }
    }

    @Override
    public void update(SocialAccount account) {
        String sql = "UPDATE social_accounts SET platform_account_id = ?, username = ?, display_name = ?, " +
                "profile_url = ?, follower_count = ?, following_count = ?, is_connected = ?, is_demo = ?, " +
                "access_token = ?, token_expires_at = ?, last_synced_at = ?, connection_status = ?, status_reason = ? WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, account.getPlatformAccountId());
            ps.setString(2, account.getUsername());
            ps.setString(3, account.getDisplayName());
            ps.setString(4, safeTruncate(account.getProfileUrl(), 2000));
            ps.setLong(5, account.getFollowerCount());
            ps.setLong(6, account.getFollowingCount());
            ps.setBoolean(7, account.isConnected());
            ps.setBoolean(8, account.isDemo());
            ps.setString(9, account.getAccessToken());
            ps.setTimestamp(10, account.getTokenExpiresAt() != null ? Timestamp.valueOf(account.getTokenExpiresAt()) : null);
            ps.setTimestamp(11, account.getLastSyncedAt() != null ? Timestamp.valueOf(account.getLastSyncedAt()) : null);
            ps.setString(12, account.getConnectionStatus() != null ? account.getConnectionStatus() : (account.isConnected() ? "CONNECTED" : "DISCONNECTED"));
            ps.setString(13, safeTruncate(account.getStatusReason(), 2000));
            ps.setLong(14, account.getId());

            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error updating social account: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<SocialAccount> findById(Long id) {
        String sql = "SELECT * FROM social_accounts WHERE id = ?";
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
            throw new DatabaseException("Error finding social account by id: " + e.getMessage(), e);
        }
    }

    @Override
    public List<SocialAccount> findByUserId(Long userId) {
        String sql = "SELECT * FROM social_accounts WHERE user_id = ?";
        List<SocialAccount> list = new ArrayList<>();
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Error finding social accounts for user: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<SocialAccount> findByUserAndPlatform(Long userId, Platform platform) {
        String sql = "SELECT * FROM social_accounts WHERE user_id = ? AND platform = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, userId);
            ps.setString(2, platform.name());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DatabaseException("Error finding account by user and platform: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(Long id) {
        String sql = "DELETE FROM social_accounts WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error deleting social account: " + e.getMessage(), e);
        }
    }

    @Override
    public void disconnectAll(Long userId) {
        String sql = "UPDATE social_accounts SET is_connected = FALSE WHERE user_id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error disconnecting social accounts: " + e.getMessage(), e);
        }
    }

    private void setAccountStatementParams(PreparedStatement ps, SocialAccount account) throws SQLException {
        ps.setLong(1, account.getUserId());
        ps.setString(2, account.getPlatform().name());
        ps.setString(3, account.getPlatformAccountId());
        ps.setString(4, account.getUsername());
        ps.setString(5, account.getDisplayName());
        ps.setString(6, safeTruncate(account.getProfileUrl(), 2000));
        ps.setLong(7, account.getFollowerCount());
        ps.setLong(8, account.getFollowingCount());
        ps.setBoolean(9, account.isConnected());
        ps.setBoolean(10, account.isDemo());
        ps.setString(11, account.getAccessToken());
        ps.setTimestamp(12, account.getTokenExpiresAt() != null ? Timestamp.valueOf(account.getTokenExpiresAt()) : null);
        ps.setTimestamp(13, account.getLastSyncedAt() != null ? Timestamp.valueOf(account.getLastSyncedAt()) : null);
        ps.setString(14, account.getConnectionStatus() != null ? account.getConnectionStatus() : (account.isConnected() ? "CONNECTED" : "DISCONNECTED"));
        ps.setString(15, safeTruncate(account.getStatusReason(), 2000));
    }

    private String safeTruncate(String val, int maxLen) {
        if (val == null) return null;
        return val.length() <= maxLen ? val : val.substring(0, maxLen);
    }

    private SocialAccount mapRow(ResultSet rs) throws SQLException {
        SocialAccount acc = new SocialAccount();
        acc.setId(rs.getLong("id"));
        acc.setUserId(rs.getLong("user_id"));
        acc.setPlatform(Platform.valueOf(rs.getString("platform")));
        acc.setPlatformAccountId(rs.getString("platform_account_id"));
        acc.setUsername(rs.getString("username"));
        acc.setDisplayName(rs.getString("display_name"));
        acc.setProfileUrl(rs.getString("profile_url"));
        acc.setFollowerCount(rs.getLong("follower_count"));
        acc.setFollowingCount(rs.getLong("following_count"));
        acc.setConnected(rs.getBoolean("is_connected"));
        acc.setDemo(rs.getBoolean("is_demo"));
        acc.setAccessToken(rs.getString("access_token"));
        Timestamp exp = rs.getTimestamp("token_expires_at");
        if (exp != null) acc.setTokenExpiresAt(exp.toLocalDateTime());
        Timestamp syn = rs.getTimestamp("last_synced_at");
        if (syn != null) acc.setLastSyncedAt(syn.toLocalDateTime());
        try {
            String status = rs.getString("connection_status");
            if (status != null) acc.setConnectionStatus(status);
            acc.setStatusReason(rs.getString("status_reason"));
        } catch (SQLException ignored) {
        }
        return acc;
    }
}
