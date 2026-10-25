package com.socialanalytics.analytics;

import com.socialanalytics.model.Hashtag;
import com.socialanalytics.model.Post;
import com.socialanalytics.util.TextUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * Performs content analysis identifying top hashtags, most frequent topics,
 * best-performing topics, and best-performing content types using Java Collections.
 */
public class ContentAnalyzer {

    public static class ContentAnalysisResult {
        private List<Hashtag> topHashtags = new ArrayList<>();
        private List<TopicMetric> frequentTopics = new ArrayList<>();
        private List<TopicMetric> bestPerformingTopics = new ArrayList<>();
        private Map<String, Double> contentTypeAverageInteractions = new HashMap<>();
        private String bestContentType = "N/A";

        public List<Hashtag> getTopHashtags() {
            return topHashtags;
        }

        public List<TopicMetric> getFrequentTopics() {
            return frequentTopics;
        }

        public List<TopicMetric> getBestPerformingTopics() {
            return bestPerformingTopics;
        }

        public Map<String, Double> getContentTypeAverageInteractions() {
            return contentTypeAverageInteractions;
        }

        public String getBestContentType() {
            return bestContentType;
        }
    }

    public static class TopicMetric {
        private final String topic;
        private final long frequency;
        private final long totalInteractions;
        private final double averageInteractions;

        public TopicMetric(String topic, long frequency, long totalInteractions) {
            this.topic = topic;
            this.frequency = frequency;
            this.totalInteractions = totalInteractions;
            this.averageInteractions = frequency > 0 ? (double) totalInteractions / frequency : 0.0;
        }

        public String getTopic() {
            return topic;
        }

        public long getFrequency() {
            return frequency;
        }

        public long getTotalInteractions() {
            return totalInteractions;
        }

        public double getAverageInteractions() {
            return averageInteractions;
        }
    }

    public ContentAnalysisResult analyze(List<Post> posts) {
        ContentAnalysisResult result = new ContentAnalysisResult();
        if (posts == null || posts.isEmpty()) {
            return result;
        }

        Map<String, Long> tagCount = new HashMap<>();
        Map<String, Long> tagInteractions = new HashMap<>();
        Map<String, Long> keywordCount = new HashMap<>();
        Map<String, Long> keywordInteractions = new HashMap<>();
        Map<String, Long> typeInteractions = new HashMap<>();
        Map<String, Integer> typeCounts = new HashMap<>();

        for (Post p : posts) {
            long interactions = p.getTotalInteractions();

            // 1. Hashtags
            Set<String> uniquePostTags = new HashSet<>(p.getHashtags());
            for (String tag : uniquePostTags) {
                tagCount.put(tag, tagCount.getOrDefault(tag, 0L) + 1);
                tagInteractions.put(tag, tagInteractions.getOrDefault(tag, 0L) + interactions);
            }

            // 2. Keywords / Topics
            List<String> keywords = TextUtil.extractKeywords(p.getContent());
            Set<String> uniquePostKeywords = new HashSet<>(keywords);
            for (String kw : uniquePostKeywords) {
                keywordCount.put(kw, keywordCount.getOrDefault(kw, 0L) + 1);
                keywordInteractions.put(kw, keywordInteractions.getOrDefault(kw, 0L) + interactions);
            }

            // 3. Content Type
            String type = p.getContentType() != null ? p.getContentType().toUpperCase() : "TEXT";
            typeCounts.put(type, typeCounts.getOrDefault(type, 0) + 1);
            typeInteractions.put(type, typeInteractions.getOrDefault(type, 0L) + interactions);
        }

        // Use PriorityQueue to extract Top 10 Hashtags by frequency
        PriorityQueue<Hashtag> tagHeap = new PriorityQueue<>(10, Comparator.comparingLong(Hashtag::getCount));
        for (Map.Entry<String, Long> entry : tagCount.entrySet()) {
            String tag = entry.getKey();
            long count = entry.getValue();
            long totalEng = tagInteractions.getOrDefault(tag, 0L);
            Hashtag h = new Hashtag(tag, count, totalEng);
            tagHeap.offer(h);
            if (tagHeap.size() > 10) {
                tagHeap.poll();
            }
        }
        List<Hashtag> topHashtags = new ArrayList<>(tagHeap);
        topHashtags.sort(Collections.reverseOrder());
        result.topHashtags = topHashtags;

        // Use PriorityQueue to extract Top 10 Frequent Topics
        PriorityQueue<TopicMetric> freqHeap = new PriorityQueue<>(10, Comparator.comparingLong(TopicMetric::getFrequency));
        for (Map.Entry<String, Long> entry : keywordCount.entrySet()) {
            String kw = entry.getKey();
            long count = entry.getValue();
            long eng = keywordInteractions.getOrDefault(kw, 0L);
            TopicMetric tm = new TopicMetric(kw, count, eng);
            freqHeap.offer(tm);
            if (freqHeap.size() > 10) {
                freqHeap.poll();
            }
        }
        List<TopicMetric> freqTopics = new ArrayList<>(freqHeap);
        freqTopics.sort((a, b) -> Long.compare(b.getFrequency(), a.getFrequency()));
        result.frequentTopics = freqTopics;

        // Best performing topics (min 2 occurrences, highest average interactions)
        List<TopicMetric> bestTopics = new ArrayList<>();
        for (Map.Entry<String, Long> entry : keywordCount.entrySet()) {
            if (entry.getValue() >= 2) {
                String kw = entry.getKey();
                bestTopics.add(new TopicMetric(kw, entry.getValue(), keywordInteractions.getOrDefault(kw, 0L)));
            }
        }
        bestTopics.sort((a, b) -> Double.compare(b.getAverageInteractions(), a.getAverageInteractions()));
        if (bestTopics.size() > 5) {
            bestTopics = bestTopics.subList(0, 5);
        }
        result.bestPerformingTopics = bestTopics;

        // Best Content Type
        String bestType = "TEXT";
        double maxAvg = -1.0;
        for (Map.Entry<String, Integer> entry : typeCounts.entrySet()) {
            String type = entry.getKey();
            int count = entry.getValue();
            long total = typeInteractions.getOrDefault(type, 0L);
            double avg = count > 0 ? (double) total / count : 0.0;
            result.contentTypeAverageInteractions.put(type, avg);
            if (avg > maxAvg) {
                maxAvg = avg;
                bestType = type;
            }
        }
        result.bestContentType = bestType;

        return result;
    }
}
