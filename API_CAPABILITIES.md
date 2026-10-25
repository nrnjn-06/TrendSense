# Social Media Official API Capabilities & Technical Reference

This document provides a technical specification of the official API capabilities, permitted account types, required permissions, and platform-imposed limitations for all integrated social platforms in **Social Analytics**.

In accordance with strict platform integrity guidelines:
- **No web scraping** is performed.
- **No passwords** are requested from users.
- **No private accounts** are simulated as accessible if the official API restricts them.
- All restricted features are gracefully handled with transparent fallbacks and explicit data availability tracking.

---

## 1. Platform Capability Comparison Matrix

| Platform | Official API & Version | Account Type Required | Authentication Method | Available Endpoints | Normalized Fields Retrieved | Platform Limitations & Rate Limits | Developer Requirements |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Instagram** | Meta Instagram Graph API (v19.0) | Instagram Business or Creator account connected to a Facebook Page | OAuth 2.0 User Access Token & Page Access Token | `/{page-id}?fields=instagram_business_account`, `/{ig-id}/media`, `/{media-id}/insights`, `/{media-id}/comments` | `id`, `caption`, `media_type`, `timestamp`, `like_count`, `comments_count`, `permalink`, `impressions` (via insights) | **Limitations:** Personal accounts cannot be queried. Share counts are not exposed via standard Graph API for photos/videos. Comments on boosted/ad posts may have separate moderation constraints.<br>**Rate Limit:** 200 calls/hour per user. | Meta Developer App in 'Live' mode or test user in 'Development' mode with `instagram_basic`, `instagram_manage_insights`, and `instagram_manage_comments` permissions. |
| **Facebook** | Meta Graph API (v19.0) Pages API | Facebook Page (Admin, Editor, or Moderator role) | OAuth 2.0 Page Access Token (derived from User Token) | `/{page-id}/posts`, `/{post-id}/insights`, `/{post-id}/comments`, `/{page-id}?fields=fan_count` | `id`, `message`, `created_time`, `likes` (via reactions summary), `comments_count`, `shares`, `permalink_url`, `post_impressions` | **Limitations:** Personal user timeline access (`user_posts`) is strictly deprecated and not permitted for analytics apps. Page insights require Page Read Engagement approval.<br>**Rate Limit:** 4,800 calls per client token bucket per 24h. | Meta Developer App with App Review approval for `pages_show_list`, `pages_read_engagement`, and `pages_read_user_content`. |
| **LinkedIn** | LinkedIn Community Management API (v2) | LinkedIn Company/Organization Page Admin OR Member context | OAuth 2.0 3-Legged Authorization Code Flow | `/v2/userinfo`, `/v2/ugcPosts`, `/v2/organizationalEntityAcls`, `/v2/socialActions/{urn}/comments` | `id`, `specificContent` (text commentary), `created` timestamp, `likes`, `comments`, `shares` (reposts) | **Limitations:** Accessing organization updates requires organization admin privileges and community management access approval. Full impression analytics requires LinkedIn Marketing Developer Platform permissions.<br>**Rate Limit:** 500 requests/day for member endpoints; higher for approved org tiers. | LinkedIn Developer Portal App approved for `r_organization_social`, `w_organization_social`, or `r_basicprofile`. |
| **X (Twitter)** | X API v2 (Twitter API v2) | Any X account with Developer Portal Application registration | OAuth 2.0 Bearer Token (App-Only) OR OAuth 2.0 with PKCE (User Context) | `/2/users/by/username/{user}`, `/2/users/{id}/tweets`, `/2/tweets/{id}` | `id`, `text`, `created_at`, `like_count`, `retweet_count`, `reply_count`, `quote_count`, `impression_count` | **Limitations:** Free tier is strictly limited to 1,500 Tweets per month write and limited reads. Public reply thread inspection requires Basic/Pro Tier ($100+/mo) for full search. Personal timeline scraping is forbidden.<br>**Rate Limit:** 15 requests per 15-minute window for user lookup and tweet lookup on basic tiers. | X Developer Portal Project & App with `tweet.read` and `users.read` scopes. |

---

## 2. Detailed Technical Breakdown by Platform

### A. Instagram Graph API (Meta)
1. **Prerequisites**:
   - An Instagram account switched to **Professional** (Business or Creator).
   - Linked Facebook Page where the user holds admin privileges.
2. **Access Token Flow**:
   - `User Access Token` $\rightarrow$ `/me/accounts` $\rightarrow$ Retrieve Facebook `Page Access Token` $\rightarrow$ Query `instagram_business_account` ID $\rightarrow$ Query Media & Insights.
3. **Data Availability**:
   - **Post Content**: Fully available (`caption`, `media_type`, `permalink`).
   - **Interactions**: `like_count`, `comments_count` are available.
   - **Shares**: **NOT PROVIDED** by official Instagram Graph API for standard organic media. In `SocialAnalytics`, `post.getShares()` defaults to 0 and does not distort metrics.
   - **Views / Impressions**: Available exclusively via the `/{media-id}/insights?metric=impressions,reach` endpoint.

---

### B. Facebook Graph API (Pages)
1. **Prerequisites**:
   - Published Facebook Page.
   - User must have page management permission.
2. **Access Token Flow**:
   - Page Access Token required.
3. **Data Availability**:
   - **Post Content**: `message` and `permalink_url` available.
   - **Reactions & Comments**: Retrieved via summary objects `reactions.summary(total_count)` and `comments.summary(total_count)`.
   - **Shares**: Available via `shares.count`.
   - **Impressions**: Requires page insights permission.

---

### C. LinkedIn Community Management API
1. **Prerequisites**:
   - Developer application approved for Community Management API product.
2. **Access Token Flow**:
   - 3-legged OAuth code exchange for access token.
3. **Data Availability**:
   - **Post Content**: Available inside `specificContent['com.linkedin.ugc.ShareContent']`.
   - **Interactions**: Available via `socialActions` endpoint.
   - **Shares**: Reposts tracked via LinkedIn shares count.

---

### D. X API v2 (Twitter)
1. **Prerequisites**:
   - App created in the X Developer Portal with Client ID & Client Secret or Project Bearer Token.
2. **Access Token Flow**:
   - OAuth 2.0 Bearer Token or User Context (PKCE).
3. **Data Availability**:
   - **Post Content**: Available in `tweet.text`.
   - **Metrics**: `like_count`, `retweet_count`, `reply_count`, `quote_count`, and `impression_count` (public metrics).
   - **Comments / Replies**: Free tier does not provide conversation timeline search without elevated paid subscription ($100/mo). Returns empty comments list gracefully rather than faking data.

---

## 3. Demo Mode Architectural Guarantees

When live platform credentials or permissions are not yet provisioned:
1. The user can toggle **Demo Mode**.
2. **DemoDataConnector** produces realistic normalized `SocialAccount`, `Post`, and `Comment` objects across Instagram, Facebook, LinkedIn, and X.
3. The UI explicitly displays:
   > **"Demo Data – Not connected to a live social-media platform."**
4. **No Code Duplication**: Both Demo Mode and Live API Mode feed into the identical:
   - `MultithreadedAnalyticsEngine`
   - `EngagementAnalyzer`
   - `SentimentAnalyzer`
   - `ContentAnalyzer`
   - `TrendAnalyzer` & `TrendScoringService`
   - `BestTimeAnalyzer`
   - `RecommendationEngine`
   - `PostDAO` & `CommentDAO` (MySQL / Embedded JDBC storage)
