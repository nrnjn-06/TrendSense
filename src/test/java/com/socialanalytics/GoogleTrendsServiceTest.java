package com.socialanalytics;

import com.socialanalytics.model.GoogleTrendItem;
import com.socialanalytics.service.GoogleTrendsService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class GoogleTrendsServiceTest {

    @Test
    public void testParseRssXml() {
        GoogleTrendsService service = new GoogleTrendsService();
        String sampleXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <rss xmlns:atom="http://www.w3.org/2005/Atom" xmlns:ht="https://trends.google.com/trending/rss" version="2.0">
                    <channel>
                        <title>Daily Search Trends</title>
                        <item>
                            <title>Cricket World Cup</title>
                            <ht:approx_traffic>500000+</ht:approx_traffic>
                            <pubDate>Mon, 19 Oct 2026 10:00:00 -0700</pubDate>
                            <ht:news_item>
                                <ht:news_item_title>Thrilling finish in tournament match</ht:news_item_title>
                                <ht:news_item_url>https://news.example.com/cricket</ht:news_item_url>
                                <ht:news_item_source>Sports Network</ht:news_item_source>
                            </ht:news_item>
                        </item>
                        <item>
                            <title>Java 21 Release</title>
                            <ht:approx_traffic>20000+</ht:approx_traffic>
                            <pubDate>Mon, 19 Oct 2026 08:30:00 -0700</pubDate>
                            <ht:news_item>
                                <ht:news_item_title>Java features long-term support</ht:news_item_title>
                                <ht:news_item_url>https://tech.example.com/java</ht:news_item_url>
                                <ht:news_item_source>Tech Portal</ht:news_item_source>
                            </ht:news_item>
                        </item>
                    </channel>
                </rss>
                """;

        List<GoogleTrendItem> items = service.parseRssXml(sampleXml);
        assertNotNull(items);
        assertEquals(2, items.size());

        GoogleTrendItem first = items.get(0);
        assertEquals(1, first.getRank());
        assertEquals("Cricket World Cup", first.getTopic());
        assertEquals("500000+", first.getApproxTraffic());
        assertEquals("Thrilling finish in tournament match", first.getNewsTitle());
        assertEquals("Sports Network", first.getNewsSource());

        GoogleTrendItem second = items.get(1);
        assertEquals(2, second.getRank());
        assertEquals("Java 21 Release", second.getTopic());
        assertEquals("20000+", second.getApproxTraffic());
    }

    @Test
    public void testFallbackTrends() {
        GoogleTrendsService service = new GoogleTrendsService();
        List<GoogleTrendItem> fallback = service.getFallbackTrends("IN");
        assertNotNull(fallback);
        assertFalse(fallback.isEmpty());
        assertTrue(fallback.get(0).getTopic().length() > 0);
    }

    @Test
    public void testLiveFetchTrends() {
        GoogleTrendsService service = new GoogleTrendsService();
        List<GoogleTrendItem> live = service.fetchDailyTrends("IN");
        assertNotNull(live);
        assertFalse(live.isEmpty());
        System.out.println("[TEST] Fetched " + live.size() + " Google Trends: " + live.get(0));
    }
}
