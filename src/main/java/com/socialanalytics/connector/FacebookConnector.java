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
 * Connector for Meta Facebook Pages Graph API (v19.0).
 * Connects to authorized Facebook Pages using Page Access Tokens.
 */
public class FacebookConnector extends AbstractSocialMediaConnector {
    private static final String GRAPH_API_BASE = "https://graph.facebook.com/v19.0";

    public FacebookConnector() {
        super(Platform.FACEBOOK);
    }

    public FacebookConnector(String pageAccessToken) {
        super(Platform.FACEBOOK);
        this.accessToken = pageAccessToken;
    }

    @Override
    public boolean authenticate(String tokenOrCredentials) throws AuthenticationException, ApiException {
        this.accessToken = tokenOrCredentials;
        if (accessToken == null || accessToken.isBlank()) {
            throw new AuthenticationException("Facebook Page access token is required.");
        }
        String url = GRAPH_API_BASE + "/me?access_token=" + accessToken;
        JsonObject response = executeGet(url, null);
        return response.has("id");
    }

    @Override
    public SocialAccount fetchAccountInfo(String pageId) throws ApiException {
        if (accessToken == null) throw new AuthenticationException("Facebook Page is not authenticated.");
        String targetId = (pageId == null || pageId.isBlank() || pageId.length() > 30) ? "me" : pageId;

        JsonObject json;
        try {
            String url = GRAPH_API_BASE + "/" + targetId +
                    "?fields=id,name,fan_count,link&access_token=" + accessToken;
            json = executeGet(url, null);
        } catch (ApiException e) {
            String basicUrl = GRAPH_API_BASE + "/" + targetId +
                    "?fields=id,name,link&access_token=" + accessToken;
            json = executeGet(basicUrl, null);
        }

        SocialAccount acc = new SocialAccount();
        acc.setPlatform(Platform.FACEBOOK);
        acc.setPlatformAccountId(json.has("id") ? json.get("id").getAsString() : targetId);
        acc.setUsername(json.has("name") ? json.get("name").getAsString().toLowerCase().replaceAll("\\s+", "") : "facebook_page");
        acc.setDisplayName(json.has("name") ? json.get("name").getAsString() : "Facebook Page");
        acc.setProfileUrl(json.has("link") ? json.get("link").getAsString() : "");
        acc.setFollowerCount(json.has("fan_count") ? json.get("fan_count").getAsLong() : 0);
        acc.setFollowingCount(0);
        acc.setConnected(true);
        acc.setDemo(false);
        acc.setAccessToken(accessToken);
        acc.setLastSyncedAt(LocalDateTime.now());
        return acc;
    }

    @Override
    public List<Post> fetchPosts(String pageId, int maxResults) throws ApiException {
        if (accessToken == null) throw new AuthenticationException("Facebook Page is not authenticated.");
        String targetId = (pageId == null || pageId.isBlank() || pageId.length() > 30) ? "me" : pageId;
        int limit = Math.min(Math.max(1, maxResults), 100);
        String url = GRAPH_API_BASE + "/" + targetId +
                "/posts?fields=id,message,created_time,shares,permalink_url,reactions.summary(total_count),comments.summary(total_count)" +
                "&limit=" + limit + "&access_token=" + accessToken;

        JsonObject json;
        try {
            json = executeGet(url, null);
        } catch (ApiException e) {
            // If user token does not have user_posts permission, return empty list gracefully
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
            post.setPlatform(Platform.FACEBOOK);
            post.setContent(obj.has("message") ? obj.get("message").getAsString() : "");
            post.setContentType("STATUS");
            post.setPermalink(obj.has("permalink_url") ? obj.get("permalink_url").getAsString() : "");

            // Reactions / Likes
            long likes = 0;
            if (obj.has("reactions") && obj.getAsJsonObject("reactions").has("summary")) {
                likes = obj.getAsJsonObject("reactions").getAsJsonObject("summary").get("total_count").getAsLong();
            }
            post.setLikes(likes);

            // Comments count
            long comments = 0;
            if (obj.has("comments") && obj.getAsJsonObject("comments").has("summary")) {
                comments = obj.getAsJsonObject("comments").getAsJsonObject("summary").get("total_count").getAsLong();
            }
            post.setCommentsCount(comments);

            // Shares
            long shares = 0;
            if (obj.has("shares") && obj.getAsJsonObject("shares").has("count")) {
                shares = obj.getAsJsonObject("shares").get("count").getAsLong();
            }
            post.setShares(shares);

            // Timestamp
            if (obj.has("created_time")) {
                try {
                    post.setTimestamp(ZonedDateTime.parse(obj.get("created_time").getAsString(), DateTimeFormatter.ISO_DATE_TIME).toLocalDateTime());
                } catch (Exception e) {
                    post.setTimestamp(LocalDateTime.now());
                }
            } else {
                post.setTimestamp(LocalDateTime.now());
            }

            for (String tag : TextUtil.extractHashtags(post.getContent())) {
                post.addHashtag(tag);
            }

            posts.add(post);
        }
        return posts;
    }

    @Override
    public List<Comment> fetchComments(String postId, int maxResults) throws ApiException {
        if (accessToken == null) throw new AuthenticationException("Facebook Page is not authenticated.");
        int limit = Math.min(Math.max(1, maxResults), 50);
        String url = GRAPH_API_BASE + "/" + postId +
                "/comments?fields=id,message,created_time,from&limit=" + limit + "&access_token=" + accessToken;

        JsonObject json = executeGet(url, null);
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
            c.setPostId(postId);
            c.setPlatform(Platform.FACEBOOK);
            c.setText(obj.has("message") ? obj.get("message").getAsString() : "");

            if (obj.has("from") && obj.getAsJsonObject("from").has("name")) {
                c.setAuthorName(obj.getAsJsonObject("from").get("name").getAsString());
            } else {
                c.setAuthorName("Facebook User");
            }

            if (obj.has("created_time")) {
                try {
                    c.setTimestamp(ZonedDateTime.parse(obj.get("created_time").getAsString(), DateTimeFormatter.ISO_DATE_TIME).toLocalDateTime());
                } catch (Exception e) {
                    c.setTimestamp(LocalDateTime.now());
                }
            }
            comments.add(c);
        }
        return comments;
    }

    @Override
    public Engagement fetchEngagement(String pageId) throws ApiException {
        List<Post> posts = fetchPosts(pageId, 50);
        long totalLikes = 0;
        long totalComments = 0;
        long totalShares = 0;

        for (Post p : posts) {
            totalLikes += p.getLikes();
            totalComments += p.getCommentsCount();
            totalShares += p.getShares();
        }

        Engagement e = new Engagement();
        e.setTotalPosts(posts.size());
        e.setTotalLikes(totalLikes);
        e.setTotalComments(totalComments);
        e.setTotalShares(totalShares);
        e.setViewsAvailable(false); // Post impressions require Page Read Insights review
        if (posts.size() > 0) {
            e.setAverageLikes((double) totalLikes / posts.size());
            e.setAverageComments((double) totalComments / posts.size());
            e.setAverageShares((double) totalShares / posts.size());
        }
        return e;
    }
}
