package com.socialanalytics;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.socialanalytics.connector.XConnector;
import com.socialanalytics.exception.ApiException;
import com.socialanalytics.exception.AuthenticationException;
import com.socialanalytics.model.Platform;
import com.socialanalytics.model.Post;
import com.socialanalytics.model.SocialAccount;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class XConnectorTest {

    private static class TestableXConnector extends XConnector {
        private String lastRequestedUrl;
        private String mockJsonResponse;
        private int executeCount = 0;

        public TestableXConnector(String mockJsonResponse) {
            super("mock_test_token_123");
            this.mockJsonResponse = mockJsonResponse;
        }

        @Override
        protected JsonObject executeGet(String url, String token) throws ApiException {
            this.lastRequestedUrl = url;
            this.executeCount++;
            return JsonParser.parseString(mockJsonResponse).getAsJsonObject();
        }

        public String getLastRequestedUrl() {
            return lastRequestedUrl;
        }

        public int getExecuteCount() {
            return executeCount;
        }
    }

    @Test
    @DisplayName("Verify XConnector uses /users/me when given an OAuth access token or me")
    void testFetchAccountInfoRoutesToUsersMe() throws Exception {
        String jsonPayload = """
            {
              "data": {
                "id": "2244994945",
                "name": "TrendSense Dev",
                "username": "TrendSenseApp",
                "profile_image_url": "https://pbs.twimg.com/profile_images/123/pic.jpg",
                "public_metrics": {
                  "followers_count": 4250,
                  "following_count": 310,
                  "tweet_count": 89
                }
              }
            }
            """;

        TestableXConnector connector = new TestableXConnector(jsonPayload);

        // 1. Passing "me"
        SocialAccount acc1 = connector.fetchAccountInfo("me");
        assertNotNull(acc1);
        assertTrue(connector.getLastRequestedUrl().contains("/users/me"));
        assertEquals("2244994945", acc1.getPlatformAccountId());
        assertEquals("TrendSenseApp", acc1.getUsername());
        assertEquals("TrendSense Dev", acc1.getDisplayName());
        assertEquals(4250, acc1.getFollowerCount());
        assertEquals(310, acc1.getFollowingCount());
        assertEquals(Platform.X, acc1.getPlatform());
        assertTrue(acc1.isConnected());
        assertFalse(acc1.isDemo());

        // 2. Passing the OAuth access token (which is a long bearer token)
        SocialAccount acc2 = connector.fetchAccountInfo("mock_test_token_123");
        assertNotNull(acc2);
        assertTrue(connector.getLastRequestedUrl().contains("/users/me"));

        // 3. Passing null or blank
        SocialAccount acc3 = connector.fetchAccountInfo("");
        assertNotNull(acc3);
        assertTrue(connector.getLastRequestedUrl().contains("/users/me"));
    }

    @Test
    @DisplayName("Verify XConnector routes to /users/by/username/ when given a Twitter handle")
    void testFetchAccountInfoRoutesToUsername() throws Exception {
        String jsonPayload = """
            {
              "data": {
                "id": "998877",
                "name": "Official Handle",
                "username": "sample_handle",
                "public_metrics": {
                  "followers_count": 100,
                  "following_count": 50
                }
              }
            }
            """;

        TestableXConnector connector = new TestableXConnector(jsonPayload);
        SocialAccount acc = connector.fetchAccountInfo("@sample_handle");

        assertNotNull(acc);
        assertTrue(connector.getLastRequestedUrl().contains("/users/by/username/sample_handle"));
        assertEquals("sample_handle", acc.getUsername());
    }

    @Test
    @DisplayName("Verify XConnector parses tweets and metrics in fetchPosts")
    void testFetchPostsParsesPublicMetrics() throws Exception {
        String jsonPayload = """
            {
              "data": [
                {
                  "id": "1890001",
                  "text": "Excited to test the new TrendSense Sage Green dashboard! #AI #Analytics",
                  "created_at": "2026-03-15T10:00:00.000Z",
                  "public_metrics": {
                    "like_count": 150,
                    "reply_count": 25,
                    "retweet_count": 40,
                    "quote_count": 10,
                    "impression_count": 5400
                  },
                  "entities": {
                    "hashtags": [
                      {"tag": "AI"},
                      {"tag": "Analytics"}
                    ]
                  }
                }
              ]
            }
            """;

        TestableXConnector connector = new TestableXConnector(jsonPayload);
        List<Post> posts = connector.fetchPosts("2244994945", 10);

        assertNotNull(posts);
        assertEquals(1, posts.size());
        Post p = posts.get(0);
        assertEquals("1890001", p.getId());
        assertEquals(Platform.X, p.getPlatform());
        assertEquals(150, p.getLikes());
        assertEquals(25, p.getCommentsCount());
        assertEquals(50, p.getShares()); // 40 retweets + 10 quotes
        assertEquals(5400L, p.getViews());
        assertTrue(p.getHashtags().contains("ai"));
        assertTrue(p.getHashtags().contains("analytics"));
    }

    @Test
    @DisplayName("Verify unauthenticated XConnector throws AuthenticationException")
    void testUnauthenticatedConnectorThrows() {
        XConnector connector = new XConnector(); // No token supplied
        assertThrows(AuthenticationException.class, () -> connector.fetchAccountInfo("me"));
        assertThrows(AuthenticationException.class, () -> connector.fetchPosts("123", 10));
    }
}
