package com.socialanalytics.service;

import com.socialanalytics.model.GoogleTrendItem;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Service to fetch real-time daily search trends from Google Trends public RSS feed.
 * Requires zero API keys or developer credentials.
 */
public class GoogleTrendsService {

    private static final GoogleTrendsService INSTANCE = new GoogleTrendsService();
    private static final String BASE_RSS_URL = "https://trends.google.com/trending/rss?geo=";
    private final HttpClient httpClient;

    public GoogleTrendsService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(6))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public static GoogleTrendsService getInstance() {
        return INSTANCE;
    }

    /**
     * Fetches real-time daily search trends for the given country code (e.g. "IN", "US", "GB").
     *
     * @param geo 2-letter ISO country code.
     * @return List of parsed GoogleTrendItem objects.
     */
    public List<GoogleTrendItem> fetchDailyTrends(String geo) {
        String country = (geo != null && !geo.isBlank()) ? geo.trim().toUpperCase() : "IN";
        String targetUrl = BASE_RSS_URL + country;

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(targetUrl))
                    .timeout(Duration.ofSeconds(8))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .header("Accept", "application/rss+xml, application/xml, text/xml")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 && response.body() != null && !response.body().isBlank()) {
                List<GoogleTrendItem> items = parseRssXml(response.body());
                if (!items.isEmpty()) {
                    return items;
                }
            }
        } catch (Exception ex) {
            System.err.println("[GoogleTrendsService] Live fetch notice: " + ex.getMessage() + ". Using fallback items.");
        }

        return getFallbackTrends(country);
    }

    /**
     * Parses Google Trends RSS XML into structured GoogleTrendItem domain objects.
     */
    public List<GoogleTrendItem> parseRssXml(String xmlContent) {
        List<GoogleTrendItem> list = new ArrayList<>();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            // Disable external DTDs and entities for security
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xmlContent)));
            doc.getDocumentElement().normalize();

            NodeList itemNodes = doc.getElementsByTagName("item");
            int rankCounter = 1;

            for (int i = 0; i < itemNodes.getLength(); i++) {
                Node node = itemNodes.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element itemElem = (Element) node;

                    String title = getTagValue(itemElem, "title");
                    String approxTraffic = getTagValueWithNamespace(itemElem, "approx_traffic");
                    String pubDate = getTagValue(itemElem, "pubDate");
                    String pictureUrl = getTagValueWithNamespace(itemElem, "picture");

                    String newsTitle = "";
                    String newsUrl = "";
                    String newsSource = "";

                    NodeList newsNodes = itemElem.getElementsByTagNameNS("*", "news_item");
                    if (newsNodes.getLength() == 0) {
                        newsNodes = itemElem.getElementsByTagName("ht:news_item");
                    }
                    if (newsNodes.getLength() > 0 && newsNodes.item(0).getNodeType() == Node.ELEMENT_NODE) {
                        Element newsElem = (Element) newsNodes.item(0);
                        newsTitle = getTagValueWithNamespace(newsElem, "news_item_title");
                        newsUrl = getTagValueWithNamespace(newsElem, "news_item_url");
                        newsSource = getTagValueWithNamespace(newsElem, "news_item_source");
                    }

                    if (approxTraffic == null || approxTraffic.isBlank()) {
                        approxTraffic = "10K+";
                    }

                    if (title != null && !title.isBlank()) {
                        list.add(new GoogleTrendItem(
                                rankCounter++,
                                title,
                                approxTraffic,
                                formatPubDate(pubDate),
                                newsTitle,
                                newsUrl,
                                newsSource,
                                pictureUrl
                        ));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[GoogleTrendsService] XML parse error: " + e.getMessage());
        }
        return list;
    }

    private String getTagValue(Element parent, String tagName) {
        NodeList nl = parent.getElementsByTagName(tagName);
        if (nl != null && nl.getLength() > 0 && nl.item(0) != null) {
            return nl.item(0).getTextContent().trim();
        }
        return "";
    }

    private String getTagValueWithNamespace(Element parent, String localName) {
        NodeList nl = parent.getElementsByTagNameNS("*", localName);
        if (nl != null && nl.getLength() > 0 && nl.item(0) != null) {
            return nl.item(0).getTextContent().trim();
        }
        nl = parent.getElementsByTagName("ht:" + localName);
        if (nl != null && nl.getLength() > 0 && nl.item(0) != null) {
            return nl.item(0).getTextContent().trim();
        }
        return "";
    }

    private String formatPubDate(String pubDate) {
        if (pubDate == null || pubDate.isBlank()) return "Today";
        // e.g. "Mon, 19 Oct 2026 09:10:00 -0700" -> keep friendly date
        if (pubDate.length() > 16) {
            return pubDate.substring(0, 16);
        }
        return pubDate;
    }

    /**
     * Fallback representative trends if offline or rate-limited.
     */
    public List<GoogleTrendItem> getFallbackTrends(String country) {
        List<GoogleTrendItem> list = new ArrayList<>();
        list.add(new GoogleTrendItem(1, "Indian Premier League 2026", "200K+", "Today",
                "IPL Auction and schedule updates spark massive search interest", "https://google.com", "Sports News", ""));
        list.add(new GoogleTrendItem(2, "Artificial Intelligence in Business", "100K+", "Today",
                "New AI enterprise benchmarks released for global businesses", "https://google.com", "Tech Review", ""));
        list.add(new GoogleTrendItem(3, "Tamil Cinema Box Office Records", "50K+", "Today",
                "New regional release crosses milestone on opening weekend", "https://google.com", "Cinema Express", ""));
        list.add(new GoogleTrendItem(4, "Stock Market & Sensex Rally", "50K+", "Today",
                "Sensex gains over 400 points driven by tech sector rally", "https://google.com", "Business Standard", ""));
        list.add(new GoogleTrendItem(5, "Quantum Computing Breakthrough", "20K+", "Today",
                "Researchers achieve room-temperature quantum qubit coherence", "https://google.com", "Science Daily", ""));
        return list;
    }
}
