package com.socialanalytics.service;

import com.socialanalytics.connector.ConnectorRegistry;
import com.socialanalytics.connector.SocialMediaConnector;
import com.socialanalytics.connector.demo.DemoDataGenerator;
import com.socialanalytics.dao.CommentDAO;
import com.socialanalytics.dao.PostDAO;
import com.socialanalytics.dao.SocialAccountDAO;
import com.socialanalytics.dao.impl.JdbcCommentDAO;
import com.socialanalytics.dao.impl.JdbcPostDAO;
import com.socialanalytics.dao.impl.JdbcSocialAccountDAO;
import com.socialanalytics.exception.ApiException;
import com.socialanalytics.exception.AuthenticationException;
import com.socialanalytics.model.Comment;
import com.socialanalytics.model.Platform;
import com.socialanalytics.model.Post;
import com.socialanalytics.model.SocialAccount;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service managing authorized social accounts and orchestrating data synchronization
 * from either official platform APIs or transparent Demo Mode.
 */
public class SocialAccountService {
    private static final SocialAccountService INSTANCE = new SocialAccountService();
    private final SocialAccountDAO socialAccountDAO;
    private final PostDAO postDAO;
    private final CommentDAO commentDAO;
    private final ConnectorRegistry connectorRegistry;

    public SocialAccountService() {
        this.socialAccountDAO = new JdbcSocialAccountDAO();
        this.postDAO = new JdbcPostDAO();
        this.commentDAO = new JdbcCommentDAO();
        this.connectorRegistry = ConnectorRegistry.getInstance();
    }

    public SocialAccountService(SocialAccountDAO socialAccountDAO, PostDAO postDAO,
                                CommentDAO commentDAO, ConnectorRegistry connectorRegistry) {
        this.socialAccountDAO = socialAccountDAO;
        this.postDAO = postDAO;
        this.commentDAO = commentDAO;
        this.connectorRegistry = connectorRegistry;
    }

    public static SocialAccountService getInstance() {
        return INSTANCE;
    }

    /**
     * Connects an account in Demo Mode for a user.
     */
    public SocialAccount connectDemoAccount(Long userId, Platform platform) {
        // Check if already exists
        Optional<SocialAccount> existing = socialAccountDAO.findByUserAndPlatform(userId, platform);
        SocialAccount account;
        if (existing.isPresent()) {
            account = existing.get();
            account.setConnected(true);
            account.setConnectionStatus("CONNECTED");
            account.setStatusReason(null);
            account.setDemo(true);
            account.setLastSyncedAt(LocalDateTime.now());
            socialAccountDAO.update(account);
        } else {
            List<SocialAccount> demoAccounts = DemoDataGenerator.generateDemoAccounts(userId);
            account = demoAccounts.stream()
                    .filter(a -> a.getPlatform() == platform)
                    .findFirst()
                    .orElse(demoAccounts.get(0));
            account.setUserId(userId);
            account.setConnected(true);
            account.setConnectionStatus("CONNECTED");
            account.setStatusReason(null);
            account = socialAccountDAO.save(account);
        }

        // Sync demo posts & comments
        syncAccountData(account, true);
        return account;
    }

    /**
     * Connects all 4 platforms in Demo Mode at once for rapid demonstration.
     */
    public List<SocialAccount> connectAllDemoAccounts(Long userId) {
        List<SocialAccount> connected = new ArrayList<>();
        for (Platform p : Platform.values()) {
            connected.add(connectDemoAccount(userId, p));
        }
        return connected;
    }

    /**
     * Connects an account using an authorization code received from official OAuth flow.
     */
    public SocialAccount connectOAuthAccount(Long userId, Platform platform, String authorizationCode) throws AuthenticationException, ApiException {
        OAuthService oAuthService = OAuthService.getInstance();
        String accessToken = oAuthService.exchangeCodeForToken(platform, authorizationCode);
        return connectLiveAccount(userId, platform, accessToken);
    }

    /**
     * Connects an account via Developer Testing token fallback.
     */
    public SocialAccount connectDeveloperTokenAccount(Long userId, Platform platform, String token) throws AuthenticationException, ApiException {
        return connectLiveAccount(userId, platform, token);
    }

    /**
     * Connects an account via live official API.
     */
    public SocialAccount connectLiveAccount(Long userId, Platform platform, String tokenOrHandle) throws AuthenticationException, ApiException {
        try {
            SocialMediaConnector connector = connectorRegistry.getConnector(platform, tokenOrHandle, false);

            connector.authenticate(tokenOrHandle);
            SocialAccount fetched = connector.fetchAccountInfo(tokenOrHandle);
            fetched.setUserId(userId);
            fetched.setConnected(true);
            fetched.setConnectionStatus("CONNECTED");
            fetched.setStatusReason(null);
            fetched.setDemo(false);
            fetched.setAccessToken(tokenOrHandle);
            fetched.setLastSyncedAt(LocalDateTime.now());

            Optional<SocialAccount> existing = socialAccountDAO.findByUserAndPlatform(userId, platform);
            SocialAccount saved;
            if (existing.isPresent()) {
                fetched.setId(existing.get().getId());
                socialAccountDAO.update(fetched);
                saved = fetched;
                // Delete previous demo posts so they never mix into the live account
                postDAO.deleteByAccountId(saved.getId());
            } else {
                saved = socialAccountDAO.save(fetched);
            }

            // Fetch live posts
            syncAccountData(saved, false);
            return saved;
        } catch (Exception e) {
            recordConnectionFailure(userId, platform, e.getMessage());
            throw e;
        }
    }

    /**
     * Records an official connection failure with reason for UI display.
     */
    public SocialAccount recordConnectionFailure(Long userId, Platform platform, String reason) {
        Optional<SocialAccount> existing = socialAccountDAO.findByUserAndPlatform(userId, platform);
        SocialAccount account;
        if (existing.isPresent()) {
            account = existing.get();
            account.setConnected(false);
            account.setConnectionStatus("FAILED");
            account.setStatusReason(reason);
            socialAccountDAO.update(account);
        } else {
            account = new SocialAccount();
            account.setUserId(userId);
            account.setPlatform(platform);
            account.setPlatformAccountId("unconnected_" + platform.name().toLowerCase());
            account.setUsername("unconnected_" + platform.name().toLowerCase());
            account.setDisplayName(platform.getDisplayName());
            account.setConnected(false);
            account.setDemo(false);
            account.setConnectionStatus("FAILED");
            account.setStatusReason(reason);
            account = socialAccountDAO.save(account);
        }
        return account;
    }

    /**
     * Synchronizes posts and comments from either Live API or Demo generator.
     * Returns the number of posts successfully synchronized.
     */
    public int syncAccountData(SocialAccount account, boolean forceDemo) {
        long startTime = System.currentTimeMillis();
        SocialMediaConnector connector = connectorRegistry.getConnector(
                account.getPlatform(),
                account.getAccessToken(),
                forceDemo || account.isDemo()
        );

        List<Post> posts = connector.fetchPosts(account.getPlatformAccountId(), 100);
        long postsFetchTime = System.currentTimeMillis() - startTime;

        // Clean previous posts for this account before inserting freshly synced posts
        postDAO.deleteByAccountId(account.getId());

        for (Post p : posts) {
            p.setAccountId(account.getId());
            // Calculate engagement rate
            if (p.getViews() != null && p.getViews() > 0) {
                p.setEngagementRate(((double) p.getTotalInteractions() / p.getViews()) * 100.0);
            } else if (account.getFollowerCount() > 0) {
                p.setEngagementRate(((double) p.getTotalInteractions() / account.getFollowerCount()) * 100.0);
            }
        }
        if (!posts.isEmpty()) {
            postDAO.saveAll(posts);
        }

        // Fetch comments where supported for top recent posts to prevent rate limits and ensure fast sync
        if (account.getPlatform().isSupportsComments() && !posts.isEmpty()) {
            List<Comment> allComments = new ArrayList<>();
            int maxCommentPosts = Math.min(posts.size(), 5);
            for (int i = 0; i < maxCommentPosts; i++) {
                Post p = posts.get(i);
                try {
                    List<Comment> comments = connector.fetchComments(p.getId(), 10);
                    if (comments != null && !comments.isEmpty()) {
                        allComments.addAll(comments);
                    }
                } catch (Exception ignored) {
                }
            }
            if (!allComments.isEmpty()) {
                commentDAO.saveAll(allComments);
            }
        }

        account.setLastSyncedAt(LocalDateTime.now());
        socialAccountDAO.update(account);

        long totalTime = System.currentTimeMillis() - startTime;
        System.out.println(String.format(
                "[SocialAccountService] Synchronized %d posts for %s (@%s) [Posts: %d ms | Total: %d ms]",
                posts.size(), account.getPlatform().getDisplayName(), account.getUsername(), postsFetchTime, totalTime
        ));

        return posts.size();
    }

    public List<SocialAccount> getAccountsForUser(Long userId) {
        return socialAccountDAO.findByUserId(userId);
    }

    public Optional<SocialAccount> getAccountForUserAndPlatform(Long userId, Platform platform) {
        return socialAccountDAO.findByUserAndPlatform(userId, platform);
    }

    public void disconnectAccount(Long accountId) {
        Optional<SocialAccount> acc = socialAccountDAO.findById(accountId);
        if (acc.isPresent()) {
            SocialAccount a = acc.get();
            a.setConnected(false);
            a.setConnectionStatus("DISCONNECTED");
            a.setAccessToken(null);
            socialAccountDAO.update(a);
        }
    }

    public void disconnectPlatformForUser(Long userId, Platform platform) {
        Optional<SocialAccount> acc = socialAccountDAO.findByUserAndPlatform(userId, platform);
        if (acc.isPresent()) {
            SocialAccount a = acc.get();
            a.setConnected(false);
            a.setConnectionStatus("DISCONNECTED");
            a.setAccessToken(null);
            socialAccountDAO.update(a);
        }
    }
}
