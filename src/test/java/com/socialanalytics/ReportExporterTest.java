package com.socialanalytics;

import com.socialanalytics.model.AnalyticsSummary;
import com.socialanalytics.model.Platform;
import com.socialanalytics.model.Post;
import com.socialanalytics.model.Trend;
import com.socialanalytics.model.TrendResult;
import com.socialanalytics.util.ReportExporter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ReportExporterTest {

    @TempDir
    Path tempDir;

    @Test
    public void testExportHtmlReport() throws IOException {
        AnalyticsSummary summary = new AnalyticsSummary();
        summary.setTotalPosts(10);
        summary.setTotalInteractions(5000);
        summary.setAverageEngagementRate(4.5);
        summary.setBestPostingTimeWindow("9 PM - 11 PM");
        summary.setLiveDataAvailable(true);

        Post topPost = new Post();
        topPost.setId("p_top");
        topPost.setAccountId(1L);
        topPost.setPlatform(Platform.INSTAGRAM);
        topPost.setContent("Exciting new trailer launch!");
        topPost.setTimestamp(LocalDateTime.now());
        topPost.setContentType("VIDEO");
        topPost.setLikes(4000);
        topPost.setCommentsCount(1000);
        topPost.setShares(0);
        topPost.setEngagementRate(5.0);
        summary.setTopPost(topPost);

        TrendResult trendResult = new TrendResult();
        Trend t1 = new Trend("Java", 10, 50, 2000);
        t1.setTrendScore(88.5);
        trendResult.getAllTrends().add(t1);
        summary.setTrends(trendResult);

        List<Post> posts = new ArrayList<>();
        posts.add(topPost);

        File htmlFile = ReportExporter.exportHtmlReport(summary, posts, tempDir.toString());
        assertTrue(htmlFile.exists());
        assertTrue(htmlFile.length() > 0);

        String content = Files.readString(htmlFile.toPath());
        assertTrue(content.contains("TrendSense Analytics Report"));
        assertTrue(content.contains("LIVE API DATA"));
        assertTrue(content.contains("Exciting new trailer launch!"));
        assertTrue(content.contains("Java"));
    }

    @Test
    public void testExportCsvReport() throws IOException {
        List<Post> posts = new ArrayList<>();
        Post p = new Post();
        p.setId("ig_1001");
        p.setAccountId(1L);
        p.setPlatform(Platform.INSTAGRAM);
        p.setContent("Behind the scenes photo #film");
        p.setContentType("IMAGE");
        p.setTimestamp(LocalDateTime.now());
        p.setLikes(120);
        p.setCommentsCount(15);
        p.setShares(5);
        p.setEngagementRate(2.8);
        p.setPermalink("https://instagram.com/p/1001");
        posts.add(p);

        File csvFile = ReportExporter.exportCsvReport(posts, tempDir.toString());
        assertTrue(csvFile.exists());
        assertTrue(csvFile.length() > 0);

        String content = Files.readString(csvFile.toPath());
        assertTrue(content.contains("Post_ID,Platform,Content"));
        assertTrue(content.contains("ig_1001"));
        assertTrue(content.contains("Behind the scenes photo #film"));
    }
}
