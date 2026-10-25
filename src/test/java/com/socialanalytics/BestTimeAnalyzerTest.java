package com.socialanalytics;

import com.socialanalytics.analytics.BestTimeAnalyzer;
import com.socialanalytics.model.Platform;
import com.socialanalytics.model.Post;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class BestTimeAnalyzerTest {
    private BestTimeAnalyzer analyzer;

    @BeforeEach
    public void setup() {
        analyzer = new BestTimeAnalyzer();
    }

    @Test
    public void testInsufficientDataEdgeCase() {
        List<Post> posts = new ArrayList<>();
        posts.add(new Post("1", 1L, Platform.X, "Only one post", LocalDateTime.now(), "TWEET", 10, 2, 1, 100L, ""));

        BestTimeAnalyzer.BestTimeResult result = analyzer.analyze(posts);
        assertFalse(result.isSufficientData());
        assertEquals(-1, result.getPeakHour());
        assertTrue(result.getWindowDescription().contains("Insufficient Data"));
    }

    @Test
    public void testPeakPostingHourIdentification() {
        List<Post> posts = new ArrayList<>();
        LocalDateTime base = LocalDateTime.now().withMinute(0);

        // 2 posts at 2 PM (14:00) with low engagement
        posts.add(new Post("1", 1L, Platform.INSTAGRAM, "A", base.withHour(14), "IMAGE", 10, 2, 0, null, ""));
        posts.add(new Post("2", 1L, Platform.INSTAGRAM, "B", base.withHour(14), "IMAGE", 15, 3, 0, null, ""));

        // 2 posts at 8 PM (20:00) with very high engagement
        posts.add(new Post("3", 1L, Platform.INSTAGRAM, "C", base.withHour(20), "IMAGE", 500, 100, 0, null, ""));
        posts.add(new Post("4", 1L, Platform.INSTAGRAM, "D", base.withHour(20), "IMAGE", 600, 150, 0, null, ""));

        // 1 post at 10 AM (10:00) with moderate engagement
        posts.add(new Post("5", 1L, Platform.INSTAGRAM, "E", base.withHour(10), "IMAGE", 100, 20, 0, null, ""));

        BestTimeAnalyzer.BestTimeResult result = analyzer.analyze(posts);

        assertTrue(result.isSufficientData());
        assertEquals(20, result.getPeakHour(), "Peak hour should be 20 (8 PM)");
        assertTrue(result.getWindowDescription().contains("8 PM"));
        assertEquals(675.0, result.getPeakAverageEngagement(), 0.01);
    }
}
