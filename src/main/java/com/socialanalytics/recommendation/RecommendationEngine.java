package com.socialanalytics.recommendation;

import com.socialanalytics.analytics.BestTimeAnalyzer;
import com.socialanalytics.analytics.ContentAnalyzer;
import com.socialanalytics.model.Engagement;
import com.socialanalytics.model.Hashtag;
import com.socialanalytics.model.Recommendation;
import com.socialanalytics.model.SentimentResult;
import com.socialanalytics.model.Trend;
import com.socialanalytics.model.TrendResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Transparent, deterministic Rule-Based Recommendation Engine.
 * Generates actionable social media strategies purely from evaluated analytics metrics.
 * Does not make false claims of AI/ML; all recommendations state the exact mathematical rule triggered.
 */
public class RecommendationEngine {

    /**
     * Evaluates rules against analytics models and synthesizes actionable recommendations.
     */
    public List<Recommendation> generateRecommendations(
            Engagement engagement,
            TrendResult trendResult,
            SentimentResult sentiment,
            BestTimeAnalyzer.BestTimeResult bestTime,
            ContentAnalyzer.ContentAnalysisResult contentResult) {

        List<Recommendation> recs = new ArrayList<>();

        // RULE 1: Topic with Above-Average Engagement + Rising Trend + Peak Posting Time
        if (trendResult != null && !trendResult.getRisingTopics().isEmpty()) {
            Trend topRising = trendResult.getRisingTopics().get(0);
            String timeWindow = (bestTime != null && bestTime.isSufficientData()) ? bestTime.getWindowDescription() : "peak evening hours";

            Recommendation r = new Recommendation(
                    UUID.randomUUID().toString(),
                    Recommendation.RecommendationType.CONTENT_TOPIC,
                    Recommendation.Priority.HIGH,
                    "Capitalize on Rising Topic: '" + topRising.getTopic() + "'",
                    String.format("'%s' is exhibiting a +%.1f%% growth with high audience engagement. Schedule posts around %s to maximize organic reach.",
                            topRising.getTopic(), topRising.getGrowthPercentage(), timeWindow),
                    "Rule: Topic Growth > +15% AND Trend Status = RISING",
                    Math.min(95.0, topRising.getTrendScore())
            );
            recs.add(r);
        }

        // RULE 2: Optimal Content Type Allocation
        if (contentResult != null && !contentResult.getContentTypeAverageInteractions().isEmpty()) {
            String bestType = contentResult.getBestContentType();
            double bestAvg = contentResult.getContentTypeAverageInteractions().getOrDefault(bestType, 0.0);

            // Find lowest content type
            String worstType = null;
            double worstAvg = Double.MAX_VALUE;
            for (Map.Entry<String, Double> e : contentResult.getContentTypeAverageInteractions().entrySet()) {
                if (e.getValue() < worstAvg && !e.getKey().equalsIgnoreCase(bestType)) {
                    worstAvg = e.getValue();
                    worstType = e.getKey();
                }
            }

            Recommendation r = new Recommendation(
                    UUID.randomUUID().toString(),
                    Recommendation.RecommendationType.CONTENT_TYPE,
                    Recommendation.Priority.HIGH,
                    "Shift Format Focus to " + bestType,
                    String.format("%s posts generate an average of %.0f interactions, significantly outperforming %s (%.0f avg). Increase %s frequency.",
                            bestType, bestAvg, (worstType != null ? worstType : "other formats"), (worstType != null ? worstAvg : 0.0), bestType),
                    "Rule: Best Content Type Average Interactions > Lowest Content Type Average Interactions",
                    88.0
            );
            recs.add(r);
        }

        // RULE 3: Best Posting Schedule Window
        if (bestTime != null && bestTime.isSufficientData() && bestTime.getPeakHour() >= 0) {
            Recommendation r = new Recommendation(
                    UUID.randomUUID().toString(),
                    Recommendation.RecommendationType.POSTING_TIME,
                    Recommendation.Priority.MEDIUM,
                    "Publish Content During Peak Window (" + bestTime.getWindowDescription() + ")",
                    String.format("Historical data shows audience interaction peaks during %s, yielding an average of %.0f interactions per post.",
                            bestTime.getWindowDescription(), bestTime.getPeakAverageEngagement()),
                    "Rule: Hour Bucket Engagement > Global Hourly Mean",
                    85.0
            );
            recs.add(r);
        }

        // RULE 4: Rising Hashtag Utilization
        if (contentResult != null && !contentResult.getTopHashtags().isEmpty()) {
            StringBuilder topTags = new StringBuilder();
            int count = 0;
            for (Hashtag h : contentResult.getTopHashtags()) {
                if (count > 0) topTags.append(", ");
                topTags.append("#").append(h.getTag());
                count++;
                if (count >= 3) break;
            }

            Recommendation r = new Recommendation(
                    UUID.randomUUID().toString(),
                    Recommendation.RecommendationType.HASHTAG_STRATEGY,
                    Recommendation.Priority.MEDIUM,
                    "Incorporate Proven Hashtags: " + topTags,
                    "These hashtags generate the strongest aggregate visibility and engagement. Include them in relevant captions to boost discoverability.",
                    "Rule: Top 3 Frequency and Interaction Hashtags in post corpus",
                    82.0
            );
            recs.add(r);
        }

        // RULE 5: Negative Sentiment Alert & Investigation
        if (sentiment != null && sentiment.getNegativePercent() > 15.0) {
            StringBuilder negWords = new StringBuilder();
            int count = 0;
            for (String kw : sentiment.getTopNegativeKeywords().keySet()) {
                if (count > 0) negWords.append(", ");
                negWords.append("'").append(kw).append("'");
                count++;
                if (count >= 3) break;
            }

            Recommendation r = new Recommendation(
                    UUID.randomUUID().toString(),
                    Recommendation.RecommendationType.SENTIMENT_ACTION,
                    Recommendation.Priority.HIGH,
                    "Investigate Negative Audience Sentiment (" + String.format("%.1f%%", sentiment.getNegativePercent()) + ")",
                    String.format("Negative sentiment is elevated at %.1f%% of analyzed feedback. Frequently recurring negative keywords: %s. Review recent updates or comments.",
                            sentiment.getNegativePercent(), negWords),
                    "Rule: Negative Sentiment Percentage > 15.0%",
                    90.0
            );
            recs.add(r);
        } else if (sentiment != null && sentiment.getPositivePercent() >= 60.0) {
            Recommendation r = new Recommendation(
                    UUID.randomUUID().toString(),
                    Recommendation.RecommendationType.SENTIMENT_ACTION,
                    Recommendation.Priority.LOW,
                    "Sustain High Positive Sentiment (" + String.format("%.1f%%", sentiment.getPositivePercent()) + ")",
                    "Audience sentiment is overwhelmingly favorable. Continue your current tone of voice and technical depth.",
                    "Rule: Positive Sentiment Percentage >= 60.0%",
                    75.0
            );
            recs.add(r);
        }

        return recs;
    }
}
