package com.socialanalytics;

import com.socialanalytics.analytics.BestTimeAnalyzer;
import com.socialanalytics.analytics.ContentAnalyzer;
import com.socialanalytics.model.*;
import com.socialanalytics.recommendation.RecommendationEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class RecommendationEngineTest {
    private RecommendationEngine engine;

    @BeforeEach
    public void setup() {
        engine = new RecommendationEngine();
    }

    @Test
    public void testRisingTopicRecommendation() {
        TrendResult trendResult = new TrendResult();
        Trend rising = new Trend("Artificial Intelligence", 100, 150, 15000);
        rising.setStatus(Trend.TrendStatus.RISING);
        rising.setTrendScore(88.0);
        trendResult.setRisingTopics(List.of(rising));

        List<Recommendation> recs = engine.generateRecommendations(
                new Engagement(), trendResult, null, null, null
        );

        assertFalse(recs.isEmpty());
        Recommendation rec = recs.stream()
                .filter(r -> r.getType() == Recommendation.RecommendationType.CONTENT_TOPIC)
                .findFirst()
                .orElse(null);

        assertNotNull(rec);
        assertEquals(Recommendation.Priority.HIGH, rec.getPriority());
        assertTrue(rec.getDescription().contains("Artificial Intelligence"));
        assertTrue(rec.getRuleTriggered().contains("Topic Growth > +15%"));
    }

    @Test
    public void testNegativeSentimentAlertRecommendation() {
        SentimentResult sentiment = new SentimentResult();
        sentiment.setTotalAnalyzed(100);
        sentiment.setNegativeCount(25);
        sentiment.setNegativePercent(25.0); // 25% > 15% threshold
        sentiment.setPositivePercent(50.0);
        sentiment.setNeutralPercent(25.0);
        sentiment.setTopNegativeKeywords(Map.of("broken", 12, "buggy", 8));

        List<Recommendation> recs = engine.generateRecommendations(
                new Engagement(), null, sentiment, null, null
        );

        Recommendation negRec = recs.stream()
                .filter(r -> r.getType() == Recommendation.RecommendationType.SENTIMENT_ACTION)
                .findFirst()
                .orElse(null);

        assertNotNull(negRec);
        assertEquals(Recommendation.Priority.HIGH, negRec.getPriority());
        assertTrue(negRec.getDescription().contains("25.0%"));
        assertTrue(negRec.getRuleTriggered().contains("Negative Sentiment Percentage > 15.0%"));
    }
}
