package com.socialanalytics.dao.impl;

import com.socialanalytics.dao.AnalyticsDAO;
import com.socialanalytics.database.DatabaseManager;
import com.socialanalytics.exception.DatabaseException;
import com.socialanalytics.model.Engagement;
import com.socialanalytics.model.Recommendation;
import com.socialanalytics.model.SentimentResult;
import com.socialanalytics.model.Trend;
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
 * Direct JDBC implementation of AnalyticsDAO.
 */
public class JdbcAnalyticsDAO implements AnalyticsDAO {
    private final DatabaseManager dbManager;

    public JdbcAnalyticsDAO() {
        this.dbManager = DatabaseManager.getInstance();
    }

    public JdbcAnalyticsDAO(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public void saveEngagementMetrics(Long userId, Engagement engagement) {
        String sql = "INSERT INTO engagement_metrics (user_id, total_posts, total_likes, total_comments, " +
                "total_shares, total_views, average_likes, average_comments, average_shares, " +
                "engagement_rate, recorded_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, userId);
            ps.setLong(2, engagement.getTotalPosts());
            ps.setLong(3, engagement.getTotalLikes());
            ps.setLong(4, engagement.getTotalComments());
            if (engagement.getTotalShares() != null) {
                ps.setLong(5, engagement.getTotalShares());
            } else {
                ps.setNull(5, Types.BIGINT);
            }
            if (engagement.getTotalViews() != null) {
                ps.setLong(6, engagement.getTotalViews());
            } else {
                ps.setNull(6, Types.BIGINT);
            }
            ps.setDouble(7, engagement.getAverageLikes());
            ps.setDouble(8, engagement.getAverageComments());
            if (engagement.getAverageShares() != null) {
                ps.setDouble(9, engagement.getAverageShares());
            } else {
                ps.setNull(9, Types.DOUBLE);
            }
            if (engagement.getEngagementRate() != null) {
                ps.setDouble(10, engagement.getEngagementRate());
            } else {
                ps.setNull(10, Types.DOUBLE);
            }
            ps.setTimestamp(11, Timestamp.valueOf(LocalDateTime.now()));

            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error saving engagement metrics: " + e.getMessage(), e);
        }
    }

    @Override
    public void saveSentimentResults(Long userId, SentimentResult sentiment) {
        String sql = "INSERT INTO sentiment_results (user_id, total_analyzed, positive_count, neutral_count, " +
                "negative_count, positive_pct, neutral_pct, negative_pct, average_score, recorded_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, userId);
            ps.setLong(2, sentiment.getTotalAnalyzed());
            ps.setLong(3, sentiment.getPositiveCount());
            ps.setLong(4, sentiment.getNeutralCount());
            ps.setLong(5, sentiment.getNegativeCount());
            ps.setDouble(6, sentiment.getPositivePercent());
            ps.setDouble(7, sentiment.getNeutralPercent());
            ps.setDouble(8, sentiment.getNegativePercent());
            ps.setDouble(9, sentiment.getAverageScore());
            ps.setTimestamp(10, Timestamp.valueOf(LocalDateTime.now()));

            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error saving sentiment results: " + e.getMessage(), e);
        }
    }

    @Override
    public void saveTrendResults(Long userId, List<Trend> trends) {
        if (trends == null || trends.isEmpty()) return;

        // Clean previous trends for user and batch insert new ones
        String deleteSql = "DELETE FROM trend_results WHERE user_id = ?";
        String insertSql = "INSERT INTO trend_results (user_id, topic, previous_mentions, current_mentions, " +
                "growth_percentage, trend_score, status, explanation, recorded_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement psDel = conn.prepareStatement(deleteSql)) {
                psDel.setLong(1, userId);
                psDel.executeUpdate();
            }

            try (PreparedStatement psIns = conn.prepareStatement(insertSql)) {
                Timestamp now = Timestamp.valueOf(LocalDateTime.now());
                for (Trend t : trends) {
                    psIns.setLong(1, userId);
                    psIns.setString(2, t.getTopic());
                    psIns.setLong(3, t.getPreviousMentions());
                    psIns.setLong(4, t.getCurrentMentions());
                    psIns.setDouble(5, t.getGrowthPercentage());
                    psIns.setDouble(6, t.getTrendScore());
                    psIns.setString(7, t.getStatus().name());
                    psIns.setString(8, t.getExplanation());
                    psIns.setTimestamp(9, now);
                    psIns.addBatch();
                }
                psIns.executeBatch();
            }

            conn.commit();
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ignored) {
                }
            }
            throw new DatabaseException("Error saving trend results: " + e.getMessage(), e);
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
    public void saveRecommendations(Long userId, List<Recommendation> recommendations) {
        if (recommendations == null || recommendations.isEmpty()) return;

        String deleteSql = "DELETE FROM recommendations WHERE user_id = ?";
        String insertSql = "INSERT INTO recommendations (id, user_id, category, priority, title, description, " +
                "rule_triggered, confidence_score, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement psDel = conn.prepareStatement(deleteSql)) {
                psDel.setLong(1, userId);
                psDel.executeUpdate();
            }

            try (PreparedStatement psIns = conn.prepareStatement(insertSql)) {
                Timestamp now = Timestamp.valueOf(LocalDateTime.now());
                for (Recommendation r : recommendations) {
                    psIns.setString(1, r.getId());
                    psIns.setLong(2, userId);
                    psIns.setString(3, r.getType().name());
                    psIns.setString(4, r.getPriority().name());
                    psIns.setString(5, r.getTitle());
                    psIns.setString(6, r.getDescription());
                    psIns.setString(7, r.getRuleTriggered());
                    psIns.setDouble(8, r.getConfidenceScore());
                    psIns.setTimestamp(9, now);
                    psIns.addBatch();
                }
                psIns.executeBatch();
            }

            conn.commit();
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ignored) {
                }
            }
            throw new DatabaseException("Error saving recommendations: " + e.getMessage(), e);
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
    public Optional<Engagement> getLatestEngagement(Long userId) {
        String sql = "SELECT * FROM engagement_metrics WHERE user_id = ? ORDER BY recorded_at DESC LIMIT 1";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Engagement e = new Engagement();
                    e.setTotalPosts(rs.getLong("total_posts"));
                    e.setTotalLikes(rs.getLong("total_likes"));
                    e.setTotalComments(rs.getLong("total_comments"));
                    long sh = rs.getLong("total_shares");
                    e.setTotalShares(rs.wasNull() ? null : sh);
                    long v = rs.getLong("total_views");
                    e.setTotalViews(rs.wasNull() ? null : v);
                    e.setAverageLikes(rs.getDouble("average_likes"));
                    e.setAverageComments(rs.getDouble("average_comments"));
                    double avgSh = rs.getDouble("average_shares");
                    e.setAverageShares(rs.wasNull() ? null : avgSh);
                    double rate = rs.getDouble("engagement_rate");
                    e.setEngagementRate(rs.wasNull() ? null : rate);
                    e.setSharesAvailable(e.getTotalShares() != null);
                    e.setViewsAvailable(e.getTotalViews() != null);
                    return Optional.of(e);
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving latest engagement: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<SentimentResult> getLatestSentiment(Long userId) {
        String sql = "SELECT * FROM sentiment_results WHERE user_id = ? ORDER BY recorded_at DESC LIMIT 1";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    SentimentResult s = new SentimentResult();
                    s.setTotalAnalyzed(rs.getLong("total_analyzed"));
                    s.setPositiveCount(rs.getLong("positive_count"));
                    s.setNeutralCount(rs.getLong("neutral_count"));
                    s.setNegativeCount(rs.getLong("negative_count"));
                    s.setPositivePercent(rs.getDouble("positive_pct"));
                    s.setNeutralPercent(rs.getDouble("neutral_pct"));
                    s.setNegativePercent(rs.getDouble("negative_pct"));
                    s.setAverageScore(rs.getDouble("average_score"));
                    return Optional.of(s);
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving latest sentiment: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Trend> getLatestTrends(Long userId) {
        String sql = "SELECT * FROM trend_results WHERE user_id = ? ORDER BY trend_score DESC";
        List<Trend> list = new ArrayList<>();
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Trend t = new Trend();
                    t.setTopic(rs.getString("topic"));
                    t.setPreviousMentions(rs.getLong("previous_mentions"));
                    t.setCurrentMentions(rs.getLong("current_mentions"));
                    t.setGrowthPercentage(rs.getDouble("growth_percentage"));
                    t.setTrendScore(rs.getDouble("trend_score"));
                    t.setStatus(Trend.TrendStatus.valueOf(rs.getString("status")));
                    t.setExplanation(rs.getString("explanation"));
                    list.add(t);
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving latest trends: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Recommendation> getLatestRecommendations(Long userId) {
        String sql = "SELECT * FROM recommendations WHERE user_id = ? ORDER BY confidence_score DESC";
        List<Recommendation> list = new ArrayList<>();
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Recommendation r = new Recommendation();
                    r.setId(rs.getString("id"));
                    r.setType(Recommendation.RecommendationType.valueOf(rs.getString("category")));
                    r.setPriority(Recommendation.Priority.valueOf(rs.getString("priority")));
                    r.setTitle(rs.getString("title"));
                    r.setDescription(rs.getString("description"));
                    r.setRuleTriggered(rs.getString("rule_triggered"));
                    r.setConfidenceScore(rs.getDouble("confidence_score"));
                    list.add(r);
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving latest recommendations: " + e.getMessage(), e);
        }
    }
}
