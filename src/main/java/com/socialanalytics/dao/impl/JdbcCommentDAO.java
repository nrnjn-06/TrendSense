package com.socialanalytics.dao.impl;

import com.socialanalytics.dao.CommentDAO;
import com.socialanalytics.database.DatabaseManager;
import com.socialanalytics.exception.DatabaseException;
import com.socialanalytics.model.Comment;
import com.socialanalytics.model.Platform;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Direct JDBC implementation of CommentDAO with batching and transaction support.
 */
public class JdbcCommentDAO implements CommentDAO {
    private final DatabaseManager dbManager;

    public JdbcCommentDAO() {
        this.dbManager = DatabaseManager.getInstance();
    }

    public JdbcCommentDAO(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public void save(Comment comment) {
        saveAll(List.of(comment));
    }

    @Override
    public void saveAll(List<Comment> comments) {
        if (comments == null || comments.isEmpty()) return;

        String h2Sql = "MERGE INTO comments (comment_id, post_id, platform, author_name, text, timestamp, " +
                "sentiment, sentiment_score) KEY(comment_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        String mysqlSql = "INSERT INTO comments (comment_id, post_id, platform, author_name, text, timestamp, " +
                "sentiment, sentiment_score) VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE text=VALUES(text), sentiment=VALUES(sentiment), " +
                "sentiment_score=VALUES(sentiment_score)";

        String sql = dbManager.isUsingFallback() ? h2Sql : mysqlSql;

        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (Comment c : comments) {
                    ps.setString(1, c.getCommentId());
                    ps.setString(2, c.getPostId());
                    ps.setString(3, c.getPlatform().name());
                    ps.setString(4, c.getAuthorName());
                    ps.setString(5, c.getText());
                    ps.setTimestamp(6, c.getTimestamp() != null ? Timestamp.valueOf(c.getTimestamp()) : Timestamp.valueOf(LocalDateTime.now()));
                    ps.setString(7, c.getSentiment() != null ? c.getSentiment().name() : "NEUTRAL");
                    ps.setDouble(8, c.getSentimentScore());
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            conn.commit();
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ignored) {
                }
            }
            throw new DatabaseException("Failed to batch save comments: " + e.getMessage(), e);
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
    public List<Comment> findByPostId(String postId) {
        String sql = "SELECT * FROM comments WHERE post_id = ? ORDER BY timestamp DESC";
        List<Comment> list = new ArrayList<>();
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, postId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Error finding comments for post: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Comment> findByUserId(Long userId) {
        String sql = "SELECT c.* FROM comments c " +
                "INNER JOIN posts p ON c.post_id = p.id " +
                "INNER JOIN social_accounts sa ON p.account_id = sa.id " +
                "WHERE sa.user_id = ? ORDER BY c.timestamp DESC";
        List<Comment> list = new ArrayList<>();
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
            throw new DatabaseException("Error finding comments for user: " + e.getMessage(), e);
        }
    }

    @Override
    public void updateSentiment(String commentId, Comment.SentimentType sentiment, double score) {
        String sql = "UPDATE comments SET sentiment = ?, sentiment_score = ? WHERE comment_id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, sentiment.name());
            ps.setDouble(2, score);
            ps.setString(3, commentId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error updating comment sentiment: " + e.getMessage(), e);
        }
    }

    private Comment mapRow(ResultSet rs) throws SQLException {
        Comment c = new Comment();
        c.setCommentId(rs.getString("comment_id"));
        c.setPostId(rs.getString("post_id"));
        c.setPlatform(Platform.valueOf(rs.getString("platform")));
        c.setAuthorName(rs.getString("author_name"));
        c.setText(rs.getString("text"));
        Timestamp ts = rs.getTimestamp("timestamp");
        if (ts != null) c.setTimestamp(ts.toLocalDateTime());
        String sent = rs.getString("sentiment");
        if (sent != null) c.setSentiment(Comment.SentimentType.valueOf(sent));
        c.setSentimentScore(rs.getDouble("sentiment_score"));
        return c;
    }
}
