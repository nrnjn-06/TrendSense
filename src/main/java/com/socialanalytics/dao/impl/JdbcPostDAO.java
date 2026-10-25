package com.socialanalytics.dao.impl;

import com.socialanalytics.dao.PostDAO;
import com.socialanalytics.database.DatabaseManager;
import com.socialanalytics.exception.DatabaseException;
import com.socialanalytics.model.Platform;
import com.socialanalytics.model.Post;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Direct JDBC implementation of PostDAO with transactions and batch execution.
 */
public class JdbcPostDAO implements PostDAO {
    private final DatabaseManager dbManager;

    public JdbcPostDAO() {
        this.dbManager = DatabaseManager.getInstance();
    }

    public JdbcPostDAO(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public void save(Post post) {
        saveAll(List.of(post));
    }

    @Override
    public void saveAll(List<Post> posts) {
        if (posts == null || posts.isEmpty()) return;

        // Upsert / Merge logic using standard JDBC
        String postSql = "MERGE INTO posts (id, account_id, platform, content, timestamp, content_type, " +
                "likes, comments_count, shares, views, permalink, engagement_rate) " +
                "KEY(id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        // For MySQL compatibility if MERGE is not standard, we can use INSERT ... ON DUPLICATE KEY UPDATE:
        String mysqlPostSql = "INSERT INTO posts (id, account_id, platform, content, timestamp, content_type, " +
                "likes, comments_count, shares, views, permalink, engagement_rate) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE content=VALUES(content), likes=VALUES(likes), " +
                "comments_count=VALUES(comments_count), shares=VALUES(shares), views=VALUES(views), " +
                "engagement_rate=VALUES(engagement_rate)";

        String sqlToUse = dbManager.isUsingFallback() ? postSql : mysqlPostSql;

        String hashtagSql = "MERGE INTO hashtags (tag, mention_count, total_interactions) KEY(tag) " +
                "VALUES (?, 1, ?)";
        String mysqlHashtagSql = "INSERT INTO hashtags (tag, mention_count, total_interactions) VALUES (?, 1, ?) " +
                "ON DUPLICATE KEY UPDATE mention_count = mention_count + 1, " +
                "total_interactions = total_interactions + VALUES(total_interactions)";
        String tagSqlToUse = dbManager.isUsingFallback() ? hashtagSql : mysqlHashtagSql;

        String postHashtagSql = "MERGE INTO post_hashtags (post_id, tag) KEY(post_id, tag) VALUES (?, ?)";
        String mysqlPostHashtagSql = "INSERT IGNORE INTO post_hashtags (post_id, tag) VALUES (?, ?)";
        String postTagSqlToUse = dbManager.isUsingFallback() ? postHashtagSql : mysqlPostHashtagSql;

        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            conn.setAutoCommit(false); // TRANSACTION START

            try (PreparedStatement psPost = conn.prepareStatement(sqlToUse);
                 PreparedStatement psTag = conn.prepareStatement(tagSqlToUse);
                 PreparedStatement psPostTag = conn.prepareStatement(postTagSqlToUse)) {

                for (Post p : posts) {
                    psPost.setString(1, p.getId());
                    psPost.setLong(2, p.getAccountId());
                    psPost.setString(3, p.getPlatform().name());
                    psPost.setString(4, p.getContent());
                    psPost.setTimestamp(5, p.getTimestamp() != null ? Timestamp.valueOf(p.getTimestamp()) : Timestamp.valueOf(LocalDateTime.now()));
                    psPost.setString(6, p.getContentType() != null ? p.getContentType() : "TEXT");
                    psPost.setLong(7, p.getLikes());
                    psPost.setLong(8, p.getCommentsCount());
                    if (p.getShares() != null) {
                        psPost.setLong(9, p.getShares());
                    } else {
                        psPost.setNull(9, Types.BIGINT);
                    }
                    if (p.getViews() != null) {
                        psPost.setLong(10, p.getViews());
                    } else {
                        psPost.setNull(10, Types.BIGINT);
                    }
                    psPost.setString(11, safeTruncate(p.getPermalink(), 2000));
                    psPost.setDouble(12, p.getEngagementRate() != null ? p.getEngagementRate() : 0.0);
                    psPost.addBatch();

                    // Hashtags
                    for (String tag : p.getHashtags()) {
                        psTag.setString(1, tag);
                        psTag.setLong(2, p.getTotalInteractions());
                        psTag.addBatch();

                        psPostTag.setString(1, p.getId());
                        psPostTag.setString(2, tag);
                        psPostTag.addBatch();
                    }
                }

                psPost.executeBatch();
                psTag.executeBatch();
                psPostTag.executeBatch();
            }

            conn.commit(); // TRANSACTION COMMIT
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback(); // TRANSACTION ROLLBACK
                } catch (SQLException rollbackEx) {
                    System.err.println("Rollback failed: " + rollbackEx.getMessage());
                }
            }
            throw new DatabaseException("Failed to batch save posts: " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }

    @Override
    public Optional<Post> findById(String id) {
        String sql = "SELECT * FROM posts WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Post post = mapRow(rs);
                    loadHashtags(conn, post);
                    return Optional.of(post);
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DatabaseException("Error finding post by id: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Post> findByAccountId(Long accountId) {
        String sql = "SELECT * FROM posts WHERE account_id = ? ORDER BY timestamp DESC";
        return queryPosts(sql, ps -> ps.setLong(1, accountId));
    }

    @Override
    public List<Post> findByUserId(Long userId) {
        String sql = "SELECT p.* FROM posts p " +
                "INNER JOIN social_accounts sa ON p.account_id = sa.id " +
                "WHERE sa.user_id = ? ORDER BY p.timestamp DESC";
        return queryPosts(sql, ps -> ps.setLong(1, userId));
    }

    @Override
    public List<Post> findByPlatform(Long userId, Platform platform) {
        String sql = "SELECT p.* FROM posts p " +
                "INNER JOIN social_accounts sa ON p.account_id = sa.id " +
                "WHERE sa.user_id = ? AND p.platform = ? ORDER BY p.timestamp DESC";
        return queryPosts(sql, ps -> {
            ps.setLong(1, userId);
            ps.setString(2, platform.name());
        });
    }

    @Override
    public void deleteByAccountId(Long accountId) {
        String sql = "DELETE FROM posts WHERE account_id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, accountId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error deleting posts by account id: " + e.getMessage(), e);
        }
    }

    @Override
    public long countByUserId(Long userId) {
        String sql = "SELECT COUNT(*) FROM posts p " +
                "INNER JOIN social_accounts sa ON p.account_id = sa.id " +
                "WHERE sa.user_id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
            return 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error counting posts: " + e.getMessage(), e);
        }
    }

    @FunctionalInterface
    private interface ParamSetter {
        void setParams(PreparedStatement ps) throws SQLException;
    }

    private List<Post> queryPosts(String sql, ParamSetter setter) {
        List<Post> list = new ArrayList<>();
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            setter.setParams(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }

            // Batch load hashtags
            for (Post p : list) {
                loadHashtags(conn, p);
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Error querying posts: " + e.getMessage(), e);
        }
    }

    private void loadHashtags(Connection conn, Post post) throws SQLException {
        String sql = "SELECT tag FROM post_hashtags WHERE post_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, post.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    post.addHashtag(rs.getString("tag"));
                }
            }
        }
    }

    private Post mapRow(ResultSet rs) throws SQLException {
        Post p = new Post();
        p.setId(rs.getString("id"));
        p.setAccountId(rs.getLong("account_id"));
        p.setPlatform(Platform.valueOf(rs.getString("platform")));
        p.setContent(rs.getString("content"));
        Timestamp ts = rs.getTimestamp("timestamp");
        if (ts != null) p.setTimestamp(ts.toLocalDateTime());
        p.setContentType(rs.getString("content_type"));
        p.setLikes(rs.getLong("likes"));
        p.setCommentsCount(rs.getLong("comments_count"));
        long s = rs.getLong("shares");
        p.setShares(rs.wasNull() ? null : s);
        long v = rs.getLong("views");
        p.setViews(rs.wasNull() ? null : v);
        p.setPermalink(rs.getString("permalink"));
        p.setEngagementRate(rs.getDouble("engagement_rate"));
        return p;
    }

    private String safeTruncate(String val, int maxLen) {
        if (val == null) return null;
        return val.length() <= maxLen ? val : val.substring(0, maxLen);
    }
}
