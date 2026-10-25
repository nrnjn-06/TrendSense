package com.socialanalytics.analytics;

import com.socialanalytics.model.Trend;
import com.socialanalytics.service.ConfigService;

/**
 * Transparent, configurable Trend Scoring Service.
 * Evaluates topics using a documented, weighted composite formula:
 *
 * Trend Score = (w_mention * S_mention) + (w_engagement * S_engagement)
 *             + (w_growth * S_growth)   + (w_recency * S_recency)
 *
 * Where:
 *  - w_mention:    default 0.25
 *  - w_engagement: default 0.35
 *  - w_growth:     default 0.25
 *  - w_recency:    default 0.15
 *  - Each sub-score S is normalized strictly between 0 and 100.
 */
public class TrendScoringService {
    private final double weightMention;
    private final double weightEngagement;
    private final double weightGrowth;
    private final double weightRecency;

    public TrendScoringService() {
        ConfigService config = ConfigService.getInstance();
        this.weightMention = config.getDouble("trend.weight.mention", 0.25);
        this.weightEngagement = config.getDouble("trend.weight.engagement", 0.35);
        this.weightGrowth = config.getDouble("trend.weight.growth", 0.25);
        this.weightRecency = config.getDouble("trend.weight.recency", 0.15);
    }

    public TrendScoringService(double wMention, double wEngagement, double wGrowth, double wRecency) {
        this.weightMention = wMention;
        this.weightEngagement = wEngagement;
        this.weightGrowth = wGrowth;
        this.weightRecency = wRecency;
    }

    /**
     * Scores a trend candidate relative to maximum observed metrics in the dataset.
     *
     * @param trend target trend model to score and annotate
     * @param maxMentions highest mention count observed across all topics
     * @param maxAvgInteractions highest average interactions observed across all topics
     * @param daysSinceLatestPost days elapsed since the most recent mention (0 for today)
     */
    public void scoreTrend(Trend trend, long maxMentions, double maxAvgInteractions, double daysSinceLatestPost) {
        // 1. Normalized Mention Score (0 - 100)
        double mentionScore = 0.0;
        if (maxMentions > 0) {
            mentionScore = Math.min(100.0, (trend.getCurrentMentions() * 100.0) / maxMentions);
        }

        // 2. Normalized Engagement Score (0 - 100)
        double engagementScore = 0.0;
        if (maxAvgInteractions > 0) {
            engagementScore = Math.min(100.0, (trend.getAverageInteractions() * 100.0) / maxAvgInteractions);
        }

        // 3. Normalized Growth Score (0 - 100)
        double growth = trend.getGrowthPercentage();
        double growthScore;
        if (trend.getStatus() == Trend.TrendStatus.EMERGING || !trend.isHasGrowthBaseline()) {
            // Emerging topics receive a volume & engagement-proportional score rather than a flat arbitrary bonus
            double volumeBonus = Math.min(30.0, trend.getCurrentMentions() * 8.0);
            double engBonus = (maxAvgInteractions > 0) ? Math.min(15.0, (trend.getAverageInteractions() * 15.0) / maxAvgInteractions) : 0.0;
            growthScore = 45.0 + volumeBonus + engBonus;
            growthScore = Math.min(95.0, growthScore);
        } else {
            growthScore = 50.0 + (growth / 2.0);
            growthScore = Math.max(0.0, Math.min(100.0, growthScore));
        }

        // 4. Normalized Recency Score (0 - 100)
        // Linear decay over 14 days
        double recencyScore = Math.max(0.0, 100.0 - (daysSinceLatestPost * 7.14));

        // Composite weighted score: Score = (w_m * S_m) + (w_e * S_e) + (w_g * S_g) + (w_r * S_r)
        double totalScore = (weightMention * mentionScore)
                + (weightEngagement * engagementScore)
                + (weightGrowth * growthScore)
                + (weightRecency * recencyScore);

        // Confidence adjustment: single-mention topics receive a confidence dampener
        if (trend.getCurrentMentions() <= 1) {
            totalScore *= 0.65;
        }

        totalScore = Math.max(0.0, Math.min(100.0, totalScore));

        trend.setMentionScore(Math.round(mentionScore * 10.0) / 10.0);
        trend.setEngagementScore(Math.round(engagementScore * 10.0) / 10.0);
        trend.setGrowthScore(Math.round(growthScore * 10.0) / 10.0);
        trend.setRecencyScore(Math.round(recencyScore * 10.0) / 10.0);
        trend.setTrendScore(Math.round(totalScore * 10.0) / 10.0);

        String growthDesc = trend.isHasGrowthBaseline() ? String.format("%+.1f%%", growth) : "Emerging (New)";
        String formulaStr = String.format("(%.2f×%.0f) + (%.2f×%.0f) + (%.2f×%.0f) + (%.2f×%.0f)",
                weightMention, trend.getMentionScore(),
                weightEngagement, trend.getEngagementScore(),
                weightGrowth, trend.getGrowthScore(),
                weightRecency, trend.getRecencyScore());

        trend.setExplanation(String.format(
                "Score: %.1f | %s = %.1f/100 | Growth: %s | Mentions: %d | Avg Eng: %.0f | Status: %s",
                trend.getTrendScore(), formulaStr, trend.getTrendScore(), growthDesc,
                trend.getCurrentMentions(), trend.getAverageInteractions(), trend.getStatus().getLabel()
        ));
    }

    public double getWeightMention() {
        return weightMention;
    }

    public double getWeightEngagement() {
        return weightEngagement;
    }

    public double getWeightGrowth() {
        return weightGrowth;
    }

    public double getWeightRecency() {
        return weightRecency;
    }
}
