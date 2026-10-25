package com.socialanalytics.analytics;

import com.socialanalytics.model.Hashtag;
import com.socialanalytics.model.Post;
import com.socialanalytics.model.Trend;
import com.socialanalytics.model.TrendResult;
import com.socialanalytics.util.TextUtil;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Compares topic frequencies and engagement between historical and recent periods
 * to identify rising, falling, and emerging trends.
 */
public class TrendAnalyzer {
    private final TrendScoringService scoringService;

    public TrendAnalyzer() {
        this.scoringService = new TrendScoringService();
    }

    public TrendAnalyzer(TrendScoringService scoringService) {
        this.scoringService = scoringService;
    }

    public TrendResult analyze(List<Post> posts) {
        TrendResult result = new TrendResult();
        if (posts == null || posts.isEmpty()) {
            return result;
        }

        // Sort posts chronologically (oldest first, newest last) to guarantee proper historical vs recent splitting
        List<Post> sortedPosts = new ArrayList<>(posts);
        sortedPosts.sort(java.util.Comparator.comparing(p -> p.getTimestamp() != null ? p.getTimestamp() : LocalDateTime.MIN));

        LocalDateTime minDate = sortedPosts.get(0).getTimestamp() != null ? sortedPosts.get(0).getTimestamp() : LocalDateTime.now();
        LocalDateTime maxDate = sortedPosts.get(sortedPosts.size() - 1).getTimestamp() != null ? sortedPosts.get(sortedPosts.size() - 1).getTimestamp() : minDate;
        result.setPeriodStart(minDate);
        result.setPeriodEnd(maxDate);

        // Calculate midpoint with fallback if timestamps are clustered
        Duration duration = Duration.between(minDate, maxDate);
        LocalDateTime splitDate;
        if (Math.abs(duration.toHours()) >= 24) {
            splitDate = minDate.plusSeconds(duration.getSeconds() / 2);
        } else {
            // Split chronologically by collection index if posts are within the same 24 hours
            int midIdx = sortedPosts.size() / 2;
            splitDate = (midIdx < sortedPosts.size() && sortedPosts.get(midIdx).getTimestamp() != null)
                    ? sortedPosts.get(midIdx).getTimestamp()
                    : minDate;
        }

        Map<String, Long> prevMentions = new HashMap<>();
        Map<String, Long> currMentions = new HashMap<>();
        Map<String, Long> currInteractions = new HashMap<>();
        Map<String, LocalDateTime> latestPostTime = new HashMap<>();

        for (int i = 0; i < sortedPosts.size(); i++) {
            Post p = sortedPosts.get(i);
            LocalDateTime ts = p.getTimestamp() != null ? p.getTimestamp() : LocalDateTime.now();
            boolean isCurrentPeriod;
            if (Math.abs(duration.toHours()) >= 24) {
                isCurrentPeriod = ts.isAfter(splitDate) || ts.isEqual(splitDate);
            } else {
                isCurrentPeriod = (i >= sortedPosts.size() / 2);
            }

            // Extract topics and hashtags - prioritize explicit hashtags
            Set<String> topics = new HashSet<>();
            for (String h : p.getHashtags()) {
                if (h != null && !h.isBlank()) {
                    topics.add("#" + h.toLowerCase());
                }
            }
            for (String kw : TextUtil.extractKeywords(p.getContent())) {
                if (!topics.contains("#" + kw)) {
                    topics.add(kw);
                }
            }

            for (String topic : topics) {
                if (isCurrentPeriod) {
                    currMentions.put(topic, currMentions.getOrDefault(topic, 0L) + 1);
                    currInteractions.put(topic, currInteractions.getOrDefault(topic, 0L) + p.getTotalInteractions());

                    LocalDateTime prevLatest = latestPostTime.get(topic);
                    if (prevLatest == null || ts.isAfter(prevLatest)) {
                        latestPostTime.put(topic, ts);
                    }
                } else {
                    prevMentions.put(topic, prevMentions.getOrDefault(topic, 0L) + 1);
                }
            }
        }

        // Aggregate unique topics
        Set<String> allTopicNames = new HashSet<>();
        allTopicNames.addAll(prevMentions.keySet());
        allTopicNames.addAll(currMentions.keySet());

        // Determine max mentions to set threshold
        long maxTotalMentions = 0;
        for (String topicName : allTopicNames) {
            long total = prevMentions.getOrDefault(topicName, 0L) + currMentions.getOrDefault(topicName, 0L);
            if (total > maxTotalMentions) maxTotalMentions = total;
        }
        // Minimum mention threshold: require at least 2 mentions across corpus if multi-mention topics exist
        long minThreshold = (maxTotalMentions >= 2) ? 2 : 1;

        List<Trend> trendList = new ArrayList<>();
        long maxMentions = 1;
        double maxAvgInteractions = 1.0;

        for (String topicName : allTopicNames) {
            long prev = prevMentions.getOrDefault(topicName, 0L);
            long curr = currMentions.getOrDefault(topicName, 0L);
            long total = prev + curr;

            if (total < minThreshold) {
                continue; // Suppress single-occurrence noise
            }

            long eng = currInteractions.getOrDefault(topicName, 0L);
            Trend t = new Trend(topicName, prev, curr, eng);
            trendList.add(t);

            if (curr > maxMentions) maxMentions = curr;
            if (t.getAverageInteractions() > maxAvgInteractions) maxAvgInteractions = t.getAverageInteractions();
        }

        LocalDateTime now = LocalDateTime.now();
        List<Trend> rising = new ArrayList<>();
        List<Trend> falling = new ArrayList<>();
        List<Trend> emerging = new ArrayList<>();

        for (Trend t : trendList) {
            LocalDateTime latest = latestPostTime.get(t.getTopic());
            double daysElapsed = (latest != null) ? Math.max(0, Duration.between(latest, now).toDays()) : 14.0;

            scoringService.scoreTrend(t, maxMentions, maxAvgInteractions, daysElapsed);

            if (t.getStatus() == Trend.TrendStatus.RISING) {
                rising.add(t);
            } else if (t.getStatus() == Trend.TrendStatus.FALLING) {
                falling.add(t);
            } else if (t.getStatus() == Trend.TrendStatus.EMERGING) {
                emerging.add(t);
            }
        }

        // Sort descending by Trend Score
        Collections.sort(trendList);
        Collections.sort(rising);
        Collections.sort(falling);
        Collections.sort(emerging);

        result.setAllTrends(trendList);
        result.setRisingTopics(rising.size() > 5 ? rising.subList(0, 5) : rising);
        result.setFallingTopics(falling.size() > 5 ? falling.subList(0, 5) : falling);
        result.setEmergingTopics(emerging.size() > 5 ? emerging.subList(0, 5) : emerging);

        return result;
    }
}
