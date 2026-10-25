package com.socialanalytics;

import com.socialanalytics.connector.XConnector;
import com.socialanalytics.exception.AuthenticationException;
import com.socialanalytics.model.Platform;
import com.socialanalytics.model.SocialAccount;
import com.socialanalytics.service.ConfigService;
import com.socialanalytics.service.OAuthService;
import com.socialanalytics.service.SocialAccountService;
import java.io.IOException;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class OAuthServiceTest {

    private OAuthService oAuthService;
    private SocialAccountService socialAccountService;
    private ConfigService configService;

    @BeforeEach
    void setUp() {
        configService = ConfigService.getInstance();
        oAuthService = OAuthService.getInstance();
        socialAccountService = SocialAccountService.getInstance();
    }

    @AfterEach
    void tearDown() {
        // Clean up any test overrides in ConfigService
        configService.setProperty("X_CLIENT_ID", "");
        configService.setProperty("TWITTER_CLIENT_ID", "");
        configService.setProperty("X_CLIENT_SECRET", "");
        configService.setProperty("TWITTER_CLIENT_SECRET", "");
        configService.setProperty("x.client.id", "");
        configService.setProperty("x.client.secret", "");
    }

    @Test
    @DisplayName("Verify platform configuration guidance and metadata for all platforms")
    void testPlatformConfigGuide() {
        for (Platform platform : Platform.values()) {
            OAuthService.PlatformConfigGuide guide = oAuthService.getConfigGuide(platform);
            assertNotNull(guide);
            assertEquals(platform, guide.platform());
            assertNotNull(guide.developerPortalUrl());
            assertFalse(guide.developerPortalUrl().isBlank());
            assertNotNull(guide.requiredAccountType());
            assertFalse(guide.requiredAccountType().isBlank());
            assertNotNull(guide.requiredScopes());
            assertFalse(guide.requiredScopes().isBlank());
            assertTrue(guide.redirectUri().contains("/oauth/callback/"));
        }
    }

    @Test
    @DisplayName("Verify buildAuthorizationUrl creates correct official OAuth 2.0 URLs with PKCE for X")
    void testBuildAuthorizationUrl() {
        String state = "test_state_123";

        // Instagram
        String igUrl = oAuthService.buildAuthorizationUrl(Platform.INSTAGRAM, state);
        assertTrue(igUrl.startsWith("https://www.facebook.com/v19.0/dialog/oauth") || igUrl.startsWith("https://www.instagram.com/oauth/authorize"));
        assertTrue(igUrl.contains("response_type=code"));
        assertTrue(igUrl.contains("state=test_state_123"));
        assertTrue(igUrl.contains("scope="));

        // LinkedIn
        String liUrl = oAuthService.buildAuthorizationUrl(Platform.LINKEDIN, state);
        assertTrue(liUrl.startsWith("https://www.linkedin.com/oauth/v2/authorization"));
        assertTrue(liUrl.contains("response_type=code"));

        // X (with PKCE RFC 7636)
        String xUrl = oAuthService.buildAuthorizationUrl(Platform.X, state);
        assertTrue(xUrl.startsWith("https://twitter.com/i/oauth2/authorize"));
        assertTrue(xUrl.contains("code_challenge="));
        assertTrue(xUrl.contains("code_challenge_method="));
    }

    @Test
    @DisplayName("Verify exchangeCodeForToken throws descriptive exception when unconfigured")
    void testExchangeCodeForTokenThrowsWhenUnconfigured() {
        configService.setProperty("X_CLIENT_ID", "");
        configService.setProperty("TWITTER_CLIENT_ID", "");
        configService.setProperty("X_CLIENT_SECRET", "");
        configService.setProperty("TWITTER_CLIENT_SECRET", "");

        AuthenticationException ex = assertThrows(AuthenticationException.class, () ->
                oAuthService.exchangeCodeForToken(Platform.X, "sample_test_code")
        );
        assertTrue(ex.getMessage().contains("credentials are not configured") || ex.getMessage().contains("OAuth"));
    }

    @Test
    @DisplayName("Verify Instagram is configured with App ID and Secret")
    void testInstagramIsConfigured() {
        configService.setProperty("INSTAGRAM_CLIENT_ID", "mock_instagram_id_123");
        configService.setProperty("INSTAGRAM_CLIENT_SECRET", "mock_instagram_secret_456");
        try {
            assertTrue(oAuthService.isConfigured(Platform.INSTAGRAM));
            assertFalse(oAuthService.getClientId(Platform.INSTAGRAM).isBlank());
            assertFalse(oAuthService.getClientSecret(Platform.INSTAGRAM).isBlank());
            String authUrl = oAuthService.buildAuthorizationUrl(Platform.INSTAGRAM, "csrf_123");
            assertTrue(authUrl.contains("client_id=" + oAuthService.getClientId(Platform.INSTAGRAM)));
            assertTrue(authUrl.contains("oauth%2Fcallback%2Finstagram"));
        } finally {
            configService.setProperty("INSTAGRAM_CLIENT_ID", "");
            configService.setProperty("INSTAGRAM_CLIENT_SECRET", "");
        }
    }

    @Test
    @DisplayName("Verify recording connection failure and status reasons in SocialAccountService")
    void testRecordConnectionFailure() {
        com.socialanalytics.dao.UserDAO userDAO = new com.socialanalytics.dao.impl.JdbcUserDAO();
        com.socialanalytics.model.User testUser = new com.socialanalytics.model.User();
        testUser.setUsername("oauthtester_" + System.currentTimeMillis());
        testUser.setEmail("oauth_" + System.currentTimeMillis() + "@test.com");
        testUser.setPasswordHash("dummyhash");
        testUser.setSalt("dummysalt");
        testUser = userDAO.create(testUser);
        Long testUserId = testUser.getId();

        String reason = "OAuth client ID missing in .env";

        SocialAccount failedAcc = socialAccountService.recordConnectionFailure(testUserId, Platform.INSTAGRAM, reason);
        assertNotNull(failedAcc);
        assertFalse(failedAcc.isConnected());
        assertEquals("FAILED", failedAcc.getConnectionStatus());
        assertEquals(reason, failedAcc.getStatusReason());
        assertTrue(failedAcc.isConnectionFailed());

        // Now test connecting in Demo Mode clears the failure
        SocialAccount demoAcc = socialAccountService.connectDemoAccount(testUserId, Platform.INSTAGRAM);
        assertTrue(demoAcc.isConnected());
        assertEquals("CONNECTED", demoAcc.getConnectionStatus());
        assertNull(demoAcc.getStatusReason());
        assertFalse(demoAcc.isConnectionFailed());
        assertTrue(demoAcc.isDemo());
    }

    @Test
    @DisplayName("Verify X OAuth alias resolution for TWITTER_CLIENT_ID and TWITTER_CLIENT_SECRET")
    void testXOAuthTwitterPrefixAliasResolution() {
        configService.setProperty("TWITTER_CLIENT_ID", "mock_twitter_id_12345");
        configService.setProperty("TWITTER_CLIENT_SECRET", "mock_twitter_secret_67890");

        assertEquals("mock_twitter_id_12345", oAuthService.getClientId(Platform.X));
        assertEquals("mock_twitter_secret_67890", oAuthService.getClientSecret(Platform.X));
        assertTrue(oAuthService.isConfigured(Platform.X));

        String authUrl = oAuthService.buildAuthorizationUrl(Platform.X, "state_x");
        assertTrue(authUrl.contains("client_id=mock_twitter_id_12345"));
        assertTrue(authUrl.contains("code_challenge="));
    }

    @Test
    @DisplayName("Verify X OAuth alias resolution for X_CLIENT_ID and X_CLIENT_SECRET")
    void testXOAuthXPrefixAliasResolution() {
        configService.setProperty("X_CLIENT_ID", "mock_x_id_abcde");
        configService.setProperty("X_CLIENT_SECRET", "mock_x_secret_fghij");

        assertEquals("mock_x_id_abcde", oAuthService.getClientId(Platform.X));
        assertEquals("mock_x_secret_fghij", oAuthService.getClientSecret(Platform.X));
        assertTrue(oAuthService.isConfigured(Platform.X));

        String authUrl = oAuthService.buildAuthorizationUrl(Platform.X, "state_x_2");
        assertTrue(authUrl.contains("client_id=mock_x_id_abcde"));
    }

    @Test
    @DisplayName("Verify trimming surrounding whitespace and quotes from configuration")
    void testWhitespaceAndQuotesTrimming() {
        configService.setProperty("X_CLIENT_ID", "  \"mock_trimmed_id\"  ");
        configService.setProperty("X_CLIENT_SECRET", "  'mock_trimmed_secret'  ");

        assertEquals("mock_trimmed_id", oAuthService.getClientId(Platform.X));
        assertEquals("mock_trimmed_secret", oAuthService.getClientSecret(Platform.X));
        assertTrue(oAuthService.isConfigured(Platform.X));
    }

    @Test
    @DisplayName("Verify missing client secret prevents X from being marked as configured")
    void testMissingSecretFailsConfiguration() {
        configService.setProperty("X_CLIENT_ID", "only_client_id_present");
        configService.setProperty("X_CLIENT_SECRET", "");
        configService.setProperty("TWITTER_CLIENT_SECRET", "");

        assertFalse(oAuthService.isConfigured(Platform.X));
        OAuthService.PlatformConfigGuide guide = oAuthService.getConfigGuide(Platform.X);
        assertTrue(guide.missingVariables().contains("X_CLIENT_SECRET") || guide.missingVariables().contains("TWITTER_CLIENT_SECRET"));
    }

    @Test
    @DisplayName("Verify user cancellation in callback URL is gracefully handled")
    void testUserCancellationCallbackHandling() {
        configService.setProperty("X_CLIENT_ID", "test_id");
        configService.setProperty("X_CLIENT_SECRET", "test_secret");

        String cancelUrl = "http://localhost:8080/oauth/callback/x?error=access_denied&error_description=The+user+denied+access&state=x_1234";

        AuthenticationException ex = assertThrows(AuthenticationException.class, () ->
                oAuthService.exchangeCodeForToken(Platform.X, cancelUrl)
        );
        assertTrue(ex.getMessage().contains("cancelled"));
    }

    @Test
    @DisplayName("Verify successful token exchange using mocked HTTP response")
    void testSuccessfulTokenExchangeWithMockHttp() throws Exception {
        configService.setProperty("X_CLIENT_ID", "test_mock_id");
        configService.setProperty("X_CLIENT_SECRET", "test_mock_secret");

        StubHttpClient mockHttp = new StubHttpClient(200, "{\"token_type\":\"bearer\",\"expires_in\":7200,\"access_token\":\"mock_real_x_token_999\"}");
        OAuthService customService = new OAuthService(configService, mockHttp);
        String token = customService.exchangeCodeForToken(Platform.X, "valid_mock_code_123");

        assertEquals("mock_real_x_token_999", token);
    }

    @Test
    @DisplayName("Verify token exchange failure extracts API error details correctly")
    void testTokenExchangeFailureExtractsDetails() {
        configService.setProperty("X_CLIENT_ID", "test_mock_id");
        configService.setProperty("X_CLIENT_SECRET", "test_mock_secret");

        StubHttpClient mockHttp = new StubHttpClient(400, "{\"error\":\"invalid_grant\",\"error_description\":\"Value passed for the token was invalid.\"}");
        OAuthService customService = new OAuthService(configService, mockHttp);
        AuthenticationException ex = assertThrows(AuthenticationException.class, () ->
                customService.exchangeCodeForToken(Platform.X, "expired_code_xyz")
        );

        assertTrue(ex.getMessage().contains("Value passed for the token was invalid"));
    }

    @Test
    @DisplayName("Verify Demo Mode separation and disconnecting clears access token")
    void testDemoModeSeparationAndDisconnect() {
        com.socialanalytics.dao.UserDAO userDAO = new com.socialanalytics.dao.impl.JdbcUserDAO();
        com.socialanalytics.model.User testUser = new com.socialanalytics.model.User();
        testUser.setUsername("demotester_" + System.currentTimeMillis());
        testUser.setEmail("demotester_" + System.currentTimeMillis() + "@test.com");
        testUser.setPasswordHash("dummyhash");
        testUser.setSalt("dummysalt");
        testUser = userDAO.create(testUser);
        Long testUserId = testUser.getId();

        // 1. Connect in demo mode
        SocialAccount demoAcc = socialAccountService.connectDemoAccount(testUserId, Platform.X);
        assertTrue(demoAcc.isDemo(), "Demo account must have isDemo() == true");
        assertTrue(demoAcc.isConnected());

        // 2. Disconnect platform
        socialAccountService.disconnectPlatformForUser(testUserId, Platform.X);
        SocialAccount disconnected = socialAccountService.getAccountForUserAndPlatform(testUserId, Platform.X).orElseThrow();
        assertFalse(disconnected.isConnected());
        assertEquals("DISCONNECTED", disconnected.getConnectionStatus());
        assertNull(disconnected.getAccessToken(), "Access token must be null after disconnect");
    }

    // --- Helper Mock / Stub HTTP Client without third-party libraries ---
    private static class StubHttpClient extends HttpClient {
        private final int statusCode;
        private final String body;

        public StubHttpClient(int statusCode, String body) {
            this.statusCode = statusCode;
            this.body = body;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> responseInfo) {
            return (HttpResponse<T>) new StubHttpResponse(statusCode, body, request.uri());
        }

        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) {
            return CompletableFuture.completedFuture(send(request, responseBodyHandler));
        }

        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler, HttpResponse.PushPromiseHandler<T> pushPromiseHandler) {
            return sendAsync(request, responseBodyHandler);
        }

        @Override
        public Optional<CookieHandler> cookieHandler() { return Optional.empty(); }
        @Override
        public Optional<Duration> connectTimeout() { return Optional.empty(); }
        @Override
        public Redirect followRedirects() { return Redirect.NEVER; }
        @Override
        public Optional<ProxySelector> proxy() { return Optional.empty(); }
        @Override
        public SSLContext sslContext() { return null; }
        @Override
        public SSLParameters sslParameters() { return new SSLParameters(); }
        @Override
        public Optional<Authenticator> authenticator() { return Optional.empty(); }
        @Override
        public Version version() { return Version.HTTP_2; }
        @Override
        public Optional<Executor> executor() { return Optional.empty(); }
    }

    private static class StubHttpResponse implements HttpResponse<String> {
        private final int statusCode;
        private final String body;
        private final URI uri;

        public StubHttpResponse(int statusCode, String body, URI uri) {
            this.statusCode = statusCode;
            this.body = body;
            this.uri = uri;
        }

        @Override
        public int statusCode() { return statusCode; }
        @Override
        public HttpRequest request() { return null; }
        @Override
        public Optional<HttpResponse<String>> previousResponse() { return Optional.empty(); }
        @Override
        public HttpHeaders headers() { return HttpHeaders.of(Map.of(), (k, v) -> true); }
        @Override
        public String body() { return body; }
        @Override
        public Optional<SSLSession> sslSession() { return Optional.empty(); }
        @Override
        public URI uri() { return uri; }
        @Override
        public HttpClient.Version version() { return HttpClient.Version.HTTP_2; }
    }
}
