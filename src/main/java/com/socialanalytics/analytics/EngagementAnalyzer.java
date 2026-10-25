package com.socialanalytics.analytics;

import com.socialanalytics.model.Engagement;
import com.socialanalytics.model.Platform;
import com.socialanalytics.model.Post;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Performs comprehensive engagement analysis across multi-platform post collections.
 * Handles nullable / optional metrics (such as impressions) transparently.
 */
public class EngagementAnalyzer {

    public Engagement analyze(List<Post> posts, long followerCount) {
        Map<Platform, Long> map = new EnumMap<>(Platform.class);
        if (posts != null && !posts.isEmpty() && posts.get(0).getPlatform() != null) {
            map.put(posts.get(0).getPlatform(), followerCount);
        }
        return analyze(posts, map);
    }

    public Engagement analyze(List<Post> posts, Map<Platform, Long> platformFollowers) {
        Engagement engagement = new Engagement();
        if (posts == null || posts.isEmpty()) {
            engagement.setTotalShares(0L);
            engagement.setSharesAvailable(false);
            engagement.setEngagementRate(null);
            engagement.setEngagementRateAvailable(false);
            return engagement;
        }

        engagement.setTotalPosts(posts.size());

        long sumLikes = 0;
        long sumComments = 0;
        long sumShares = 0;
        long sumViews = 0;
        boolean sharesAvailable = false;
        boolean viewsAvailable = false;

        Map<Platform, Engagement.PlatformBreakdown> platformMap = new EnumMap<>(Platform.class);

        // Min-heap for Top-5 posts and Max-heap for Bottom-5 posts
        PriorityQueue<Post> topQueue = new PriorityQueue<>(Comparator.comparingLong(Post::getTotalInteractions));
        PriorityQueue<Post> bottomQueue = new PriorityQueue<>((a, b) -> Long.compare(b.getTotalInteractions(), a.getTotalInteractions()));

        for (Post p : posts) {
            sumLikes += p.getLikes();
            sumComments += p.getCommentsCount();

            if (p.isSharesAvailable()) {
                sharesAvailable = true;
                sumShares += p.getShares();
            }

            if (p.isViewsAvailable()) {
                viewsAvailable = true;
                sumViews += p.getViews();
            }

            // Platform breakdown
            Engagement.PlatformBreakdown pb = platformMap.computeIfAbsent(p.getPlatform(), Engagement.PlatformBreakdown::new);
            pb.setPostsCount(pb.getPostsCount() + 1);
            pb.setLikes(pb.getLikes() + p.getLikes());
            pb.setComments(pb.getComments() + p.getCommentsCount());
            if (p.isSharesAvailable()) {
                long curShares = (pb.getShares() != null) ? pb.getShares() : 0L;
                pb.setShares(curShares + p.getShares());
                pb.setSharesAvailable(true);
            }
            if (p.isViewsAvailable()) {
                long currentViews = (pb.getViews() != null) ? pb.getViews() : 0L;
                pb.setViews(currentViews + p.getViews());
            }

            // Maintain top 5
            topQueue.offer(p);
            if (topQueue.size() > 5) {
                topQueue.poll();
            }

            // Maintain bottom 5
            bottomQueue.offer(p);
            if (bottomQueue.size() > 5) {
                bottomQueue.poll();
            }
        }

        engagement.setTotalLikes(sumLikes);
        engagement.setTotalComments(sumComments);
        engagement.setSharesAvailable(sharesAvailable);
        engagement.setTotalShares(sharesAvailable ? sumShares : null);
        engagement.setViewsAvailable(viewsAvailable);
        engagement.setTotalViews(viewsAvailable ? sumViews : null);

        int count = posts.size();
        engagement.setAverageLikes(count > 0 ? ((double) sumLikes / count) : 0.0);
        engagement.setAverageComments(count > 0 ? ((double) sumComments / count) : 0.0);
        engagement.setAverageShares(sharesAvailable && count > 0 ? ((double) sumShares / count) : null);

        // Engagement Rate Calculation:
        // Evaluated strictly per platform to eliminate follower cross-contamination between platforms
        double sumRates = 0.0;
        int validRateCount = 0;
        boolean hasImpressionsBasis = false;

        for (Engagement.PlatformBreakdown pb : platformMap.values()) {
            if (pb.getPostsCount() > 0) {
                pb.setAverageEngagement((double) pb.getTotalInteractions() / pb.getPostsCount());
                long pFollowers = (platformFollowers != null) ? platformFollowers.getOrDefault(pb.getPlatform(), 0L) : 0L;

                if (pb.getViews() != null && pb.getViews() > 0) {
                    double rate = ((double) pb.getTotalInteractions() / pb.getViews()) * 100.0;
                    sumRates += rate;
                    validRateCount++;
                    hasImpressionsBasis = true;
                } else if (pFollowers > 0) {
                    double avgInteractions = (double) pb.getTotalInteractions() / pb.getPostsCount();
                    double rate = (avgInteractions / pFollowers) * 100.0;
                    sumRates += rate;
                    validRateCount++;
                }
            }
        }

        if (validRateCount > 0) {
            engagement.setEngagementRate(sumRates / validRateCount);
            engagement.setEngagementRateAvailable(true);
            engagement.setEngagementRateBasis(hasImpressionsBasis ? "IMPRESSIONS" : "FOLLOWERS");
        } else {
            // Neither impressions nor followers are available on the analyzed platforms (e.g. basic Instagram API)
            engagement.setEngagementRate(null);
            engagement.setEngagementRateAvailable(false);
            engagement.setEngagementRateBasis("UNAVAILABLE");
        }

        engagement.setPlatformBreakdowns(platformMap);

        // Drain top posts in descending order
        List<Post> topPosts = new ArrayList<>(topQueue);
        topPosts.sort((a, b) -> Long.compare(b.getTotalInteractions(), a.getTotalInteractions()));
        engagement.setTopPosts(topPosts);

        // Drain lowest posts in ascending order
        List<Post> lowestPosts = new ArrayList<>(bottomQueue);
        lowestPosts.sort(Comparator.comparingLong(Post::getTotalInteractions));
        engagement.setLowestPosts(lowestPosts);

        return engagement;
    }
}
