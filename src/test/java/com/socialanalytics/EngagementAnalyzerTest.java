package com.socialanalytics;

import com.socialanalytics.analytics.EngagementAnalyzer;
import com.socialanalytics.model.Engagement;
import com.socialanalytics.model.Platform;
import com.socialanalytics.model.Post;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class EngagementAnalyzerTest {
    private EngagementAnalyzer analyzer;

    @BeforeEach
    public void setup() {
        analyzer = new EngagementAnalyzer();
    }

    @Test
    public void testEmptyPostList() {
        Engagement result = analyzer.analyze(new ArrayList<>(), 1000);
        assertEquals(0, result.getTotalPosts());
        assertEquals(0, result.getTotalLikes());
        assertEquals(0, result.getTotalComments());
        assertEquals(0, result.getTotalShares());
        assertNull(result.getTotalViews());
        assertFalse(result.isViewsAvailable());
    }

    @Test
    public void testBasicEngagementCalculations() {
        List<Post> posts = new ArrayList<>();

        Post p1 = new Post("1", 1L, Platform.INSTAGRAM, "Post 1", LocalDateTime.now(), "IMAGE", 100, 20, 0, 1000L, "");
        Post p2 = new Post("2", 1L, Platform.INSTAGRAM, "Post 2", LocalDateTime.now(), "REEL", 200, 40, 0, 2000L, "");

        posts.add(p1);
        posts.add(p2);

        Engagement result = analyzer.analyze(posts, 5000);

        assertEquals(2, result.getTotalPosts());
        assertEquals(300, result.getTotalLikes());
        assertEquals(60, result.getTotalComments());
        assertEquals(3000L, result.getTotalViews());
        assertTrue(result.isViewsAvailable());

        assertEquals(150.0, result.getAverageLikes());
        assertEquals(30.0, result.getAverageComments());

        // Impressions rate: 360 interactions / 3000 views = 12%
        assertEquals(12.0, result.getEngagementRate(), 0.01);
    }

    @Test
    public void testMissingViewsGracefulHandling() {
        List<Post> posts = new ArrayList<>();
        // Views are null (as happens with official Facebook/LinkedIn basic tier)
        Post p1 = new Post("fb_1", 2L, Platform.FACEBOOK, "FB Post", LocalDateTime.now(), "STATUS", 50, 10, 5, null, "");
        posts.add(p1);

        Engagement result = analyzer.analyze(posts, 1000);

        assertNull(result.getTotalViews());
        assertFalse(result.isViewsAvailable());
        assertEquals(50, result.getTotalLikes());
        assertEquals(10, result.getTotalComments());
        assertEquals(5, result.getTotalShares());
        assertEquals(65, result.getTotalInteractions());
    }

    @Test
    public void testTopAndLowestPostsIdentification() {
        List<Post> posts = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            Post p = new Post("p_" + i, 1L, Platform.X, "Tweet " + i, LocalDateTime.now(), "TWEET", i * 10, i * 2, i, (long) (i * 100), "");
            posts.add(p);
        }

        Engagement result = analyzer.analyze(posts, 10000);

        // Top 5 should be ordered descending
        assertEquals(5, result.getTopPosts().size());
        assertEquals("p_10", result.getTopPosts().get(0).getId());
        assertEquals("p_9", result.getTopPosts().get(1).getId());

        // Lowest 5 should be ordered ascending
        assertEquals(5, result.getLowestPosts().size());
        assertEquals("p_1", result.getLowestPosts().get(0).getId());
        assertEquals("p_2", result.getLowestPosts().get(1).getId());
    }

    @Test
    public void testEngagementRateUnavailableWhenNoFollowersAndNoViews() {
        List<Post> posts = new ArrayList<>();
        Post p1 = new Post("ig_1", 7L, Platform.INSTAGRAM, "Ig Reel", LocalDateTime.now(), "VIDEO", 500, 25, null, null, "");
        posts.add(p1);

        // 0 followers, null views -> cannot calculate meaningful engagement rate
        Engagement result = analyzer.analyze(posts, 0L);

        assertNull(result.getEngagementRate());
        assertFalse(result.isEngagementRateAvailable());
        assertEquals("UNAVAILABLE", result.getEngagementRateBasis());
        assertFalse(result.isSharesAvailable());
        assertNull(result.getTotalShares());
        assertEquals(525L, p1.getTotalInteractions());
    }

    @Test
    public void testFollowersBasedEngagementRate() {
        List<Post> posts = new ArrayList<>();
        // 2 posts, 200 total interactions, 100 avg interactions, 1000 followers -> 10.0%
        Post p1 = new Post("li_1", 9L, Platform.LINKEDIN, "Article 1", LocalDateTime.now(), "ARTICLE", 80, 20, 0, null, "");
        Post p2 = new Post("li_2", 9L, Platform.LINKEDIN, "Article 2", LocalDateTime.now(), "ARTICLE", 90, 10, 0, null, "");
        posts.add(p1);
        posts.add(p2);

        Engagement result = analyzer.analyze(posts, 1000L);

        assertNotNull(result.getEngagementRate());
        assertTrue(result.isEngagementRateAvailable());
        assertEquals("FOLLOWERS", result.getEngagementRateBasis());
        assertEquals(10.0, result.getEngagementRate(), 0.001);
    }

    @Test
    public void testPlatformIsolationNoCrossContamination() {
        List<Post> posts = new ArrayList<>();
        // Instagram posts with 0 followers
        Post p1 = new Post("ig_1", 7L, Platform.INSTAGRAM, "IG Post", LocalDateTime.now(), "VIDEO", 1000, 50, null, null, "");
        posts.add(p1);

        java.util.Map<Platform, Long> platformFollowers = new java.util.EnumMap<>(Platform.class);
        platformFollowers.put(Platform.INSTAGRAM, 0L);
        platformFollowers.put(Platform.LINKEDIN, 1250L); // LinkedIn has 1250 followers, but IG post must NOT use it!

        Engagement result = analyzer.analyze(posts, platformFollowers);

        // Instagram rate must be UNAVAILABLE, not 1050/1250 = 84%
        assertNull(result.getEngagementRate());
        assertFalse(result.isEngagementRateAvailable());
        assertEquals("UNAVAILABLE", result.getEngagementRateBasis());
    }
}
