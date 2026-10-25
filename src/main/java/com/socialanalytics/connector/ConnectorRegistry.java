package com.socialanalytics.connector;

import com.socialanalytics.connector.demo.DemoDataConnector;
import com.socialanalytics.model.Platform;
import com.socialanalytics.service.ConfigService;

/**
 * Factory and registry to acquire platform connectors depending on
 * configured credentials and requested application mode (LIVE vs DEMO).
 */
public class ConnectorRegistry {
    private static final ConnectorRegistry INSTANCE = new ConnectorRegistry();
    private final ConfigService config = ConfigService.getInstance();

    private ConnectorRegistry() {
    }

    public static ConnectorRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Resolves the connector for a given platform.
     *
     * @param platform platform enum
     * @param forceDemo if true, guarantees returning DemoDataConnector
     * @return SocialMediaConnector instance
     */
    public SocialMediaConnector getConnector(Platform platform, boolean forceDemo) {
        return getConnector(platform, null, forceDemo);
    }

    /**
     * Resolves the connector for a given platform, using the provided account access token
     * if available, or falling back to environment config or demo generator.
     */
    public SocialMediaConnector getConnector(Platform platform, String accessToken, boolean forceDemo) {
        if (forceDemo) {
            return new DemoDataConnector(platform);
        }

        // 1. Resolve token: explicitly passed token takes precedence over static properties
        String token = (accessToken != null && !accessToken.isBlank()) ? accessToken.trim() : null;
        if (token == null) {
            switch (platform) {
                case INSTAGRAM:
                    token = config.get("instagram.access.token");
                    break;
                case FACEBOOK:
                    token = config.get("facebook.page.access.token");
                    break;
                case LINKEDIN:
                    token = config.get("linkedin.access.token");
                    break;
                case X:
                    token = config.get("x.bearer.token");
                    break;
            }
        }

        // 2. If token is present, instantiate official live platform connector
        if (token != null && !token.isBlank()) {
            switch (platform) {
                case INSTAGRAM:
                    return new InstagramConnector(token);
                case FACEBOOK:
                    return new FacebookConnector(token);
                case LINKEDIN:
                    return new LinkedInConnector(token);
                case X:
                    return new XConnector(token);
            }
        }

        // 3. If live connection requested without pre-set token, return unauthenticated live connector
        // (so that authenticate() can be called subsequently)
        switch (platform) {
            case INSTAGRAM:
                return new InstagramConnector();
            case FACEBOOK:
                return new FacebookConnector();
            case LINKEDIN:
                return new LinkedInConnector();
            case X:
                return new XConnector();
        }

        return new DemoDataConnector(platform);
    }
}
