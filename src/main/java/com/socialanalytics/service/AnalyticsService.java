package com.socialanalytics.service;

import com.socialanalytics.analytics.MultithreadedAnalyticsEngine;
import com.socialanalytics.dao.AnalyticsDAO;
import com.socialanalytics.dao.CommentDAO;
import com.socialanalytics.dao.PostDAO;
import com.socialanalytics.dao.SocialAccountDAO;
import com.socialanalytics.dao.impl.JdbcAnalyticsDAO;
import com.socialanalytics.dao.impl.JdbcCommentDAO;
import com.socialanalytics.dao.impl.JdbcPostDAO;
import com.socialanalytics.dao.impl.JdbcSocialAccountDAO;
import com.socialanalytics.connector.demo.DemoDataGenerator;
import com.socialanalytics.model.AnalyticsSummary;
import com.socialanalytics.model.AppMode;
import com.socialanalytics.model.Comment;
import com.socialanalytics.model.Platform;
import com.socialanalytics.model.Post;
import com.socialanalytics.model.SocialAccount;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Service managing analytics execution, caching, and persistence across user datasets.
 * Enforces strict separation between LIVE mode (real connected accounts only) and
 * DEMO mode (isolated simulated preview only).
 */
public class AnalyticsService {
    private static final AnalyticsService INSTANCE = new AnalyticsService();
    private final PostDAO postDAO;
    private final CommentDAO commentDAO;
    private final SocialAccountDAO socialAccountDAO;
    private final AnalyticsDAO analyticsDAO;
    private final MultithreadedAnalyticsEngine multithreadedEngine;

    private AnalyticsSummary cachedSummary;
    private Long cachedUserId;
    private AppMode cachedMode;
    private Platform cachedPlatformFilter;

    public AnalyticsService() {
        this.postDAO = new JdbcPostDAO();
        this.commentDAO = new JdbcCommentDAO();
        this.socialAccountDAO = new JdbcSocialAccountDAO();
        this.analyticsDAO = new JdbcAnalyticsDAO();
        this.multithreadedEngine = new MultithreadedAnalyticsEngine();
    }

    public AnalyticsService(PostDAO postDAO, CommentDAO commentDAO,
                            SocialAccountDAO socialAccountDAO, AnalyticsDAO analyticsDAO,
                            MultithreadedAnalyticsEngine engine) {
        this.postDAO = postDAO;
        this.commentDAO = commentDAO;
        this.socialAccountDAO = socialAccountDAO;
        this.analyticsDAO = analyticsDAO;
        this.multithreadedEngine = engine;
    }

    public static AnalyticsService getInstance() {
        return INSTANCE;
    }

    /**
     * Executes multithreaded analytics defaulting to LIVE mode.
     */
    public synchronized AnalyticsSummary runAnalyticsForUser(
            Long userId,
            Platform platformFilter,
            MultithreadedAnalyticsEngine.ProgressCallback callback) {
        return runAnalyticsForUser(userId, platformFilter, AppMode.LIVE, callback);
    }

    /**
     * Executes multithreaded analytics enforcing strict data separation by AppMode.
     *
     * LIVE MODE: Reads only genuine data from connected accounts.
     * If no accounts or posts exist, returns an accurate empty state without loading simulated data.
     *
     * DEMO MODE: Reads isolated simulated data for preview/testing without modifying real accounts.
     */
    public synchronized AnalyticsSummary runAnalyticsForUser(
            Long userId,
            Platform platformFilter,
            AppMode mode,
            MultithreadedAnalyticsEngine.ProgressCallback callback) {

        if (mode == null) {
            mode = AppMode.LIVE;
        }

        if (mode == AppMode.LIVE) {
            return runLiveAnalytics(userId, platformFilter, callback);
        } else {
            return runDemoAnalytics(platformFilter, callback);
        }
    }

    private AnalyticsSummary runLiveAnalytics(Long userId, Platform platformFilter, MultithreadedAnalyticsEngine.ProgressCallback callback) {
        List<SocialAccount> allAccounts = socialAccountDAO.findByUserId(userId);
        List<SocialAccount> liveAccounts = allAccounts.stream()
                .filter(a -> a.isConnected() && !a.isDemo())
                .filter(a -> platformFilter == null || a.getPlatform() == platformFilter)
                .toList();

        // If no real accounts are connected for this filter, return clean empty state
        if (liveAccounts.isEmpty()) {
            AnalyticsSummary emptySummary = new AnalyticsSummary();
            emptySummary.setTotalPosts(0);
            emptySummary.setTotalInteractions(0);
            emptySummary.setAverageEngagementRate(0.0);
            emptySummary.setBestPostingTimeWindow("Insufficient Data (Connect an account)");
            emptySummary.setDemoData(false);
            emptySummary.setLiveDataAvailable(false);
            emptySummary.setDataSourceDescription("No social accounts connected yet. Connect a platform to start analyzing your social-media data.");
            this.cachedSummary = emptySummary;
            this.cachedUserId = userId;
            this.cachedMode = AppMode.LIVE;
            this.cachedPlatformFilter = platformFilter;
            return emptySummary;
        }

        List<Post> rawPosts = (platformFilter != null)
                ? postDAO.findByPlatform(userId, platformFilter)
                : postDAO.findByUserId(userId);

        // Filter strictly to non-demo posts
        List<Post> livePosts = rawPosts.stream()
                .filter(p -> p.getId() != null && !p.getId().startsWith("demo_"))
                .toList();

        String accountHandle = liveAccounts.get(0).getUsername();
        String platformName = liveAccounts.get(0).getPlatform().getDisplayName();

        // If connected account has 0 posts retrieved
        if (livePosts.isEmpty()) {
            AnalyticsSummary zeroPostSummary = new AnalyticsSummary();
            zeroPostSummary.setTotalPosts(0);
            zeroPostSummary.setTotalInteractions(0);
            zeroPostSummary.setAverageEngagementRate(0.0);
            zeroPostSummary.setBestPostingTimeWindow("Insufficient Data (0 posts accessible)");
            zeroPostSummary.setDemoData(false);
            zeroPostSummary.setLiveDataAvailable(false);
            zeroPostSummary.setConnectedAccountName(accountHandle);
            zeroPostSummary.setDataSourceDescription("Connected to @" + accountHandle + " (" + platformName + ") via Live API. 0 posts are accessible or published on this account.");
            this.cachedSummary = zeroPostSummary;
            this.cachedUserId = userId;
            this.cachedMode = AppMode.LIVE;
            this.cachedPlatformFilter = platformFilter;
            return zeroPostSummary;
        }

        List<Comment> liveComments = commentDAO.findByUserId(userId).stream()
                .filter(c -> c.getPostId() != null && !c.getPostId().startsWith("demo_"))
                .toList();

        Map<Platform, Long> platformFollowers = new EnumMap<>(Platform.class);
        for (SocialAccount a : liveAccounts) {
            platformFollowers.put(a.getPlatform(), a.getFollowerCount());
        }

        AnalyticsSummary summary = multithreadedEngine.runAnalytics(livePosts, liveComments, platformFollowers, callback);
        summary.setDemoData(false);
        summary.setLiveDataAvailable(true);
        summary.setConnectedAccountName(accountHandle);
        summary.setDataSourceDescription("Live Account Data – Retrieved directly from official API for @" + accountHandle + " (" + platformName + ").");

        // Persist real analytics snapshot
        if (summary.getTotalPosts() > 0) {
            analyticsDAO.saveEngagementMetrics(userId, summary.getEngagement());
            analyticsDAO.saveSentimentResults(userId, summary.getSentiment());
            if (summary.getTrends() != null) {
                analyticsDAO.saveTrendResults(userId, summary.getTrends().getAllTrends());
            }
            analyticsDAO.saveRecommendations(userId, summary.getRecommendations());
        }

        this.cachedSummary = summary;
        this.cachedUserId = userId;
        this.cachedMode = AppMode.LIVE;
        this.cachedPlatformFilter = platformFilter;
        return summary;
    }

    private AnalyticsSummary runDemoAnalytics(Platform platformFilter, MultithreadedAnalyticsEngine.ProgressCallback callback) {
        List<Post> demoPosts = new ArrayList<>();
        if (platformFilter != null) {
            demoPosts.addAll(DemoDataGenerator.generateDemoPosts((long) (platformFilter.ordinal() + 1), platformFilter));
        } else {
            for (Platform p : Platform.values()) {
                demoPosts.addAll(DemoDataGenerator.generateDemoPosts((long) (p.ordinal() + 1), p));
            }
        }

        List<Comment> demoComments = new ArrayList<>();
        for (Post p : demoPosts) {
            demoComments.addAll(DemoDataGenerator.generateDemoComments(p.getId(), p.getPlatform()));
        }

        Map<Platform, Long> demoFollowers = new EnumMap<>(Platform.class);
        for (Platform p : Platform.values()) {
            demoFollowers.put(p, 25000L);
        }
        AnalyticsSummary summary = multithreadedEngine.runAnalytics(demoPosts, demoComments, demoFollowers, callback);
        summary.setDemoData(true);
        summary.setLiveDataAvailable(false);
        summary.setConnectedAccountName("Simulated Demo Channel");
        summary.setDataSourceDescription("DEMO MODE — SIMULATED DATA (Illustrative sample data, not retrieved from real social accounts).");

        this.cachedSummary = summary;
        this.cachedUserId = null;
        this.cachedMode = AppMode.DEMO;
        this.cachedPlatformFilter = platformFilter;
        return summary;
    }

    public synchronized AnalyticsSummary getCachedSummary(Long userId) {
        return getCachedSummary(userId, cachedMode != null ? cachedMode : AppMode.LIVE);
    }

    public synchronized AnalyticsSummary getCachedSummary(Long userId, AppMode mode) {
        if (cachedSummary != null && cachedMode == mode && (mode == AppMode.DEMO || (cachedUserId != null && cachedUserId.equals(userId)))) {
            return cachedSummary;
        }
        return runAnalyticsForUser(userId, null, mode, null);
    }

    public synchronized void invalidateCache() {
        this.cachedSummary = null;
        this.cachedUserId = null;
        this.cachedMode = null;
        this.cachedPlatformFilter = null;
    }

    public List<Post> getPostsForUser(Long userId) {
        return getPostsForUser(userId, cachedMode != null ? cachedMode : AppMode.LIVE, cachedPlatformFilter);
    }

    public List<Post> getPostsForUser(Long userId, AppMode mode) {
        return getPostsForUser(userId, mode, cachedPlatformFilter);
    }

    public List<Post> getPostsForUser(Long userId, AppMode mode, Platform platformFilter) {
        if (mode == AppMode.DEMO) {
            List<Post> demoPosts = new ArrayList<>();
            if (platformFilter != null) {
                demoPosts.addAll(DemoDataGenerator.generateDemoPosts((long) (platformFilter.ordinal() + 1), platformFilter));
            } else {
                for (Platform p : Platform.values()) {
                    demoPosts.addAll(DemoDataGenerator.generateDemoPosts((long) (p.ordinal() + 1), p));
                }
            }
            return demoPosts;
        }

        List<Post> rawPosts = (platformFilter != null)
                ? postDAO.findByPlatform(userId, platformFilter)
                : postDAO.findByUserId(userId);

        return rawPosts.stream()
                .filter(p -> p.getId() != null && !p.getId().startsWith("demo_"))
                .toList();
    }
}
