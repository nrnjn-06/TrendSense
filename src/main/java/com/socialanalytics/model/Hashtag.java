package com.socialanalytics.model;

import java.util.Objects;

/**
 * Model representing hashtag occurrences and aggregated performance.
 */
public class Hashtag implements Comparable<Hashtag> {
    private String tag;
    private long count;
    private long totalInteractions;
    private double averageInteractions;

    public Hashtag() {
    }

    public Hashtag(String tag, long count, long totalInteractions) {
        this.tag = tag != null ? tag.toLowerCase().replaceAll("^#", "") : "";
        this.count = count;
        this.totalInteractions = totalInteractions;
        this.averageInteractions = count > 0 ? (double) totalInteractions / count : 0.0;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
        this.averageInteractions = count > 0 ? (double) totalInteractions / count : 0.0;
    }

    public long getTotalInteractions() {
        return totalInteractions;
    }

    public void setTotalInteractions(long totalInteractions) {
        this.totalInteractions = totalInteractions;
        this.averageInteractions = count > 0 ? (double) totalInteractions / count : 0.0;
    }

    public double getAverageInteractions() {
        return averageInteractions;
    }

    @Override
    public int compareTo(Hashtag o) {
        // Compare primarily by count descending, then total interactions descending
        int countCmp = Long.compare(o.count, this.count);
        if (countCmp != 0) return countCmp;
        return Long.compare(o.totalInteractions, this.totalInteractions);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Hashtag hashtag = (Hashtag) o;
        return Objects.equals(tag, hashtag.tag);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tag);
    }

    @Override
    public String toString() {
        return "#" + tag + " (" + count + " posts, avg " + String.format("%.1f", averageInteractions) + ")";
    }
}
