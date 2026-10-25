package com.socialanalytics.dao;

import com.socialanalytics.model.Engagement;
import com.socialanalytics.model.Recommendation;
import com.socialanalytics.model.SentimentResult;
import com.socialanalytics.model.Trend;
import java.util.List;
import java.util.Optional;

/**
 * DAO interface for persisting and retrieving aggregated analytics snapshots.
 */
public interface AnalyticsDAO {
    void saveEngagementMetrics(Long userId, Engagement engagement);
    void saveSentimentResults(Long userId, SentimentResult sentiment);
    void saveTrendResults(Long userId, List<Trend> trends);
    void saveRecommendations(Long userId, List<Recommendation> recommendations);

    Optional<Engagement> getLatestEngagement(Long userId);
    Optional<SentimentResult> getLatestSentiment(Long userId);
    List<Trend> getLatestTrends(Long userId);
    List<Recommendation> getLatestRecommendations(Long userId);
}
