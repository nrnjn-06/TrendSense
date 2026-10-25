package com.socialanalytics;

import com.socialanalytics.analytics.TrendScoringService;
import com.socialanalytics.model.Trend;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TrendScoringTest {
    private TrendScoringService scoringService;

    @BeforeEach
    public void setup() {
        // Explicit weights: Mention=0.25, Engagement=0.35, Growth=0.25, Recency=0.15
        scoringService = new TrendScoringService(0.25, 0.35, 0.25, 0.15);
    }

    @Test
    public void testRisingTrendScoring() {
        // Previous = 100, Current = 150 -> Growth = +50%
        Trend t = new Trend("Artificial Intelligence", 100, 150, 15000);

        assertEquals(50.0, t.getGrowthPercentage(), 0.01);
        assertEquals(Trend.TrendStatus.RISING, t.getStatus());

        scoringService.scoreTrend(t, 200, 100.0, 0.0);

        assertTrue(t.getTrendScore() > 60.0);
        assertTrue(t.getTrendScore() <= 100.0);
        assertNotNull(t.getExplanation());
        assertTrue(t.getExplanation().contains("Rising"));
        assertTrue(t.getExplanation().contains("+50.0%"));
    }

    @Test
    public void testFallingTrendStatus() {
        // Previous = 100, Current = 40 -> Growth = -60%
        Trend t = new Trend("Outdated Tech", 100, 40, 200);

        assertEquals(-60.0, t.getGrowthPercentage(), 0.01);
        assertEquals(Trend.TrendStatus.FALLING, t.getStatus());

        scoringService.scoreTrend(t, 200, 100.0, 5.0);

        assertTrue(t.getTrendScore() < 50.0);
        assertTrue(t.getExplanation().contains("Falling"));
    }

    @Test
    public void testEmergingTopicStatus() {
        // Previous = 0, Current = 50 -> Emerging topic with no previous baseline
        Trend t = new Trend("Quantum AI", 0, 50, 4000);

        assertEquals(Trend.TrendStatus.EMERGING, t.getStatus());
        assertFalse(t.isHasGrowthBaseline());
        assertEquals(0.0, t.getGrowthPercentage(), 0.001);

        scoringService.scoreTrend(t, 100, 80.0, 1.0);

        assertTrue(t.getGrowthScore() >= 80.0);
        assertTrue(t.getTrendScore() > 60.0);
    }

    @Test
    public void testScoreClampedBetweenZeroAndHundred() {
        Trend extreme = new Trend("HyperTopic", 1, 10000, 1000000);
        scoringService.scoreTrend(extreme, 100, 50.0, 0.0);

        assertTrue(extreme.getTrendScore() >= 0.0);
        assertTrue(extreme.getTrendScore() <= 100.0);
    }
}
