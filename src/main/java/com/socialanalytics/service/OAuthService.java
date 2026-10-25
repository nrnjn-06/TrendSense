package com.socialanalytics.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.socialanalytics.exception.AuthenticationException;
import com.socialanalytics.model.Platform;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service encapsulating official OAuth 2.0 authorization flows, credential verification,
 * authorization URL construction, PKCE challenge generation, and code-for-token exchange across supported platforms.
 */
public class OAuthService {
    private static final OAuthService INSTANCE = new OAuthService();

    private final ConfigService configService;
    private final HttpClient httpClient;
    private final SecureRandom secureRandom = new SecureRandom();

    // In-memory cache for RFC 7636 PKCE verifiers keyed by OAuth state and platform
    private final Map<String, String> pkceVerifiersByState = new ConcurrentHashMap<>();
    private final Map<Platform, String> lastPkceVerifiersByPlatform = new ConcurrentHashMap<>();

    // Standard fallback 64-character RFC 7636 compliant verifier
    private static final String DEFAULT_PKCE_VERIFIER = "SocialAnalyticsTrendSenseOAuth2PkceChallengeVerifierKeyString1234567890";

    public OAuthService() {
        this(ConfigService.getInstance(), HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build());
    }

    public OAuthService(ConfigService configService, HttpClient httpClient) {
        this.configService = configService;
        this.httpClient = httpClient;
    }

    public static OAuthService getInstance() {
        return INSTANCE;
    }

    /**
     * Checks if the required OAuth credentials (Client ID and Client Secret)
     * are configured in the environment (.env, system environment, or application.properties).
     */
    public boolean isConfigured(Platform platform) {
        String clientId = getClientId(platform);
        String clientSecret = getClientSecret(platform);
        return clientId != null && !clientId.isBlank() && clientSecret != null && !clientSecret.isBlank();
    }

    public String getClientId(Platform platform) {
        String id = configService.get(platform.getClientIdConfigKey(), "").trim();
        if (id.isBlank() && platform == Platform.INSTAGRAM) {
            // Re-use Facebook/Meta App ID if Instagram-specific ID is not configured
            id = configService.get(Platform.FACEBOOK.getClientIdConfigKey(), "").trim();
            if (id.isBlank()) {
                id = configService.get("meta.app.id", "").trim();
            }
        }
        return id;
    }

    public String getClientSecret(Platform platform) {
        String secret = configService.get(platform.getClientSecretConfigKey(), "").trim();
        if (secret.isBlank() && platform == Platform.INSTAGRAM) {
            // Re-use Facebook/Meta App Secret if Instagram-specific secret is not configured
            secret = configService.get(Platform.FACEBOOK.getClientSecretConfigKey(), "").trim();
            if (secret.isBlank()) {
                secret = configService.get("meta.app.secret", "").trim();
            }
        }
        return secret;
    }

    public String getRedirectUri(Platform platform) {
        String custom = configService.get(platform.getRedirectUriConfigKey(), "").trim();
        if (custom.isBlank() && platform == Platform.INSTAGRAM) {
            String metaRedirect = configService.get("meta.redirect.uri", "").trim();
            if (!metaRedirect.isBlank()) {
                return metaRedirect;
            }
        }
        return custom.isBlank() ? platform.getDefaultRedirectUri() : custom;
    }

    /**
     * Generates a unique state parameter for CSRF protection.
     */
    public String generateState(Platform platform) {
        return platform.name().toLowerCase() + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Constructs the official platform authorization URL with required parameters and PKCE for X.
     */
    public String buildAuthorizationUrl(Platform platform, String state) {
        String clientId = getClientId(platform);
        String redirectUri = getRedirectUri(platform);
        String customScopes = configService.get(platform.name().toLowerCase() + ".oauth.scopes", "").trim();
        String scopes = customScopes.isBlank() ? platform.getDefaultScopes() : customScopes;

        StringBuilder url = new StringBuilder(platform.getOauthAuthUrl());
        url.append("?response_type=code");
        url.append("&client_id=").append(urlEncode(clientId));
        url.append("&redirect_uri=").append(urlEncode(redirectUri));
        url.append("&scope=").append(urlEncode(scopes));
        if (state != null && !state.isBlank()) {
            url.append("&state=").append(urlEncode(state));
        }

        // Platform-specific adjustments
        if (platform == Platform.X) {
            // Generate RFC 7636 compliant PKCE verifier (43 to 128 characters)
            String verifier = generatePkceVerifier();
            if (state != null && !state.isBlank()) {
                pkceVerifiersByState.put(state, verifier);
            }
            lastPkceVerifiersByPlatform.put(platform, verifier);

            PkceChallenge challenge = createPkceChallenge(verifier);
            url.append("&code_challenge=").append(urlEncode(challenge.challenge()));
            url.append("&code_challenge_method=").append(urlEncode(challenge.method()));
        }

        return url.toString();
    }

    private String generatePkceVerifier() {
        byte[] bytes = new byte[48]; // 48 bytes -> 64 URL-safe chars (43..128 range)
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private PkceChallenge createPkceChallenge(String verifier) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(verifier.getBytes(StandardCharsets.US_ASCII));
            String challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
            return new PkceChallenge(challenge, "S256");
        } catch (NoSuchAlgorithmException e) {
            return new PkceChallenge(verifier, "plain");
        }
    }

    private record PkceChallenge(String challenge, String method) {}

    /**
     * Exchanges an authorization code for an official platform access token.
     */
    public String exchangeCodeForToken(Platform platform, String authorizationCode) throws AuthenticationException {
        return exchangeCodeForToken(platform, authorizationCode, null);
    }

    /**
     * Exchanges an authorization code (or callback URL) for an official platform access token,
     * validating state and retrieving cached PKCE verifiers.
     */
    public String exchangeCodeForToken(Platform platform, String authorizationCodeOrUrl, String stateOverride) throws AuthenticationException {
        if (authorizationCodeOrUrl == null || authorizationCodeOrUrl.isBlank()) {
            throw new AuthenticationException("Authorization code cannot be empty.");
        }

        String rawInput = authorizationCodeOrUrl.trim();

        // Check for error responses from platform callback
        if (rawInput.contains("error=access_denied") || rawInput.contains("error=user_cancelled_authorize")) {
            throw new AuthenticationException("Authorization was cancelled by the user on " + platform.getDisplayName() + ".");
        }
        if (rawInput.contains("error=")) {
            String error = extractParam(rawInput, "error");
            String desc = extractParam(rawInput, "error_description");
            throw new AuthenticationException("Platform authorization error: " + (desc != null ? desc : error));
        }

        // Clean and extract code if full URL was provided
        String code = rawInput;
        if (rawInput.contains("code=")) {
            code = extractParam(rawInput, "code");
        }
        if (code != null && code.contains("#")) {
            code = code.substring(0, code.indexOf('#'));
        }
        if (code == null || code.isBlank()) {
            throw new AuthenticationException("Could not extract valid authorization code from input.");
        }
        code = code.trim();

        // Extract state if available
        String resolvedState = stateOverride;
        if (resolvedState == null && rawInput.contains("state=")) {
            resolvedState = extractParam(rawInput, "state");
        }

        String clientId = getClientId(platform);
        String clientSecret = getClientSecret(platform);
        String redirectUri = getRedirectUri(platform);

        if (clientId.isBlank() || clientSecret.isBlank()) {
            throw new AuthenticationException("Cannot exchange code: OAuth application credentials are not configured for " + platform.getDisplayName());
        }

        try {
            HttpRequest request;
            if (platform == Platform.INSTAGRAM) {
                // Direct Instagram OAuth token exchange
                String postBody = "client_id=" + urlEncode(clientId) +
                        "&client_secret=" + urlEncode(clientSecret) +
                        "&grant_type=authorization_code" +
                        "&redirect_uri=" + urlEncode(redirectUri) +
                        "&code=" + urlEncode(code);

                request = HttpRequest.newBuilder()
                        .uri(URI.create(platform.getOauthTokenUrl()))
                        .timeout(Duration.ofSeconds(15))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .header("Accept", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(postBody))
                        .build();

            } else if (platform == Platform.FACEBOOK) {
                // Meta Graph API token exchange
                String postBody = "client_id=" + urlEncode(clientId) +
                        "&client_secret=" + urlEncode(clientSecret) +
                        "&redirect_uri=" + urlEncode(redirectUri) +
                        "&code=" + urlEncode(code);

                request = HttpRequest.newBuilder()
                        .uri(URI.create(platform.getOauthTokenUrl()))
                        .timeout(Duration.ofSeconds(15))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .header("Accept", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(postBody))
                        .build();

            } else if (platform == Platform.LINKEDIN) {
                // LinkedIn OAuth 2.0 token exchange
                String postBody = "grant_type=authorization_code" +
                        "&code=" + urlEncode(code) +
                        "&redirect_uri=" + urlEncode(redirectUri) +
                        "&client_id=" + urlEncode(clientId) +
                        "&client_secret=" + urlEncode(clientSecret);

                request = HttpRequest.newBuilder()
                        .uri(URI.create(platform.getOauthTokenUrl()))
                        .timeout(Duration.ofSeconds(15))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .header("Accept", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(postBody))
                        .build();

            } else if (platform == Platform.X) {
                // X API v2 OAuth 2.0 PKCE token exchange
                String pkceVerifier = null;
                if (resolvedState != null && pkceVerifiersByState.containsKey(resolvedState)) {
                    pkceVerifier = pkceVerifiersByState.remove(resolvedState);
                }
                if (pkceVerifier == null) {
                    pkceVerifier = lastPkceVerifiersByPlatform.get(Platform.X);
                }
                if (pkceVerifier == null || pkceVerifier.isBlank()) {
                    pkceVerifier = DEFAULT_PKCE_VERIFIER;
                }

                String postBody = "grant_type=authorization_code" +
                        "&code=" + urlEncode(code) +
                        "&redirect_uri=" + urlEncode(redirectUri) +
                        "&code_verifier=" + urlEncode(pkceVerifier) +
                        "&client_id=" + urlEncode(clientId);

                HttpRequest.Builder b = HttpRequest.newBuilder()
                        .uri(URI.create(platform.getOauthTokenUrl()))
                        .timeout(Duration.ofSeconds(15))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .header("Accept", "application/json");

                // Confidential client HTTP Basic Auth
                if (!clientSecret.isBlank()) {
                    String authStr = Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));
                    b.header("Authorization", "Basic " + authStr);
                }

                b.POST(HttpRequest.BodyPublishers.ofString(postBody));
                request = b.build();

            } else {
                throw new AuthenticationException("Unsupported platform for OAuth exchange: " + platform);
            }

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body();

            if (response.statusCode() >= 400) {
                String errorDetail = extractErrorMessage(responseBody);
                String detail;
                if (response.statusCode() == 400) {
                    detail = "Invalid request parameter or expired code. Verify redirect URI (" + redirectUri + "). " + errorDetail;
                } else if (response.statusCode() == 401) {
                    detail = "Unauthorized client credentials. Check " + (platform == Platform.X ? "X_CLIENT_ID / TWITTER_CLIENT_ID and X_CLIENT_SECRET" : "Client ID and Secret") + ". " + errorDetail;
                } else if (response.statusCode() == 403) {
                    detail = "Forbidden access. Developer app lacks approved permissions. " + errorDetail;
                } else if (response.statusCode() == 429) {
                    detail = "Rate limit reached. Quota exceeded. " + errorDetail;
                } else {
                    detail = errorDetail;
                }
                throw new AuthenticationException("OAuth token exchange failed (HTTP " + response.statusCode() + "): " + detail);
            }

            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
            if (json.has("access_token")) {
                String token = json.get("access_token").getAsString();
                if (platform == Platform.INSTAGRAM) {
                    token = exchangeForLongLivedInstagramToken(token, clientSecret);
                }
                return token;
            } else {
                throw new AuthenticationException("Platform response did not contain an access_token: " + responseBody);
            }

        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AuthenticationException("Network error during OAuth token exchange: " + e.getMessage(), e);
        }
    }

    private static String extractParam(String url, String paramName) {
        String pattern = paramName + "=";
        int start = url.indexOf(pattern);
        if (start == -1) return null;
        start += pattern.length();
        int end = url.indexOf('&', start);
        String val = (end != -1) ? url.substring(start, end) : url.substring(start);
        try {
            return java.net.URLDecoder.decode(val, StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            return val;
        }
    }

    private String exchangeForLongLivedInstagramToken(String shortLivedToken, String clientSecret) {
        if (shortLivedToken == null || shortLivedToken.isBlank() || clientSecret == null || clientSecret.isBlank()) {
            return shortLivedToken;
        }
        try {
            String url = "https://graph.instagram.com/access_token?grant_type=ig_exchange_token" +
                    "&client_secret=" + urlEncode(clientSecret) +
                    "&access_token=" + urlEncode(shortLivedToken);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                JsonObject json = JsonParser.parseString(resp.body()).getAsJsonObject();
                if (json.has("access_token")) {
                    return json.get("access_token").getAsString();
                }
            }
        } catch (Exception ignored) {
        }
        return shortLivedToken;
    }

    private String extractErrorMessage(String responseBody) {
        try {
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
            if (json.has("error_description")) {
                return json.get("error_description").getAsString();
            } else if (json.has("error")) {
                if (json.get("error").isJsonObject()) {
                    JsonObject errObj = json.getAsJsonObject("error");
                    if (errObj.has("message")) return errObj.get("message").getAsString();
                }
                return json.get("error").getAsString();
            } else if (json.has("message")) {
                return json.get("message").getAsString();
            }
        } catch (Exception ignored) {
        }
        return responseBody;
    }

    /**
     * Returns structured diagnostics describing configuration presence and official setup steps.
     */
    public PlatformConfigGuide getConfigGuide(Platform platform) {
        ConfigService.SafePlatformDiagnostics diag = configService.getPlatformDiagnostics(platform);
        boolean configured = diag.configured();

        List<String> missing = new java.util.ArrayList<>();
        if (!diag.clientIdInfo().present()) {
            if (platform == Platform.X) {
                missing.add("X_CLIENT_ID (or TWITTER_CLIENT_ID)");
            } else {
                missing.add(platform.getClientIdConfigKey().toUpperCase().replace('.', '_'));
            }
        }
        if (!diag.clientSecretInfo().present()) {
            if (platform == Platform.X) {
                missing.add("X_CLIENT_SECRET (or TWITTER_CLIENT_SECRET)");
            } else {
                missing.add(platform.getClientSecretConfigKey().toUpperCase().replace('.', '_'));
            }
        }

        String sourceDesc;
        if (configured) {
            sourceDesc = "Client ID: " + diag.clientIdInfo().sourceDescription() +
                    " | Secret: " + diag.clientSecretInfo().sourceDescription();
        } else {
            sourceDesc = diag.loadedEnvFilePath() != null ?
                    "Loaded from " + diag.loadedEnvFilePath() : "No .env file found";
        }

        return new PlatformConfigGuide(
                platform,
                configured,
                String.join(", ", missing),
                sourceDesc,
                diag.bearerTokenInfo().present(),
                platform.getDeveloperPortalUrl(),
                platform.getRequiredAccountType(),
                platform.getDefaultScopes(),
                diag.redirectUri(),
                diag.loadedEnvFilePath()
        );
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public record PlatformConfigGuide(
            Platform platform,
            boolean configured,
            String missingVariables,
            String configSourceDescription,
            boolean hasBearerToken,
            String developerPortalUrl,
            String requiredAccountType,
            String requiredScopes,
            String redirectUri,
            String envFilePath
    ) {}
}
