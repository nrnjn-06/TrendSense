package com.socialanalytics.model;

/**
 * Enumeration of supported social media platforms with metadata regarding
 * official API access models, permitted account types, and supported metrics.
 */
public enum Platform {
    INSTAGRAM(
            "Instagram",
            "Instagram Graph API",
            "Instagram Professional (Business or Creator) account",
            true, // supports comments
            true, // supports impressions/insights (with read_insights)
            "IMAGE, VIDEO, REEL, CAROUSEL",
            "https://www.instagram.com/oauth/authorize",
            "https://api.instagram.com/oauth/access_token",
            "instagram_business_basic,instagram_business_manage_comments,instagram_business_manage_insights",
            "https://developers.facebook.com/apps/",
            "instagram.client.id",
            "instagram.client.secret",
            "instagram.redirect.uri",
            "https://localhost:8080/oauth/callback/instagram"
    ),
    FACEBOOK(
            "Facebook",
            "Meta Graph API (Pages API)",
            "Facebook Page with Page Access Token (Page Admin/Editor role)",
            true, // supports comments
            true, // supports page & post impressions
            "STATUS, PHOTO, VIDEO, LINK",
            "https://www.facebook.com/v19.0/dialog/oauth",
            "https://graph.facebook.com/v19.0/oauth/access_token",
            "pages_show_list,pages_read_engagement,pages_read_user_content",
            "https://developers.facebook.com/apps/",
            "facebook.app.id",
            "facebook.app.secret",
            "facebook.redirect.uri",
            "http://localhost:8080/oauth/callback/facebook"
    ),
    LINKEDIN(
            "LinkedIn",
            "LinkedIn Community Management API / Share API",
            "LinkedIn Organization / Company Page Admin role (w_member_social / w_organization_social)",
            true, // supports comments
            true, // supports impressions / clicks
            "ARTICLE, IMAGE, VIDEO, TEXT",
            "https://www.linkedin.com/oauth/v2/authorization",
            "https://www.linkedin.com/oauth/v2/accessToken",
            "openid,profile,email,r_organization_social,w_member_social",
            "https://www.linkedin.com/developers/apps",
            "linkedin.client.id",
            "linkedin.client.secret",
            "linkedin.redirect.uri",
            "http://localhost:8080/oauth/callback/linkedin"
    ),
    X(
            "X",
            "X API v2 (Twitter API v2)",
            "X Developer App with OAuth 2.0 User Context (tweet.read, users.read)",
            false, // public free tier comments reading severely limited without Basic/Pro
            false, // impressions require Organic Tweet Metrics permission (elevated)
            "TWEET, RETWEET, QUOTE, POLL",
            "https://twitter.com/i/oauth2/authorize",
            "https://api.twitter.com/2/oauth2/token",
            "tweet.read users.read offline.access",
            "https://developer.x.com/en/portal/dashboard",
            "x.client.id",
            "x.client.secret",
            "x.redirect.uri",
            "http://localhost:8080/oauth/callback/x"
    );

    private final String displayName;
    private final String apiType;
    private final String requiredAccountType;
    private final boolean supportsComments;
    private final boolean supportsImpressions;
    private final String supportedContentTypes;
    private final String oauthAuthUrl;
    private final String oauthTokenUrl;
    private final String defaultScopes;
    private final String developerPortalUrl;
    private final String clientIdConfigKey;
    private final String clientSecretConfigKey;
    private final String redirectUriConfigKey;
    private final String defaultRedirectUri;

    Platform(String displayName, String apiType, String requiredAccountType,
             boolean supportsComments, boolean supportsImpressions, String supportedContentTypes,
             String oauthAuthUrl, String oauthTokenUrl, String defaultScopes,
             String developerPortalUrl, String clientIdConfigKey, String clientSecretConfigKey,
             String redirectUriConfigKey, String defaultRedirectUri) {
        this.displayName = displayName;
        this.apiType = apiType;
        this.requiredAccountType = requiredAccountType;
        this.supportsComments = supportsComments;
        this.supportsImpressions = supportsImpressions;
        this.supportedContentTypes = supportedContentTypes;
        this.oauthAuthUrl = oauthAuthUrl;
        this.oauthTokenUrl = oauthTokenUrl;
        this.defaultScopes = defaultScopes;
        this.developerPortalUrl = developerPortalUrl;
        this.clientIdConfigKey = clientIdConfigKey;
        this.clientSecretConfigKey = clientSecretConfigKey;
        this.redirectUriConfigKey = redirectUriConfigKey;
        this.defaultRedirectUri = defaultRedirectUri;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getApiType() {
        return apiType;
    }

    public String getRequiredAccountType() {
        return requiredAccountType;
    }

    public boolean isSupportsComments() {
        return supportsComments;
    }

    public boolean isSupportsImpressions() {
        return supportsImpressions;
    }

    public String getSupportedContentTypes() {
        return supportedContentTypes;
    }

    public String getOauthAuthUrl() {
        return oauthAuthUrl;
    }

    public String getOauthTokenUrl() {
        return oauthTokenUrl;
    }

    public String getDefaultScopes() {
        return defaultScopes;
    }

    public String getDeveloperPortalUrl() {
        return developerPortalUrl;
    }

    public String getClientIdConfigKey() {
        return clientIdConfigKey;
    }

    public String getClientSecretConfigKey() {
        return clientSecretConfigKey;
    }

    public String getRedirectUriConfigKey() {
        return redirectUriConfigKey;
    }

    public String getDefaultRedirectUri() {
        return defaultRedirectUri;
    }

    public static Platform fromString(String text) {
        if (text == null) return null;
        for (Platform p : Platform.values()) {
            if (p.name().equalsIgnoreCase(text.trim()) || p.displayName.equalsIgnoreCase(text.trim())) {
                return p;
            }
        }
        throw new IllegalArgumentException("Unknown platform: " + text);
    }
}
