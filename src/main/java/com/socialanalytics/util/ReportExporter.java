package com.socialanalytics.util;

import com.socialanalytics.model.AnalyticsSummary;
import com.socialanalytics.model.Post;
import com.socialanalytics.model.Trend;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Utility for exporting analytics snapshots and post performance datasets
 * into executive HTML reports and standard CSV formats.
 */
public class ReportExporter {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static File exportHtmlReport(AnalyticsSummary summary, List<Post> posts, String outputDir) throws IOException {
        File dir = new File(outputDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File htmlFile = new File(dir, "TrendSense_Executive_Report.html");

        try (PrintWriter out = new PrintWriter(new FileWriter(htmlFile, StandardCharsets.UTF_8))) {
            out.println("<!DOCTYPE html>");
            out.println("<html lang='en'>");
            out.println("<head>");
            out.println("<meta charset='UTF-8'>");
            out.println("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
            out.println("<title>TrendSense - Executive Analytics Report</title>");
            out.println("<style>");
            out.println("body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background: #0f172a; color: #f8fafc; margin: 0; padding: 30px; }");
            out.println(".container { max-width: 1100px; margin: 0 auto; }");
            out.println(".header { display: flex; justify-content: space-between; align-items: center; border-bottom: 2px solid #334155; padding-bottom: 20px; margin-bottom: 30px; }");
            out.println(".header h1 { margin: 0; font-size: 28px; color: #38bdf8; font-weight: 800; }");
            out.println(".header .badge { background: #0284c7; color: white; padding: 6px 14px; border-radius: 20px; font-size: 13px; font-weight: 600; }");
            out.println(".metrics-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 16px; margin-bottom: 35px; }");
            out.println(".card { background: #1e293b; border: 1px solid #334155; border-radius: 12px; padding: 20px; text-align: center; }");
            out.println(".card .val { font-size: 30px; font-weight: bold; color: #f1f5f9; margin-top: 8px; }");
            out.println(".card .lbl { font-size: 12px; text-transform: uppercase; letter-spacing: 1px; color: #94a3b8; font-weight: 600; }");
            out.println(".section-title { font-size: 20px; font-weight: 700; color: #38bdf8; margin: 30px 0 15px 0; border-left: 4px solid #38bdf8; padding-left: 10px; }");
            out.println("table { width: 100%; border-collapse: collapse; background: #1e293b; border-radius: 8px; overflow: hidden; margin-bottom: 30px; }");
            out.println("th, td { padding: 12px 16px; text-align: left; border-bottom: 1px solid #334155; font-size: 13px; }");
            out.println("th { background: #0f172a; color: #94a3b8; text-transform: uppercase; font-size: 11px; letter-spacing: 0.5px; }");
            out.println("tr:hover { background: #243247; }");
            out.println(".highlight-card { background: linear-gradient(135deg, #1e293b 0%, #0f172a 100%); border: 1px solid #0284c7; border-radius: 10px; padding: 20px; margin-bottom: 25px; }");
            out.println(".footer { text-align: center; font-size: 12px; color: #64748b; margin-top: 40px; border-top: 1px solid #334155; padding-top: 20px; }");
            out.println("@media print { body { background: white; color: black; } .card, table { border: 1px solid #ccc; background: white; color: black; } }");
            out.println("</style>");
            out.println("</head>");
            out.println("<body>");
            out.println("<div class='container'>");

            // Header
            out.println("<div class='header'>");
            out.println("<div>");
            out.println("<h1>TrendSense Analytics Report</h1>");
            out.println("<div style='color: #94a3b8; font-size: 13px; margin-top: 4px;'>Generated: " + (summary.getGeneratedAt() != null ? summary.getGeneratedAt().format(DATE_FMT) : "Now") + "</div>");
            out.println("</div>");
            out.println("<div class='badge'>" + (summary.isLiveDataAvailable() ? "LIVE API DATA" : "DEMO DATASET") + "</div>");
            out.println("</div>");

            // Key KPI Metric Cards
            out.println("<div class='metrics-grid'>");
            out.println("<div class='card'><div class='lbl'>Total Posts</div><div class='val'>" + summary.getTotalPosts() + "</div></div>");
            out.println("<div class='card'><div class='lbl'>Total Engagement</div><div class='val' style='color:#38bdf8;'>" + String.format("%,d", summary.getTotalInteractions()) + "</div></div>");
            String engRateDisplay = summary.isEngagementRateAvailable()
                    ? String.format("%.2f%%", summary.getAverageEngagementRate())
                    : "N/A (API Scope Restricted)";
            out.println("<div class='card'><div class='lbl'>Avg Engagement Rate</div><div class='val' style='color:#34d399;'>" + engRateDisplay + "</div></div>");
            out.println("<div class='card'><div class='lbl'>Dominant Sentiment</div><div class='val' style='color:#fbbf24;'>" + (summary.getSentiment() != null ? summary.getSentiment().getDominantSentiment() : "Neutral") + "</div></div>");
            out.println("<div class='card'><div class='lbl'>Best Posting Window</div><div class='val' style='font-size:20px; color:#c084fc; margin-top:15px;'>" + summary.getBestPostingTimeWindow() + "</div></div>");
            out.println("</div>");

            // Top Post Highlight
            if (summary.getTopPost() != null) {
                Post top = summary.getTopPost();
                out.println("<div class='highlight-card'>");
                out.println("<div style='font-size: 12px; font-weight: bold; color: #38bdf8; text-transform: uppercase;'>🏆 Top Performing Post</div>");
                out.println("<div style='font-size: 16px; margin: 10px 0; color: #f8fafc; font-style: italic;'>\"" + escapeHtml(top.getContent()) + "\"</div>");
                out.println("<div style='font-size: 13px; color: #94a3b8;'><strong>Platform:</strong> " + top.getPlatform().getDisplayName() + " | <strong>Likes:</strong> " + String.format("%,d", top.getLikes()) + " | <strong>Comments:</strong> " + String.format("%,d", top.getCommentsCount()) + " | <strong>Total Interactions:</strong> " + String.format("%,d", top.getTotalInteractions()) + "</div>");
                out.println("</div>");
            }

            // Top Posts Table
            out.println("<div class='section-title'>Post Performance Table</div>");
            out.println("<table>");
            out.println("<thead><tr><th>Platform</th><th>Content</th><th>Type</th><th>Likes</th><th>Comments</th><th>Total</th><th>Eng. Rate</th></tr></thead>");
            out.println("<tbody>");
            int limit = Math.min(posts.size(), 50);
            for (int i = 0; i < limit; i++) {
                Post p = posts.get(i);
                String preview = p.getContent() != null ? (p.getContent().length() > 65 ? p.getContent().substring(0, 65) + "..." : p.getContent()) : "";
                out.println("<tr>");
                out.println("<td><strong>" + p.getPlatform().getDisplayName() + "</strong></td>");
                out.println("<td>" + escapeHtml(preview) + "</td>");
                out.println("<td>" + p.getContentType() + "</td>");
                out.println("<td>" + String.format("%,d", p.getLikes()) + "</td>");
                out.println("<td>" + String.format("%,d", p.getCommentsCount()) + "</td>");
                out.println("<td><strong>" + String.format("%,d", p.getTotalInteractions()) + "</strong></td>");
                out.println("<td style='color:#34d399; font-weight:600;'>" + String.format("%.2f%%", p.getEngagementRate() != null ? p.getEngagementRate() : 0.0) + "</td>");
                out.println("</tr>");
            }
            out.println("</tbody></table>");

            // Trending Topics Section
            if (summary.getTrends() != null && summary.getTrends().getAllTrends() != null && !summary.getTrends().getAllTrends().isEmpty()) {
                out.println("<div class='section-title'>Trending Hashtags &amp; Topics</div>");
                out.println("<table>");
                out.println("<thead><tr><th>Topic</th><th>Trend Score</th><th>Growth</th><th>Mentions</th><th>Status</th></tr></thead>");
                out.println("<tbody>");
                for (Trend tm : summary.getTrends().getAllTrends()) {
                    out.println("<tr>");
                    out.println("<td><strong>" + escapeHtml(tm.getTopic()) + "</strong></td>");
                    out.println("<td>" + String.format("%.1f/100", tm.getTrendScore()) + "</td>");
                    out.println("<td style='color:" + (tm.getGrowthPercentage() >= 0 ? "#34d399" : "#f87171") + ";'>" + String.format("%+.1f%%", tm.getGrowthPercentage()) + "</td>");
                    out.println("<td>" + String.format("%,d", tm.getCurrentMentions()) + "</td>");
                    out.println("<td>" + (tm.getStatus() != null ? tm.getStatus().getSymbol() + " " + tm.getStatus().getLabel() : "—") + "</td>");
                    out.println("</tr>");
                }
                out.println("</tbody></table>");
            }

            // Footer
            out.println("<div class='footer'>");
            out.println("TrendSense Social Media Analytics &amp; Trend Engine &bull; Academic Project Final Review 2026");
            out.println("</div>");

            out.println("</div>");
            out.println("</body></html>");
        }

        return htmlFile;
    }

    public static File exportCsvReport(List<Post> posts, String outputDir) throws IOException {
        File dir = new File(outputDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File csvFile = new File(dir, "TrendSense_Posts_Dataset.csv");

        try (PrintWriter out = new PrintWriter(new FileWriter(csvFile, StandardCharsets.UTF_8))) {
            out.println("Post_ID,Platform,Content,Content_Type,Likes,Comments,Shares,Total_Interactions,Engagement_Rate,Timestamp,Permalink");
            for (Post p : posts) {
                out.printf("\"%s\",\"%s\",\"%s\",\"%s\",%d,%d,%s,%d,%s,\"%s\",\"%s\"%n",
                        p.getId(),
                        p.getPlatform().name(),
                        p.getContent() != null ? p.getContent().replace("\"", "\"\"").replace("\n", " ") : "",
                        p.getContentType() != null ? p.getContentType() : "POST",
                        p.getLikes(),
                        p.getCommentsCount(),
                        p.isSharesAvailable() ? String.valueOf(p.getShares()) : "N/A",
                        p.getTotalInteractions(),
                        p.getEngagementRate() != null ? String.format("%.2f", p.getEngagementRate()) : "N/A",
                        p.getTimestamp() != null ? p.getTimestamp().toString() : "",
                        p.getPermalink() != null ? p.getPermalink() : ""
                );
            }
        }
        return csvFile;
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
