package com.socialanalytics.model;

import java.time.LocalDateTime;

/**
 * Domain entity representing an authorized social media account/page connected to the application.
 */
public class SocialAccount {
    private Long id;
    private Long userId;
    private Platform platform;
    private String platformAccountId;
    private String username;
    private String displayName;
    private String profileUrl;
    private long followerCount;
    private long followingCount;
    private boolean connected;
    private boolean demo;
    private String accessToken;
    private LocalDateTime tokenExpiresAt;
    private LocalDateTime lastSyncedAt;
    private String connectionStatus = "CONNECTED";
    private String statusReason;

    public SocialAccount() {
    }

    public SocialAccount(Long id, Long userId, Platform platform, String platformAccountId,
                         String username, String displayName, String profileUrl,
                         long followerCount, long followingCount, boolean connected,
                         boolean demo, String accessToken, LocalDateTime tokenExpiresAt,
                         LocalDateTime lastSyncedAt) {
        this.id = id;
        this.userId = userId;
        this.platform = platform;
        this.platformAccountId = platformAccountId;
        this.username = username;
        this.displayName = displayName;
        this.profileUrl = profileUrl;
        this.followerCount = followerCount;
        this.followingCount = followingCount;
        this.connected = connected;
        this.demo = demo;
        this.accessToken = accessToken;
        this.tokenExpiresAt = tokenExpiresAt;
        this.lastSyncedAt = lastSyncedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Platform getPlatform() {
        return platform;
    }

    public void setPlatform(Platform platform) {
        this.platform = platform;
    }

    public String getPlatformAccountId() {
        return platformAccountId;
    }

    public void setPlatformAccountId(String platformAccountId) {
        this.platformAccountId = platformAccountId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getProfileUrl() {
        return profileUrl;
    }

    public void setProfileUrl(String profileUrl) {
        this.profileUrl = profileUrl;
    }

    public long getFollowerCount() {
        return followerCount;
    }

    public void setFollowerCount(long followerCount) {
        this.followerCount = followerCount;
    }

    public long getFollowingCount() {
        return followingCount;
    }

    public void setFollowingCount(long followingCount) {
        this.followingCount = followingCount;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public boolean isDemo() {
        return demo;
    }

    public void setDemo(boolean demo) {
        this.demo = demo;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public LocalDateTime getTokenExpiresAt() {
        return tokenExpiresAt;
    }

    public void setTokenExpiresAt(LocalDateTime tokenExpiresAt) {
        this.tokenExpiresAt = tokenExpiresAt;
    }

    public LocalDateTime getLastSyncedAt() {
        return lastSyncedAt;
    }

    public void setLastSyncedAt(LocalDateTime lastSyncedAt) {
        this.lastSyncedAt = lastSyncedAt;
    }

    public String getConnectionStatus() {
        return connectionStatus;
    }

    public void setConnectionStatus(String connectionStatus) {
        this.connectionStatus = connectionStatus;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }

    public boolean isConnectionFailed() {
        return "FAILED".equalsIgnoreCase(connectionStatus);
    }

    /**
     * Returns a masked version of the token for logging/display security.
     */
    public String getMaskedToken() {
        if (accessToken == null || accessToken.isBlank()) return "None";
        if (accessToken.length() <= 8) return "********";
        return accessToken.substring(0, 4) + "..." + accessToken.substring(accessToken.length() - 4);
    }

    @Override
    public String toString() {
        return "SocialAccount{" +
                "id=" + id +
                ", platform=" + platform +
                ", username='" + username + '\'' +
                ", displayName='" + displayName + '\'' +
                ", connected=" + connected +
                ", demo=" + demo +
                ", followers=" + followerCount +
                '}';
    }
}
