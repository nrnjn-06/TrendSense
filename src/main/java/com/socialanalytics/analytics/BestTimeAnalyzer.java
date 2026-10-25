package com.socialanalytics.analytics;

import com.socialanalytics.model.Post;
import com.socialanalytics.util.DateUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Analyzes historical posting timestamps against engagement to identify optimal posting time windows.
 */
public class BestTimeAnalyzer {

    public static class BestTimeResult {
        private final int peakHour;
        private final String windowDescription;
        private final double peakAverageEngagement;
        private final Map<Integer, Double> hourlyEngagement;
        private final boolean sufficientData;

        public BestTimeResult(int peakHour, String windowDescription, double peakAverageEngagement,
                              Map<Integer, Double> hourlyEngagement, boolean sufficientData) {
            this.peakHour = peakHour;
            this.windowDescription = windowDescription;
            this.peakAverageEngagement = peakAverageEngagement;
            this.hourlyEngagement = hourlyEngagement;
            this.sufficientData = sufficientData;
        }

        public int getPeakHour() {
            return peakHour;
        }

        public String getWindowDescription() {
            return windowDescription;
        }

        public double getPeakAverageEngagement() {
            return peakAverageEngagement;
        }

        public Map<Integer, Double> getHourlyEngagement() {
            return hourlyEngagement;
        }

        public boolean isSufficientData() {
            return sufficientData;
        }
    }

    public BestTimeResult analyze(List<Post> posts) {
        Map<Integer, Double> hourlyMap = new TreeMap<>();
        for (int h = 0; h < 24; h++) {
            hourlyMap.put(h, 0.0);
        }

        if (posts == null || posts.size() < 3) {
            return new BestTimeResult(-1, "Insufficient Data (Minimum 3 posts required)", 0.0, hourlyMap, false);
        }

        Map<Integer, List<Long>> hourBuckets = new HashMap<>();
        for (Post p : posts) {
            if (p.getTimestamp() != null) {
                int hour = p.getTimestamp().getHour();
                hourBuckets.computeIfAbsent(hour, k -> new ArrayList<>()).add(p.getTotalInteractions());
            }
        }

        if (hourBuckets.isEmpty()) {
            return new BestTimeResult(-1, "Insufficient Data (Missing Timestamps)", 0.0, hourlyMap, false);
        }

        int peakHour = -1;
        double maxAvg = -1.0;

        for (Map.Entry<Integer, List<Long>> entry : hourBuckets.entrySet()) {
            int hour = entry.getKey();
            List<Long> interactions = entry.getValue();
            long sum = 0;
            for (long val : interactions) sum += val;
            double avg = (double) sum / interactions.size();
            hourlyMap.put(hour, avg);

            if (avg > maxAvg) {
                maxAvg = avg;
                peakHour = hour;
            }
        }

        String window = (peakHour >= 0) ? DateUtil.formatHourWindow(peakHour) : "Insufficient Data";
        return new BestTimeResult(peakHour, window, maxAvg, hourlyMap, true);
    }
}
