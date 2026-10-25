package com.socialanalytics;

import com.socialanalytics.analytics.TrendAnalyzer;
import com.socialanalytics.analytics.TrendScoringService;
import com.socialanalytics.model.AppMode;
import com.socialanalytics.model.Platform;
import com.socialanalytics.model.Post;
import com.socialanalytics.model.Trend;
import com.socialanalytics.model.TrendResult;
import com.socialanalytics.service.AnalyticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PlatformDataIsolationTest {

    private AnalyticsService analyticsService;
    private TrendAnalyzer trendAnalyzer;

    @BeforeEach
    public void setup() {
        analyticsService = AnalyticsService.getInstance();
        analyticsService.invalidateCache();
        trendAnalyzer = new TrendAnalyzer(new TrendScoringService(0.25, 0.35, 0.25, 0.15));
    }

    @Test
    @DisplayName("Facebook selection in Live Mode never returns Instagram posts")
    public void testFacebookSelectionDoesNotLeakInstagramPosts() {
        Long userId = 1L;

        // Query Facebook posts in Live mode
        List<Post> fbPosts = analyticsService.getPostsForUser(userId, AppMode.LIVE, Platform.FACEBOOK);
        assertNotNull(fbPosts);

        // Every post returned MUST be Facebook
        for (Post p : fbPosts) {
            assertEquals(Platform.FACEBOOK, p.getPlatform(), "Facebook filter must never leak non-Facebook posts");
        }
    }

    @Test
    @DisplayName("Demo Mode filters strictly by requested platform")
    public void testDemoModePlatformIsolation() {
        Long userId = 1L;

        List<Post> igDemoPosts = analyticsService.getPostsForUser(userId, AppMode.DEMO, Platform.INSTAGRAM);
        assertNotNull(igDemoPosts);
        assertFalse(igDemoPosts.isEmpty(), "Demo mode must provide Instagram posts");
        for (Post p : igDemoPosts) {
            assertEquals(Platform.INSTAGRAM, p.getPlatform());
        }

        List<Post> fbDemoPosts = analyticsService.getPostsForUser(userId, AppMode.DEMO, Platform.FACEBOOK);
        assertNotNull(fbDemoPosts);
        assertFalse(fbDemoPosts.isEmpty(), "Demo mode must provide Facebook posts");
        for (Post p : fbDemoPosts) {
            assertEquals(Platform.FACEBOOK, p.getPlatform());
        }

        List<Post> xDemoPosts = analyticsService.getPostsForUser(userId, AppMode.DEMO, Platform.X);
        assertNotNull(xDemoPosts);
        assertFalse(xDemoPosts.isEmpty(), "Demo mode must provide X posts");
        for (Post p : xDemoPosts) {
            assertEquals(Platform.X, p.getPlatform());
        }

        List<Post> liDemoPosts = analyticsService.getPostsForUser(userId, AppMode.DEMO, Platform.LINKEDIN);
        assertNotNull(liDemoPosts);
        assertFalse(liDemoPosts.isEmpty(), "Demo mode must provide LinkedIn posts");
        for (Post p : liDemoPosts) {
            assertEquals(Platform.LINKEDIN, p.getPlatform());
        }
    }

    @Test
    @DisplayName("TrendAnalyzer chronological ordering and emerging topic detection")
    public void testTrendAnalyzerChronologicalSplitting() {
        List<Post> posts = new ArrayList<>();
        LocalDateTime baseTime = LocalDateTime.now().minusDays(10);

        // Older posts (5 days ago)
        for (int i = 0; i < 4; i++) {
            Post p = new Post();
            p.setId("old_" + i);
            p.setPlatform(Platform.INSTAGRAM);
            p.setContent("Classic cinema film discussion #cinema");
            p.setTimestamp(baseTime.plusDays(i));
            p.setLikes(100);
            p.setCommentsCount(10);
            p.addHashtag("cinema");
            posts.add(p);
        }

        // Newer posts (past 2 days)
        for (int i = 0; i < 4; i++) {
            Post p = new Post();
            p.setId("new_" + i);
            p.setPlatform(Platform.INSTAGRAM);
            p.setContent("Exciting trailer launch #thalapathy #leo");
            p.setTimestamp(baseTime.plusDays(7 + i));
            p.setLikes(500);
            p.setCommentsCount(50);
            p.addHashtag("thalapathy");
            p.addHashtag("leo");
            posts.add(p);
        }

        TrendResult result = trendAnalyzer.analyze(posts);
        assertNotNull(result);
        assertNotNull(result.getAllTrends());
        assertFalse(result.getAllTrends().isEmpty());

        // Verify emerging topics that only appeared recently
        boolean foundEmerging = false;
        for (Trend t : result.getAllTrends()) {
            if (t.getTopic().equals("#thalapathy") || t.getTopic().equals("#leo")) {
                assertEquals(Trend.TrendStatus.EMERGING, t.getStatus(), "Topics appearing only in recent period must be EMERGING");
                assertFalse(t.isHasGrowthBaseline(), "Emerging topic should not have previous baseline");
                assertEquals(0.0, t.getGrowthPercentage(), "Emerging topic growth should be 0.0 rather than fake +100%");
                foundEmerging = true;
            }
        }
        assertTrue(foundEmerging, "Should have identified #thalapathy or #leo as emerging");
    }
}
