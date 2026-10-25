package com.socialanalytics.model;

/**
 * Domain model representing a trending search query retrieved from Google Trends.
 */
public class GoogleTrendItem {
    private int rank;
    private String topic;
    private String approxTraffic;
    private String pubDate;
    private String newsTitle;
    private String newsUrl;
    private String newsSource;
    private String pictureUrl;

    public GoogleTrendItem() {
    }

    public GoogleTrendItem(int rank, String topic, String approxTraffic, String pubDate,
                           String newsTitle, String newsUrl, String newsSource, String pictureUrl) {
        this.rank = rank;
        this.topic = topic;
        this.approxTraffic = approxTraffic;
        this.pubDate = pubDate;
        this.newsTitle = newsTitle;
        this.newsUrl = newsUrl;
        this.newsSource = newsSource;
        this.pictureUrl = pictureUrl;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getApproxTraffic() {
        return approxTraffic;
    }

    public void setApproxTraffic(String approxTraffic) {
        this.approxTraffic = approxTraffic;
    }

    public String getPubDate() {
        return pubDate;
    }

    public void setPubDate(String pubDate) {
        this.pubDate = pubDate;
    }

    public String getNewsTitle() {
        return newsTitle;
    }

    public void setNewsTitle(String newsTitle) {
        this.newsTitle = newsTitle;
    }

    public String getNewsUrl() {
        return newsUrl;
    }

    public void setNewsUrl(String newsUrl) {
        this.newsUrl = newsUrl;
    }

    public String getNewsSource() {
        return newsSource;
    }

    public void setNewsSource(String newsSource) {
        this.newsSource = newsSource;
    }

    public String getPictureUrl() {
        return pictureUrl;
    }

    public void setPictureUrl(String pictureUrl) {
        this.pictureUrl = pictureUrl;
    }

    @Override
    public String toString() {
        return "#" + rank + " " + topic + " (" + approxTraffic + " searches)";
    }
}
