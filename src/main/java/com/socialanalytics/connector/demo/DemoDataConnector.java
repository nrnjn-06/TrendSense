package com.socialanalytics.connector.demo;

import com.socialanalytics.connector.SocialMediaConnector;
import com.socialanalytics.exception.ApiException;
import com.socialanalytics.exception.AuthenticationException;
import com.socialanalytics.model.Comment;
import com.socialanalytics.model.Engagement;
import com.socialanalytics.model.Platform;
import com.socialanalytics.model.Post;
import com.socialanalytics.model.SocialAccount;
import java.util.List;

/**
 * Social media connector operating in DEMO MODE.
 * Supplies realistic, normalized mock domain objects for testing and demonstrations
 * when official API credentials are not yet configured.
 * Strictly adheres to transparency requirements by declaring isDemo() = true.
 */
public class DemoDataConnector implements SocialMediaConnector {
    private final Platform platform;
    private final Long simulatedAccountId;

    public DemoDataConnector(Platform platform) {
        this.platform = platform;
        this.simulatedAccountId = (long) (platform.ordinal() + 1);
    }

    @Override
    public Platform getPlatform() {
        return platform;
    }

    @Override
    public boolean authenticate(String tokenOrCredentials) throws AuthenticationException, ApiException {
        return true;
    }

    @Override
    public SocialAccount fetchAccountInfo(String accountIdOrHandle) throws ApiException {
        List<SocialAccount> accounts = DemoDataGenerator.generateDemoAccounts(1L);
        for (SocialAccount acc : accounts) {
            if (acc.getPlatform() == platform) {
                return acc;
            }
        }
        return accounts.get(0);
    }

    @Override
    public List<Post> fetchPosts(String accountId, int maxResults) throws ApiException {
        List<Post> posts = DemoDataGenerator.generateDemoPosts(simulatedAccountId, platform);
        if (posts.size() > maxResults) {
            return posts.subList(0, maxResults);
        }
        return posts;
    }

    @Override
    public List<Comment> fetchComments(String postId, int maxResults) throws ApiException {
        List<Comment> comments = DemoDataGenerator.generateDemoComments(postId, platform);
        if (comments.size() > maxResults) {
            return comments.subList(0, maxResults);
        }
        return comments;
    }

    @Override
    public Engagement fetchEngagement(String accountId) throws ApiException {
        List<Post> posts = fetchPosts(accountId, 50);
        long likes = 0, comments = 0, shares = 0;
        Long views = (platform == Platform.FACEBOOK) ? null : 0L;
        boolean hasViews = views != null;

        for (Post p : posts) {
            likes += p.getLikes();
            comments += p.getCommentsCount();
            shares += p.getShares();
            if (hasViews && p.getViews() != null) {
                views += p.getViews();
            }
        }

        Engagement e = new Engagement();
        e.setTotalPosts(posts.size());
        e.setTotalLikes(likes);
        e.setTotalComments(comments);
        e.setTotalShares(shares);
        e.setTotalViews(views);
        e.setViewsAvailable(hasViews);
        if (posts.size() > 0) {
            e.setAverageLikes((double) likes / posts.size());
            e.setAverageComments((double) comments / posts.size());
            e.setAverageShares((double) shares / posts.size());
            e.setEngagementRate(((double) (likes + comments + shares) / (posts.size() * 10000.0)) * 100.0);
        }
        return e;
    }

    @Override
    public void disconnect(String accountId) {
        // No-op for demo
    }

    @Override
    public boolean isDemo() {
        return true;
    }
}
