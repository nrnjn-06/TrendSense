package com.socialanalytics.connector;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.socialanalytics.exception.ApiException;
import com.socialanalytics.exception.AuthenticationException;
import com.socialanalytics.model.Platform;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Abstract base connector encapsulating standard HTTP client interactions,
 * JSON handling via Gson, and unified error handling across social platforms.
 */
public abstract class AbstractSocialMediaConnector implements SocialMediaConnector {
    protected final Platform platform;
    protected final HttpClient httpClient;
    protected final Gson gson;
    protected String accessToken;

    public AbstractSocialMediaConnector(Platform platform) {
        this.platform = platform;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.gson = new Gson();
    }

    @Override
    public Platform getPlatform() {
        return platform;
    }

    @Override
    public boolean isDemo() {
        return false;
    }

    @Override
    public void disconnect(String accountId) {
        this.accessToken = null;
    }

    /**
     * Executes an authenticated GET request with the specified URL and Authorization header.
     */
    protected JsonObject executeGet(String url, String bearerToken) throws ApiException {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("Accept", "application/json");

            if (bearerToken != null && !bearerToken.isBlank()) {
                builder.header("Authorization", "Bearer " + bearerToken);
            }

            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            int statusCode = response.statusCode();

            if (statusCode == 401) {
                throw new AuthenticationException("[" + platform.getDisplayName() + "] Authentication failed: Invalid or expired access token.");
            } else if (statusCode == 403) {
                throw new ApiException(platform.getDisplayName(), statusCode,
                        "Permission denied. The connected account lacks required API permissions or developer approval.");
            } else if (statusCode == 429) {
                throw new ApiException(platform.getDisplayName(), statusCode,
                        "Rate limit exceeded. Platform API request quota reached.");
            } else if (statusCode >= 400) {
                throw new ApiException(platform.getDisplayName(), statusCode,
                        "API request error: " + response.body());
            }

            JsonElement parsed = JsonParser.parseString(response.body());
            if (parsed.isJsonObject()) {
                return parsed.getAsJsonObject();
            }
            JsonObject wrap = new JsonObject();
            wrap.add("data", parsed);
            return wrap;
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException(platform.getDisplayName(), "Network error connecting to official API: " + e.getMessage(), e);
        }
    }
}
