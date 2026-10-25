package com.socialanalytics.analytics;

import com.socialanalytics.analytics.BestTimeAnalyzer.BestTimeResult;
import com.socialanalytics.analytics.ContentAnalyzer.ContentAnalysisResult;
import com.socialanalytics.model.AnalyticsSummary;
import com.socialanalytics.model.Comment;
import com.socialanalytics.model.Engagement;
import com.socialanalytics.model.Platform;
import com.socialanalytics.model.Post;
import com.socialanalytics.model.Recommendation;
import com.socialanalytics.model.SentimentResult;
import com.socialanalytics.model.TrendResult;
import com.socialanalytics.recommendation.RecommendationEngine;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Concurrently orchestrates independent analytics engines using Java Multithreading.
 * Dispatches Engagement, Sentiment, Content, Trend, and Best Time tasks across a managed
 * thread pool via Callable and Future, with thread-safe progress updates.
 */
public class MultithreadedAnalyticsEngine {

    @FunctionalInterface
    public interface ProgressCallback {
        void onProgress(double progressPercent, String taskName);
    }

    private final ExecutorService executor;
    private final EngagementAnalyzer engagementAnalyzer;
    private final SentimentAnalyzer sentimentAnalyzer;
    private final ContentAnalyzer contentAnalyzer;
    private final TrendAnalyzer trendAnalyzer;
    private final BestTimeAnalyzer bestTimeAnalyzer;
    private final RecommendationEngine recommendationEngine;

    public MultithreadedAnalyticsEngine() {
        this.executor = Executors.newFixedThreadPool(5, new ThreadFactory() {
            private final AtomicInteger threadCount = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "AnalyticsWorker-" + threadCount.getAndIncrement());
                t.setDaemon(true);
                return t;
            }
        });

        this.engagementAnalyzer = new EngagementAnalyzer();
        this.sentimentAnalyzer = new LexiconSentimentAnalyzer();
        this.contentAnalyzer = new ContentAnalyzer();
        this.trendAnalyzer = new TrendAnalyzer();
        this.bestTimeAnalyzer = new BestTimeAnalyzer();
        this.recommendationEngine = new RecommendationEngine();
    }

    /**
     * Executes concurrent analytics over the post and comment datasets.
     *
     * @param posts collection of posts to analyze
     * @param comments collection of comments to analyze
     * @param followerCount baseline follower count for rate calculations
     * @param callback optional thread-safe callback for progress notifications
     * @return fully populated AnalyticsSummary
     */
    public AnalyticsSummary runAnalytics(
            List<Post> posts,
            List<Comment> comments,
            long followerCount,
            ProgressCallback callback) {
        Map<Platform, Long> platformFollowers = new EnumMap<>(Platform.class);
        if (posts != null && !posts.isEmpty() && posts.get(0).getPlatform() != null) {
            platformFollowers.put(posts.get(0).getPlatform(), followerCount);
        }
        return runAnalytics(posts, comments, platformFollowers, callback);
    }

    public AnalyticsSummary runAnalytics(
            List<Post> posts,
            List<Comment> comments,
            Map<Platform, Long> platformFollowers,
            ProgressCallback callback) {

        if (callback != null) callback.onProgress(0.05, "Spawning concurrent analytics workers...");

        // Task 1: Engagement Analysis
        Callable<Engagement> engagementTask = () -> {
            Engagement result = engagementAnalyzer.analyze(posts, platformFollowers);
            if (callback != null) callback.onProgress(0.30, "Engagement Analysis complete");
            return result;
        };

        // Task 2: Sentiment Analysis
        Callable<SentimentResult> sentimentTask = () -> {
            SentimentResult result = sentimentAnalyzer.analyzeBatch(posts, comments);
            if (callback != null) callback.onProgress(0.50, "Sentiment Analysis complete");
            return result;
        };

        // Task 3: Content Analysis
        Callable<ContentAnalysisResult> contentTask = () -> {
            ContentAnalysisResult result = contentAnalyzer.analyze(posts);
            if (callback != null) callback.onProgress(0.65, "Content & Keyword Analysis complete");
            return result;
        };

        // Task 4: Trend Analysis
        Callable<TrendResult> trendTask = () -> {
            TrendResult result = trendAnalyzer.analyze(posts);
            if (callback != null) callback.onProgress(0.80, "Trend Scoring complete");
            return result;
        };

        // Task 5: Best Posting Time Analysis
        Callable<BestTimeResult> bestTimeTask = () -> {
            BestTimeResult result = bestTimeAnalyzer.analyze(posts);
            if (callback != null) callback.onProgress(0.90, "Optimal Schedule Analysis complete");
            return result;
        };

        // Dispatch all tasks concurrently
        Future<Engagement> futureEngagement = executor.submit(engagementTask);
        Future<SentimentResult> futureSentiment = executor.submit(sentimentTask);
        Future<ContentAnalysisResult> futureContent = executor.submit(contentTask);
        Future<TrendResult> futureTrend = executor.submit(trendTask);
        Future<BestTimeResult> futureBestTime = executor.submit(bestTimeTask);

        try {
            Engagement engagement = futureEngagement.get();
            SentimentResult sentiment = futureSentiment.get();
            ContentAnalysisResult content = futureContent.get();
            TrendResult trends = futureTrend.get();
            BestTimeResult bestTime = futureBestTime.get();

            if (callback != null) callback.onProgress(0.95, "Synthesizing rule-based recommendations...");

            // Recommendation generation based on concurrent outputs
            List<Recommendation> recs = recommendationEngine.generateRecommendations(
                    engagement, trends, sentiment, bestTime, content
            );

            // Assemble summary snapshot
            AnalyticsSummary summary = new AnalyticsSummary();
            summary.setEngagement(engagement);
            summary.setSentiment(sentiment);
            summary.setTrends(trends);
            summary.setRecommendations(recs);

            summary.setTotalPosts(engagement.getTotalPosts());
            summary.setTotalInteractions(engagement.getTotalInteractions());
            summary.setAverageEngagementRate(engagement.getEngagementRate());
            summary.setEngagementRateAvailable(engagement.isEngagementRateAvailable());
            summary.setEngagementRateBasis(engagement.getEngagementRateBasis());
            if (!engagement.getTopPosts().isEmpty()) {
                summary.setTopPost(engagement.getTopPosts().get(0));
            }

            summary.setBestPostingTimeWindow(bestTime.getWindowDescription());
            summary.setBestPostingHour(bestTime.getPeakHour());
            summary.setHourlyEngagement(bestTime.getHourlyEngagement());
            summary.setContentTypePerformance(content.getContentTypeAverageInteractions());

            // Check if any post came from demo
            if (posts != null && !posts.isEmpty()) {
                boolean isDemo = posts.get(0).getId().startsWith("demo_");
                summary.setDemoData(isDemo);
            }

            if (callback != null) callback.onProgress(1.0, "All analytics completed successfully.");

            return summary;
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Concurrent analytics processing encountered an error: " + e.getMessage(), e);
        }
    }

    /**
     * Gracefully shuts down the thread pool.
     */
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(3, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
