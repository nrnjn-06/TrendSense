package com.socialanalytics.connector;

import com.socialanalytics.exception.ApiException;
import com.socialanalytics.exception.AuthenticationException;
import com.socialanalytics.model.Comment;
import com.socialanalytics.model.Engagement;
import com.socialanalytics.model.Platform;
import com.socialanalytics.model.Post;
import com.socialanalytics.model.SocialAccount;
import java.util.List;

/**
 * Common polymorphic interface for social media platform connectors.
 * Connectors are responsible for authenticating with official APIs,
 * fetching authorized profile data, posts, comments, and engagement,
 * and normalizing them into common domain models.
 */
public interface SocialMediaConnector {

    /**
     * Gets the platform associated with this connector.
     */
    Platform getPlatform();

    /**
     * Verifies the account authorization token and checks permissions.
     */
    boolean authenticate(String tokenOrCredentials) throws AuthenticationException, ApiException;

    /**
     * Fetches authorized account profile details.
     */
    SocialAccount fetchAccountInfo(String accountIdOrHandle) throws ApiException;

    /**
     * Fetches recent permitted posts from the authorized account.
     *
     * @param maxResults maximum number of posts permitted to retrieve
     */
    List<Post> fetchPosts(String accountId, int maxResults) throws ApiException;

    /**
     * Fetches comments for a specific post where permitted by the platform's API policy.
     */
    List<Comment> fetchComments(String postId, int maxResults) throws ApiException;

    /**
     * Fetches aggregate engagement metrics for the account/page.
     */
    Engagement fetchEngagement(String accountId) throws ApiException;

    /**
     * Disconnects or revokes the current authorization session.
     */
    void disconnect(String accountId);

    /**
     * Checks if this connector operates in live API mode or mock demo mode.
     */
    boolean isDemo();
}
