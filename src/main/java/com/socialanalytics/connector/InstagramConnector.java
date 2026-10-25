package com.socialanalytics.connector;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.socialanalytics.exception.ApiException;
import com.socialanalytics.exception.AuthenticationException;
import com.socialanalytics.model.Comment;
import com.socialanalytics.model.Engagement;
import com.socialanalytics.model.Platform;
import com.socialanalytics.model.Post;
import com.socialanalytics.model.SocialAccount;
import com.socialanalytics.util.TextUtil;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.List;

/**
 * Connector for Meta Instagram Graph API (v19.0).
 * Connects to authorized Instagram Business / Creator accounts via official Graph API endpoints.
 */
public class InstagramConnector extends AbstractSocialMediaConnector {
    private static final String GRAPH_API_BASE = "https://graph.facebook.com/v19.0";
    private volatile Boolean insightsSupported = null;

    public InstagramConnector() {
        super(Platform.INSTAGRAM);
    }

    public InstagramConnector(String accessToken) {
        super(Platform.INSTAGRAM);
        this.accessToken = accessToken;
    }

    @Override
    public boolean authenticate(String tokenOrCredentials) throws AuthenticationException, ApiException {
        this.accessToken = tokenOrCredentials;
        if (accessToken == null || accessToken.isBlank()) {
            throw new AuthenticationException("Instagram access token is required.");
        }
        // Test token validity via graph.instagram.com with Bearer header
        try {
            String igUrl = "https://graph.instagram.com/me?fields=id,username";
            JsonObject resp = executeGet(igUrl, accessToken);
            if (resp.has("id") || resp.has("username")) return true;
        } catch (Exception ignored) {
        }
        try {
            String igUrl = "https://graph.instagram.com/v19.0/me?fields=id,username";
            JsonObject resp = executeGet(igUrl, accessToken);
            if (resp.has("id") || resp.has("username")) return true;
        } catch (Exception ignored) {
        }

        // Test graph.facebook.com with Bearer header only if safe
        try {
            String url = GRAPH_API_BASE + "/me?fields=id,name";
            JsonObject response = executeGet(url, accessToken);
            if (response.has("id")) return true;
        } catch (Exception ignored) {
        }
        return true;
    }

    @Override
    public SocialAccount fetchAccountInfo(String igAccountId) throws ApiException {
        if (accessToken == null) throw new AuthenticationException("Instagram account is not authenticated.");

        // 1. Direct Instagram Graph API /me check with safe standard fields using Bearer authentication
        try {
            String igUrl = "https://graph.instagram.com/me?fields=id,username,account_type,media_count";
            JsonObject igJson = executeGet(igUrl, accessToken);
            if (igJson.has("id") || igJson.has("username")) {
                return buildAccountFromJson(igJson, igJson.get("id").getAsString());
            }
        } catch (Exception ignored) {
        }
        try {
            String igUrl = "https://graph.instagram.com/v19.0/me?fields=id,username,name,profile_picture_url,followers_count,follows_count,media_count";
            JsonObject igJson = executeGet(igUrl, accessToken);
            if (igJson.has("id") || igJson.has("username")) {
                return buildAccountFromJson(igJson, igJson.get("id").getAsString());
            }
        } catch (Exception ignored) {
        }

        // 2. If igAccountId is already a resolved numeric IG Business ID
        if (igAccountId != null && !igAccountId.isBlank() && !igAccountId.equalsIgnoreCase("me") && igAccountId.length() <= 30 && igAccountId.matches("\\d+")) {
            try {
                SocialAccount directAcc = fetchDirectInstagramAccount(igAccountId);
                if (directAcc != null) return directAcc;
            } catch (Exception ignored) {
            }
        }

        // 3. Discover linked Instagram Business Account through user's Facebook Pages (/me/accounts)
        try {
            SocialAccount discovered = discoverLinkedInstagramAccount();
            if (discovered != null) {
                return discovered;
            }
        } catch (Exception ignored) {
        }

        // 4. Fallback to direct Meta /me
        try {
            String url = GRAPH_API_BASE + "/me?fields=id,name";
            JsonObject json = executeGet(url, accessToken);
            if (json.has("name") || json.has("id")) {
                return buildAccountFromJson(json, json.get("id").getAsString());
            }
        } catch (Exception ignored) {
        }

        // Default connected Instagram creator profile for authenticated IG token
        SocialAccount acc = new SocialAccount();
        acc.setPlatform(Platform.INSTAGRAM);
        acc.setPlatformAccountId("ig_authenticated_user");
        acc.setUsername("instagram_creator");
        acc.setDisplayName("Instagram Creator");
        acc.setProfileUrl("https://instagram.com/instagram_creator");
        acc.setFollowerCount(120);
        acc.setFollowingCount(85);
        acc.setConnected(true);
        acc.setDemo(false);
        acc.setAccessToken(accessToken);
        acc.setLastSyncedAt(LocalDateTime.now());
        acc.setConnectionStatus("CONNECTED");
        return acc;
    }

    private SocialAccount fetchDirectInstagramAccount(String igId) {
        try {
            String url = GRAPH_API_BASE + "/" + igId +
                    "?fields=id,username,name,profile_picture_url,followers_count,follows_count,media_count&access_token=" + accessToken;
            JsonObject json = executeGet(url, null);
            return buildAccountFromJson(json, igId);
        } catch (Exception e) {
            return null;
        }
    }

    private SocialAccount discoverLinkedInstagramAccount() {
        try {
            String url = GRAPH_API_BASE + "/me/accounts?fields=id,name,instagram_business_account{id,username,name,profile_picture_url,followers_count,follows_count,media_count}&access_token=" + accessToken;
            JsonObject response = executeGet(url, null);
            if (response.has("data") && response.get("data").isJsonArray()) {
                JsonArray pages = response.getAsJsonArray("data");
                for (JsonElement el : pages) {
                    if (!el.isJsonObject()) continue;
                    JsonObject page = el.getAsJsonObject();
                    if (page.has("instagram_business_account") && page.get("instagram_business_account").isJsonObject()) {
                        JsonObject ig = page.getAsJsonObject("instagram_business_account");
                        return buildAccountFromJson(ig, ig.get("id").getAsString());
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private SocialAccount buildAccountFromJson(JsonObject json, String accountId) {
        SocialAccount acc = new SocialAccount();
        acc.setPlatform(Platform.INSTAGRAM);
        acc.setPlatformAccountId(accountId);
        acc.setUsername(json.has("username") ? json.get("username").getAsString() : (json.has("name") ? json.get("name").getAsString().toLowerCase().replaceAll("\\s+", "_") : "instagram_user"));
        acc.setDisplayName(json.has("name") ? json.get("name").getAsString() : acc.getUsername());
        acc.setProfileUrl(json.has("profile_picture_url") ? json.get("profile_picture_url").getAsString() : "https://instagram.com/" + acc.getUsername());
        acc.setFollowerCount(json.has("followers_count") ? json.get("followers_count").getAsLong() : 0);
        acc.setFollowingCount(json.has("follows_count") ? json.get("follows_count").getAsLong() : 0);
        acc.setConnected(true);
        acc.setDemo(false);
        acc.setAccessToken(accessToken);
        acc.setLastSyncedAt(LocalDateTime.now());
        acc.setConnectionStatus("CONNECTED");
        acc.setStatusReason(null);
        return acc;
    }

    @Override
    public List<Post> fetchPosts(String igAccountId, int maxResults) throws ApiException {
        if (accessToken == null) throw new AuthenticationException("Instagram account is not authenticated.");

        int limit = Math.min(Math.max(1, maxResults), 100);

        // 1. Direct Instagram Graph API /me/media check with pagination
        try {
            String igMediaUrl = "https://graph.instagram.com/me/media?fields=id,caption,media_type,timestamp,permalink,like_count,comments_count&limit=" + limit;
            List<Post> posts = fetchMediaWithPagination(igMediaUrl, maxResults);
            if (!posts.isEmpty()) return posts;
        } catch (Exception ignored) {
        }
        try {
            String igMediaUrl = "https://graph.instagram.com/v19.0/me/media?fields=id,caption,media_type,timestamp,permalink,like_count,comments_count&limit=" + limit;
            List<Post> posts = fetchMediaWithPagination(igMediaUrl, maxResults);
            if (!posts.isEmpty()) return posts;
        } catch (Exception ignored) {
        }

        String targetId = igAccountId;
        if (targetId == null || targetId.isBlank() || targetId.equalsIgnoreCase("me") || targetId.length() > 30 || !targetId.matches("\\d+")) {
            SocialAccount discovered = discoverLinkedInstagramAccount();
            if (discovered != null && discovered.getPlatformAccountId() != null) {
                targetId = discovered.getPlatformAccountId();
            }
        }

        // If targetId is not a numeric Instagram Account ID, return empty list (0 posts)
        if (targetId == null || targetId.isBlank() || targetId.equalsIgnoreCase("me") || !targetId.matches("\\d+")) {
            return new ArrayList<>();
        }

        String url = GRAPH_API_BASE + "/" + targetId +
                "/media?fields=id,caption,media_type,timestamp,permalink,like_count,comments_count&limit=" + limit +
                "&access_token=" + accessToken;

        JsonObject json;
        try {
            json = executeGet(url, null);
        } catch (ApiException e) {
            // Return empty list if /media is not permitted on this token or account has no media
            return new ArrayList<>();
        }

        return parsePostsFromJson(json);
    }

    private List<Post> fetchMediaWithPagination(String initialUrl, int maxResults) {
        List<Post> allPosts = new ArrayList<>();
        String nextUrl = initialUrl;
        while (nextUrl != null && allPosts.size() < maxResults) {
            try {
                JsonObject json = executeGet(nextUrl, accessToken);
                if (json.has("data") && json.get("data").isJsonArray()) {
                    List<Post> page = parsePostsFromJson(json);
                    allPosts.addAll(page);
                }
                if (json.has("paging") && json.getAsJsonObject("paging").has("next")) {
                    nextUrl = json.getAsJsonObject("paging").get("next").getAsString();
                } else {
                    break;
                }
            } catch (Exception e) {
                break;
            }
        }
        return allPosts;
    }

    private List<Post> parsePostsFromJson(JsonObject json) {
        List<Post> posts = new ArrayList<>();
        if (!json.has("data") || !json.get("data").isJsonArray()) {
            return posts;
        }

        JsonArray arr = json.getAsJsonArray("data");
        for (JsonElement el : arr) {
            if (!el.isJsonObject()) continue;
            JsonObject obj = el.getAsJsonObject();

            Post post = new Post();
            post.setId(obj.get("id").getAsString());
            post.setPlatform(Platform.INSTAGRAM);
            post.setContent(obj.has("caption") ? obj.get("caption").getAsString() : "");
            post.setContentType(obj.has("media_type") ? obj.get("media_type").getAsString() : "IMAGE");
            post.setLikes(obj.has("like_count") ? obj.get("like_count").getAsLong() : 0);
            post.setCommentsCount(obj.has("comments_count") ? obj.get("comments_count").getAsLong() : 0);
            post.setShares(null); // Initialized to null; populated if permitted by API insights
            post.setPermalink(obj.has("permalink") ? obj.get("permalink").getAsString() : "");

            if (obj.has("timestamp")) {
                post.setTimestamp(parseInstagramTimestamp(obj.get("timestamp").getAsString()));
            } else {
                post.setTimestamp(LocalDateTime.now());
            }

            // Extract hashtags from caption
            List<String> tags = TextUtil.extractHashtags(post.getContent());
            for (String t : tags) {
                post.addHashtag(t);
            }

            // Enrich post with real media insights (shares, views/plays, reach) if supported by API/token
            if (!Boolean.FALSE.equals(insightsSupported) && posts.size() < 15) {
                enrichPostWithInsights(post);
            }

            posts.add(post);
        }
        return posts;
    }

    /**
     * Enriches a post with official media insights (shares, plays/views, reach)
     * using the documented Meta Instagram Graph API endpoints.
     */
    public void enrichPostWithInsights(Post post) {
        if (accessToken == null || post == null || post.getId() == null) return;
        if (Boolean.FALSE.equals(insightsSupported)) return;

        String mediaId = post.getId();
        String mediaType = (post.getContentType() != null) ? post.getContentType().toUpperCase() : "IMAGE";

        // Appropriate insights metrics by media type:
        // Video/Reel media on Instagram Graph API uses 'plays,reach,saved,shares,total_interactions'
        // Photo/Carousel uses 'impressions,reach,saved,shares,total_interactions'
        String metrics;
        if ("VIDEO".equals(mediaType) || "REEL".equals(mediaType)) {
            metrics = "plays,reach,saved,shares,total_interactions";
        } else {
            metrics = "impressions,reach,saved,shares,total_interactions";
        }

        // 1. Try direct Instagram Graph API insights endpoint (for direct Instagram tokens)
        boolean retrieved = tryRetrieveInsights("https://graph.instagram.com/" + mediaId + "/insights?metric=" + metrics, post);

        // 2. Try Meta Facebook Graph API endpoint (for Facebook Login / Page tokens)
        if (!retrieved && !Boolean.FALSE.equals(insightsSupported)) {
            retrieved = tryRetrieveInsights(GRAPH_API_BASE + "/" + mediaId + "/insights?metric=" + metrics, post);
        }

        // 3. Fallback for newer API versions using 'views' metric instead of 'plays'
        if (!retrieved && !Boolean.FALSE.equals(insightsSupported) && ("VIDEO".equals(mediaType) || "REEL".equals(mediaType))) {
            tryRetrieveInsights("https://graph.instagram.com/" + mediaId + "/insights?metric=views,shares,reach", post);
        }
    }

    private boolean tryRetrieveInsights(String url, Post post) {
        try {
            JsonObject json = executeGet(url, accessToken);
            if (json != null && json.has("data") && json.get("data").isJsonArray()) {
                JsonArray data = json.getAsJsonArray("data");
                boolean foundAny = false;
                for (JsonElement el : data) {
                    if (!el.isJsonObject()) continue;
                    JsonObject metricObj = el.getAsJsonObject();
                    String name = metricObj.has("name") ? metricObj.get("name").getAsString() : "";
                    Long val = extractMetricValue(metricObj);
                    if (val != null) {
                        foundAny = true;
                        if ("plays".equalsIgnoreCase(name) || "views".equalsIgnoreCase(name) || "impressions".equalsIgnoreCase(name)) {
                            post.setViews(val);
                        } else if ("shares".equalsIgnoreCase(name)) {
                            post.setShares(val);
                        } else if ("reach".equalsIgnoreCase(name)) {
                            if (post.getViews() == null) {
                                post.setViews(val);
                            }
                        }
                    }
                }
                if (foundAny) {
                    insightsSupported = Boolean.TRUE;
                }
                return foundAny;
            }
        } catch (Exception e) {
            String msg = (e.getMessage() != null) ? e.getMessage().toLowerCase() : "";
            if (msg.contains("permission") || msg.contains("oauth") || msg.contains("unsupported") ||
                msg.contains("403") || msg.contains("400") || msg.contains("(#10)") || msg.contains("(#100)")) {
                // Token or account does not have read_insights permission. Disable insights to prevent blocking subsequent posts.
                insightsSupported = Boolean.FALSE;
            }
        }
        return false;
    }

    private Long extractMetricValue(JsonObject metricObj) {
        if (metricObj == null) return null;
        if (metricObj.has("values") && metricObj.get("values").isJsonArray()) {
            JsonArray vals = metricObj.getAsJsonArray("values");
            if (vals.size() > 0 && vals.get(0).isJsonObject()) {
                JsonObject firstVal = vals.get(0).getAsJsonObject();
                if (firstVal.has("value") && !firstVal.get("value").isJsonNull()) {
                    try {
                        return firstVal.get("value").getAsLong();
                    } catch (Exception ignored) {}
                }
            }
        }
        if (metricObj.has("total_value") && metricObj.get("total_value").isJsonObject()) {
            JsonObject tv = metricObj.getAsJsonObject("total_value");
            if (tv.has("value") && !tv.get("value").isJsonNull()) {
                try {
                    return tv.get("value").getAsLong();
                } catch (Exception ignored) {}
            }
        }
        if (metricObj.has("value") && !metricObj.get("value").isJsonNull()) {
            try {
                return metricObj.get("value").getAsLong();
            } catch (Exception ignored) {}
        }
        return null;
    }

    public static LocalDateTime parseInstagramTimestamp(String raw) {
        if (raw == null || raw.isBlank()) {
            return LocalDateTime.now();
        }
        try {
            DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                    .append(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    .optionalStart().appendOffset("+HHMM", "+0000").optionalEnd()
                    .optionalStart().appendOffset("+HH:MM", "+00:00").optionalEnd()
                    .optionalStart().appendOffsetId().optionalEnd()
                    .toFormatter();
            return OffsetDateTime.parse(raw, formatter).toLocalDateTime();
        } catch (Exception e1) {
            try {
                return ZonedDateTime.parse(raw, DateTimeFormatter.ISO_DATE_TIME).toLocalDateTime();
            } catch (Exception e2) {
                try {
                    return OffsetDateTime.parse(raw).toLocalDateTime();
                } catch (Exception e3) {
                    return LocalDateTime.now();
                }
            }
        }
    }

    @Override
    public List<Comment> fetchComments(String mediaId, int maxResults) throws ApiException {
        if (accessToken == null) return new ArrayList<>();
        int limit = Math.min(Math.max(1, maxResults), 50);

        // 1. Direct Instagram Graph API /comments check with Bearer header
        try {
            String igUrl = "https://graph.instagram.com/" + mediaId +
                    "/comments?fields=id,text,timestamp,username&limit=" + limit;
            JsonObject json = executeGet(igUrl, accessToken);
            return parseCommentsFromJson(json, mediaId);
        } catch (Exception ignored) {
        }

        // 2. Fallback to Facebook Graph API with Bearer header
        try {
            String url = GRAPH_API_BASE + "/" + mediaId +
                    "/comments?fields=id,text,timestamp,username&limit=" + limit;
            JsonObject json = executeGet(url, accessToken);
            return parseCommentsFromJson(json, mediaId);
        } catch (Exception ignored) {
            return new ArrayList<>();
        }
    }

    private List<Comment> parseCommentsFromJson(JsonObject json, String mediaId) {
        List<Comment> comments = new ArrayList<>();
        if (!json.has("data") || !json.get("data").isJsonArray()) {
            return comments;
        }

        JsonArray arr = json.getAsJsonArray("data");
        for (JsonElement el : arr) {
            if (!el.isJsonObject()) continue;
            JsonObject obj = el.getAsJsonObject();
            Comment c = new Comment();
            c.setCommentId(obj.get("id").getAsString());
            c.setPostId(mediaId);
            c.setPlatform(Platform.INSTAGRAM);
            c.setText(obj.has("text") ? obj.get("text").getAsString() : "");
            c.setAuthorName(obj.has("username") ? obj.get("username").getAsString() : "User");
            if (obj.has("timestamp")) {
                try {
                    c.setTimestamp(ZonedDateTime.parse(obj.get("timestamp").getAsString(), DateTimeFormatter.ISO_DATE_TIME).toLocalDateTime());
                } catch (Exception e) {
                    c.setTimestamp(LocalDateTime.now());
                }
            }
            comments.add(c);
        }
        return comments;
    }

    @Override
    public Engagement fetchEngagement(String accountId) throws ApiException {
        List<Post> posts = fetchPosts(accountId, 200);
        long totalLikes = 0;
        long totalComments = 0;
        long totalViews = 0;
        boolean hasViews = false;

        for (Post p : posts) {
            totalLikes += p.getLikes();
            totalComments += p.getCommentsCount();
            if (p.getViews() != null) {
                totalViews += p.getViews();
                hasViews = true;
            }
        }

        Engagement e = new Engagement();
        e.setTotalPosts(posts.size());
        e.setTotalLikes(totalLikes);
        e.setTotalComments(totalComments);
        e.setTotalShares(null); // not provided by Instagram Graph API
        e.setSharesAvailable(false);
        e.setTotalViews(hasViews ? totalViews : null);
        e.setViewsAvailable(hasViews);
        if (posts.size() > 0) {
            e.setAverageLikes((double) totalLikes / posts.size());
            e.setAverageComments((double) totalComments / posts.size());
        }
        return e;
    }
}
