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
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Connector for official X API v2 (Twitter API v2).
 * Retrieves authorized user tweets, public metrics (likes, retweets, replies, quotes),
 * and impressions where permitted by access tier.
 */
public class XConnector extends AbstractSocialMediaConnector {
    private static final String X_API_BASE = "https://api.twitter.com/2";

    public XConnector() {
        super(Platform.X);
    }

    public XConnector(String bearerToken) {
        super(Platform.X);
        this.accessToken = bearerToken;
    }

    @Override
    public boolean authenticate(String bearerToken) throws AuthenticationException, ApiException {
        this.accessToken = bearerToken;
        if (accessToken == null || accessToken.isBlank()) {
            throw new AuthenticationException("X Bearer token is required.");
        }
        String url = X_API_BASE + "/users/me";
        JsonObject json = executeGet(url, accessToken);
        return json.has("data");
    }

    @Override
    public SocialAccount fetchAccountInfo(String usernameOrToken) throws ApiException {
        if (accessToken == null) throw new AuthenticationException("X account is not authenticated.");

        // Detect whether the input is an OAuth access token, blank, "me", or an actual Twitter username handle.
        // Twitter handles are strictly 1-15 alphanumeric/underscore characters (e.g. @TwitterDev).
        String cleaned = usernameOrToken != null ? usernameOrToken.trim() : "";
        boolean isUsername = !cleaned.isBlank()
                && !cleaned.equalsIgnoreCase("me")
                && !cleaned.equals(accessToken)
                && cleaned.length() <= 16
                && cleaned.matches("^@?[A-Za-z0-9_]{1,15}$");

        JsonObject json = null;
        if (isUsername) {
            String cleanUser = cleaned.startsWith("@") ? cleaned.substring(1) : cleaned;
            String url = X_API_BASE + "/users/by/username/" + cleanUser + "?user.fields=public_metrics,profile_image_url,description";
            try {
                json = executeGet(url, accessToken);
            } catch (ApiException e) {
                // If username lookup failed (e.g. restricted handle or tier), fallback to authenticated /users/me
                String meUrl = X_API_BASE + "/users/me?user.fields=public_metrics,profile_image_url,description";
                json = executeGet(meUrl, accessToken);
            }
        } else {
            // OAuth user context: query official /users/me
            String url = X_API_BASE + "/users/me?user.fields=public_metrics,profile_image_url,description";
            json = executeGet(url, accessToken);
        }

        if (json == null || !json.has("data") || !json.get("data").isJsonObject()) {
            throw new ApiException("X", "Could not retrieve user identity from X platform.");
        }

        JsonObject data = json.getAsJsonObject("data");
        SocialAccount acc = new SocialAccount();
        acc.setPlatform(Platform.X);
        acc.setPlatformAccountId(data.get("id").getAsString());
        acc.setUsername(data.get("username").getAsString());
        acc.setDisplayName(data.has("name") ? data.get("name").getAsString() : acc.getUsername());
        acc.setProfileUrl(data.has("profile_image_url") ? data.get("profile_image_url").getAsString() : "");

        if (data.has("public_metrics")) {
            JsonObject metrics = data.getAsJsonObject("public_metrics");
            acc.setFollowerCount(metrics.has("followers_count") ? metrics.get("followers_count").getAsLong() : 0);
            acc.setFollowingCount(metrics.has("following_count") ? metrics.get("following_count").getAsLong() : 0);
        }

        acc.setConnected(true);
        acc.setDemo(false);
        acc.setAccessToken(accessToken);
        acc.setLastSyncedAt(LocalDateTime.now());
        return acc;
    }

    @Override
    public List<Post> fetchPosts(String xUserId, int maxResults) throws ApiException {
        if (accessToken == null) throw new AuthenticationException("X account is not authenticated.");

        String resolvedId = xUserId;
        // If non-numeric ID or "me", resolve actual numeric platform account ID
        if (resolvedId == null || resolvedId.isBlank() || resolvedId.equalsIgnoreCase("me") || !resolvedId.matches("^\\d+$")) {
            try {
                SocialAccount me = fetchAccountInfo("me");
                resolvedId = me.getPlatformAccountId();
            } catch (Exception e) {
                // Return empty list if ID could not be resolved
                return new ArrayList<>();
            }
        }

        int limit = Math.min(Math.max(5, maxResults), 100);
        String url = X_API_BASE + "/users/" + resolvedId +
                "/tweets?tweet.fields=created_at,public_metrics,entities&max_results=" + limit;

        JsonObject json;
        try {
            json = executeGet(url, accessToken);
        } catch (ApiException e) {
            // Free tier access or user has 0 tweets / lacks permissions: return empty list gracefully
            return new ArrayList<>();
        }

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
            post.setPlatform(Platform.X);
            post.setContent(obj.has("text") ? obj.get("text").getAsString() : "");
            post.setContentType("TWEET");
            post.setPermalink("https://twitter.com/i/web/status/" + post.getId());

            if (obj.has("created_at")) {
                try {
                    post.setTimestamp(ZonedDateTime.parse(obj.get("created_at").getAsString(), DateTimeFormatter.ISO_DATE_TIME).toLocalDateTime());
                } catch (Exception e) {
                    post.setTimestamp(LocalDateTime.now());
                }
            } else {
                post.setTimestamp(LocalDateTime.now());
            }

            if (obj.has("public_metrics")) {
                JsonObject m = obj.getAsJsonObject("public_metrics");
                post.setLikes(m.has("like_count") ? m.get("like_count").getAsLong() : 0);
                post.setCommentsCount(m.has("reply_count") ? m.get("reply_count").getAsLong() : 0);
                long retweets = m.has("retweet_count") ? m.get("retweet_count").getAsLong() : 0;
                long quotes = m.has("quote_count") ? m.get("quote_count").getAsLong() : 0;
                post.setShares(retweets + quotes);

                if (m.has("impression_count")) {
                    post.setViews(m.get("impression_count").getAsLong());
                } else {
                    post.setViews(null);
                }
            }

            // Extract hashtags from entities or text
            if (obj.has("entities") && obj.getAsJsonObject("entities").has("hashtags")) {
                JsonArray tagArr = obj.getAsJsonObject("entities").getAsJsonArray("hashtags");
                for (JsonElement t : tagArr) {
                    if (t.isJsonObject() && t.getAsJsonObject().has("tag")) {
                        post.addHashtag(t.getAsJsonObject().get("tag").getAsString());
                    }
                }
            } else {
                for (String t : TextUtil.extractHashtags(post.getContent())) {
                    post.addHashtag(t);
                }
            }

            posts.add(post);
        }
        return posts;
    }

    @Override
    public List<Comment> fetchComments(String tweetId, int maxResults) throws ApiException {
        // Free/Standard Tier note: Official X API v2 requires Basic/Pro tier ($100+/mo) or Elevated Search
        // to retrieve full conversation replies via search endpoint (conversation_id:{tweetId}).
        // Return empty or permitted replies gracefully without failing.
        return new ArrayList<>();
    }

    @Override
    public Engagement fetchEngagement(String accountId) throws ApiException {
        List<Post> posts = fetchPosts(accountId, 50);
        long totalLikes = 0;
        long totalComments = 0;
        long totalShares = 0;
        long totalViews = 0;
        boolean hasViews = false;

        for (Post p : posts) {
            totalLikes += p.getLikes();
            totalComments += p.getCommentsCount();
            totalShares += p.getShares();
            if (p.getViews() != null) {
                totalViews += p.getViews();
                hasViews = true;
            }
        }

        Engagement e = new Engagement();
        e.setTotalPosts(posts.size());
        e.setTotalLikes(totalLikes);
        e.setTotalComments(totalComments);
        e.setTotalShares(totalShares);
        e.setTotalViews(hasViews ? totalViews : null);
        e.setViewsAvailable(hasViews);
        if (posts.size() > 0) {
            e.setAverageLikes((double) totalLikes / posts.size());
            e.setAverageComments((double) totalComments / posts.size());
            e.setAverageShares((double) totalShares / posts.size());
        }
        return e;
    }
}
