package com.socialanalytics;

import com.socialanalytics.model.AnalyticsSummary;
import com.socialanalytics.model.AppMode;
import com.socialanalytics.model.Platform;
import com.socialanalytics.service.AnalyticsService;
import com.socialanalytics.service.ConfigService;
import com.socialanalytics.service.OAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies strict data isolation between LIVE mode and DEMO mode,
 * ensuring Live Mode opens with an accurate empty state and zero simulated data leak,
 * and verifying OAuth credential reuse for Meta/Instagram.
 */
public class AppModeSeparationTest {

    private AnalyticsService analyticsService;
    private OAuthService oAuthService;

    @BeforeEach
    void setUp() {
        analyticsService = AnalyticsService.getInstance();
        analyticsService.invalidateCache();
        oAuthService = OAuthService.getInstance();
    }

    @Test
    @DisplayName("Live Mode defaults to clean empty state when no social accounts are connected")
    void testLiveModeEmptyState() {
        Long nonExistentUserId = 999999L;

        AnalyticsSummary summary = analyticsService.runAnalyticsForUser(
                nonExistentUserId,
                null,
                AppMode.LIVE,
                null
        );

        assertNotNull(summary);
        assertEquals(0, summary.getTotalPosts(), "Live mode without accounts must return exactly 0 posts");
        assertEquals(0, summary.getTotalInteractions(), "Live mode without accounts must return 0 engagement");
        assertFalse(summary.isLiveDataAvailable(), "Live data flag must be false when no accounts exist");
        assertFalse(summary.isDemoData(), "Empty live mode must NOT be flagged as demo data");
        assertTrue(summary.getDataSourceDescription().contains("No social accounts connected yet"),
                "Must display standard empty state prompt");
    }

    @Test
    @DisplayName("Demo Mode returns simulated illustrative data without modifying real accounts")
    void testDemoModeSimulatedData() {
        AnalyticsSummary summary = analyticsService.runAnalyticsForUser(
                1L,
                null,
                AppMode.DEMO,
                null
        );

        assertNotNull(summary);
        assertTrue(summary.getTotalPosts() > 0, "Demo mode must contain simulated posts");
        assertTrue(summary.isDemoData(), "Demo mode must be explicitly flagged as demo data");
        assertFalse(summary.isLiveDataAvailable(), "Demo mode must never claim to be live data");
        assertTrue(summary.getDataSourceDescription().contains("DEMO MODE"),
                "Description must clearly state Demo Mode");
    }

    @Test
    @DisplayName("Switching between LIVE and DEMO mode properly invalidates cache and preserves boundaries")
    void testModeSwitchingIsolation() {
        Long testUserId = 888888L;

        // 1. Run Live Mode (empty)
        AnalyticsSummary liveSummary = analyticsService.runAnalyticsForUser(testUserId, null, AppMode.LIVE, null);
        assertEquals(0, liveSummary.getTotalPosts());

        // 2. Switch to Demo Mode
        AnalyticsSummary demoSummary = analyticsService.runAnalyticsForUser(testUserId, null, AppMode.DEMO, null);
        assertTrue(demoSummary.getTotalPosts() > 0);
        assertTrue(demoSummary.isDemoData());

        // 3. Switch back to Live Mode
        AnalyticsSummary liveAgain = analyticsService.runAnalyticsForUser(testUserId, null, AppMode.LIVE, null);
        assertEquals(0, liveAgain.getTotalPosts(), "Returning to Live mode must return empty state, never demo data");
        assertFalse(liveAgain.isDemoData());
    }

    @Test
    @DisplayName("Instagram OAuth credentials resolution works correctly")
    void testInstagramMetaCredentialReuse() {
        com.socialanalytics.service.ConfigService config = com.socialanalytics.service.ConfigService.getInstance();
        boolean setMock = false;
        if (oAuthService.getClientId(Platform.INSTAGRAM).isBlank()) {
            config.setProperty("META_APP_ID", "mock_meta_app_id_123");
            config.setProperty("META_APP_SECRET", "mock_meta_app_secret_456");
            setMock = true;
        }
        try {
            String igId = oAuthService.getClientId(Platform.INSTAGRAM);
            assertFalse(igId.isBlank(), "Instagram Client ID must be configured or resolved via Meta fallback");
            assertFalse(oAuthService.getClientSecret(Platform.INSTAGRAM).isBlank(), "Instagram Secret must be configured or resolved via Meta fallback");
            assertTrue(oAuthService.isConfigured(Platform.INSTAGRAM));
        } finally {
            if (setMock) {
                config.setProperty("META_APP_ID", "");
                config.setProperty("META_APP_SECRET", "");
            }
        }
    }
}
