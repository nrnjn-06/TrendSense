package com.socialanalytics;

import com.socialanalytics.model.Trend;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class UiVisualSnapshotTest {

    private static final String ARTIFACT_DIR = "C:/Users/HP/.gemini/antigravity/brain/f8fa6509-6053-462f-a120-6da1a8907f0c/";

    @BeforeAll
    public static void initJavaFX() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Platform already initialized
        }
    }

    private static void saveSnapshot(Scene scene, String filename) {
        try {
            WritableImage img = scene.snapshot(null);
            int width = (int) img.getWidth();
            int height = (int) img.getHeight();
            PixelReader reader = img.getPixelReader();
            BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    bi.setRGB(x, y, reader.getArgb(x, y));
                }
            }
            File outFile = new File(ARTIFACT_DIR + filename);
            ImageIO.write(bi, "PNG", outFile);
            System.out.println("[SNAPSHOT SAVED] " + outFile.getAbsolutePath());
        } catch (Exception e) {
            System.err.println("Failed to save snapshot " + filename + ": " + e.getMessage());
        }
    }

    @Test
    public void testLoginViewRenderingAndSnapshot() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        final Exception[] error = new Exception[1];

        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
                Parent root = loader.load();
                Scene scene = new Scene(root, 960, 680);
                scene.getStylesheets().add(getClass().getResource("/css/modern-dark.css").toExternalForm());
                root.applyCss();
                root.layout();

                saveSnapshot(scene, "login_light_theme.png");
                saveSnapshot(scene, "login_sage_green.png");
            } catch (Exception e) {
                error[0] = e;
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(15, TimeUnit.SECONDS));
        if (error[0] != null) throw error[0];
    }

    @Test
    public void testMainDashboard10TabsAndTrendingTopics() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        final Exception[] error = new Exception[1];

        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
                Parent root = loader.load();
                Scene scene = new Scene(root, 1280, 820);
                scene.getStylesheets().add(getClass().getResource("/css/modern-dark.css").toExternalForm());
                root.applyCss();
                root.layout();

                // 1. Verify TabPane has exactly 10 tabs and verify their clean labels
                TabPane tabPane = null;
                for (javafx.scene.Node node : root.getChildrenUnmodifiable()) {
                    if (node instanceof TabPane tp) {
                        tabPane = tp;
                        break;
                    }
                }
                assertNotNull(tabPane, "TabPane should exist in main.fxml");
                assertEquals(10, tabPane.getTabs().size(), "There must be exactly 10 navigation tabs");

                String[] expectedTabs = {
                        "Overview",
                        "Engagement",
                        "Trending Topics",
                        "Google Trends",
                        "Sentiment Analysis",
                        "Best Posting Time",
                        "Recommendations",
                        "Posts Explorer",
                        "Connect Accounts",
                        "Settings"
                };

                for (int i = 0; i < expectedTabs.length; i++) {
                    Tab t = tabPane.getTabs().get(i);
                    assertEquals(expectedTabs[i], t.getText(), "Tab " + (i + 1) + " must match expected label");
                    assertFalse(t.getText().contains("??"), "Tab label must not contain corrupted question marks");
                }

                // 2. Snapshot Overview
                saveSnapshot(scene, "overview_light_theme.png");
                saveSnapshot(scene, "overview_sage_green.png");

                // 3. Switch to Trending Topics Tab (Tab index 2)
                tabPane.getSelectionModel().select(2);
                root.applyCss();
                root.layout();
                saveSnapshot(scene, "trending_topics_light_theme.png");
                saveSnapshot(scene, "trending_topics_sage_green.png");

                // 4. Switch to Settings Tab (Tab index 9)
                tabPane.getSelectionModel().select(9);
                root.applyCss();
                root.layout();
                saveSnapshot(scene, "settings_light_theme.png");
                saveSnapshot(scene, "settings_sage_green.png");

            } catch (Exception e) {
                error[0] = e;
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(15, TimeUnit.SECONDS));
        if (error[0] != null) throw error[0];
    }

    @Test
    public void testScoreDetailsModalDialogRendering() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        final Exception[] error = new Exception[1];

        Platform.runLater(() -> {
            try {
                // Construct a sample Trend with realistic data
                Trend t = new Trend("leads", 1, 2, 370472);
                t.setTrendScore(55.3);
                t.setMentionScore(6.0);
                t.setEngagementScore(83.0);
                t.setGrowthScore(100.0);
                t.setRecencyScore(0.0);
                t.setHasGrowthBaseline(true);
                t.setGrowthPercentage(100.0);
                t.setStatus(Trend.TrendStatus.RISING);
                t.setExplanation("Score: 55.3 | (0.25×6) + (0.35×83) + (0.25×100) + (0.15×0) = 55.3/100 | Growth: +100.0% | Mentions: 2 | Avg Eng: 185236 | Status: Rising");

                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
                Parent root = loader.load();
                com.socialanalytics.javafx.controller.DashboardController controller = loader.getController();
                assertNotNull(controller);

                javafx.scene.control.DialogPane dp = controller.buildScoreDetailsDialogPane(t);
                assertNotNull(dp);
                assertTrue(dp.getContent() instanceof javafx.scene.control.ScrollPane, "Content must be wrapped in ScrollPane");

                Scene dialogScene = new Scene(dp, 720, 640);
                dialogScene.getStylesheets().add(getClass().getResource("/css/modern-dark.css").toExternalForm());
                dp.applyCss();
                dp.layout();

                saveSnapshot(dialogScene, "score_details_dialog.png");
                saveSnapshot(dialogScene, "score_details_dialog_sage_green.png");
            } catch (Exception e) {
                error[0] = e;
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(15, TimeUnit.SECONDS));
        if (error[0] != null) throw error[0];
    }

    @Test
    public void testScoreDetailsModalDialogRenderingForIndian() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        final Exception[] error = new Exception[1];

        Platform.runLater(() -> {
            try {
                // Topic "indian" exactly matching user screenshot
                Trend t = new Trend("indian", 15, 35, 752815);
                t.setTrendScore(67.3);
                t.setMentionScore(100.0);
                t.setEngagementScore(9.6);
                t.setGrowthScore(100.0);
                t.setRecencyScore(92.9);
                t.setHasGrowthBaseline(true);
                t.setGrowthPercentage(133.3);
                t.setStatus(Trend.TrendStatus.RISING);

                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
                Parent root = loader.load();
                com.socialanalytics.javafx.controller.DashboardController controller = loader.getController();
                assertNotNull(controller);

                javafx.scene.control.DialogPane dp = controller.buildScoreDetailsDialogPane(t);
                assertNotNull(dp);
                assertTrue(dp.getContent() instanceof javafx.scene.control.ScrollPane, "Content must be wrapped in ScrollPane");

                javafx.scene.control.ScrollPane sp = (javafx.scene.control.ScrollPane) dp.getContent();
                assertTrue(sp.isFitToWidth());
                assertEquals(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER, sp.getHbarPolicy());

                Scene dialogScene = new Scene(dp, 720, 640);
                dialogScene.getStylesheets().add(getClass().getResource("/css/modern-dark.css").toExternalForm());
                dp.applyCss();
                dp.layout();

                saveSnapshot(dialogScene, "score_details_dialog_indian_fixed.png");
            } catch (Exception e) {
                error[0] = e;
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(15, TimeUnit.SECONDS));
        if (error[0] != null) throw error[0];
    }

    @Test
    public void testModeSelectionRenderingAndSnapshot() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        final Exception[] error = new Exception[1];

        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/mode-selection.fxml"));
                Parent root = loader.load();
                Scene scene = new Scene(root, 980, 680);
                scene.getStylesheets().add(getClass().getResource("/css/modern-dark.css").toExternalForm());
                root.applyCss();
                root.layout();

                saveSnapshot(scene, "mode_selection_sage_green.png");
            } catch (Exception e) {
                error[0] = e;
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(15, TimeUnit.SECONDS));
        if (error[0] != null) throw error[0];
    }

    @Test
    public void testConnectDialogExpandedRenderingAndSnapshot() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        final Exception[] error = new Exception[1];

        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
                Parent root = loader.load();
                com.socialanalytics.javafx.controller.DashboardController controller = loader.getController();
                assertNotNull(controller);

                // Build OAuth Connection Dialog Pane with Developer Testing expanded
                javafx.scene.control.DialogPane dp = controller.buildOAuthConnectionDialogPane(
                        com.socialanalytics.model.Platform.X, true, () -> {}
                );
                assertNotNull(dp);

                Scene dialogScene = new Scene(dp, 560, 840);
                dialogScene.getStylesheets().add(getClass().getResource("/css/modern-dark.css").toExternalForm());
                dp.applyCss();
                dp.layout();

                if (dp.getContent() instanceof javafx.scene.control.ScrollPane sp) {
                    sp.applyCss();
                    sp.layout();
                    sp.setVvalue(1.0);
                }

                saveSnapshot(dialogScene, "connect_dialog_expanded_sage_green.png");

                // Also test rendering when X OAuth application is configured
                com.socialanalytics.service.ConfigService.getInstance().setProperty("X_CLIENT_ID", "mock_client_id_123");
                com.socialanalytics.service.ConfigService.getInstance().setProperty("X_CLIENT_SECRET", "mock_client_secret_456");
                try {
                    javafx.scene.control.DialogPane dpConfigured = controller.buildOAuthConnectionDialogPane(
                            com.socialanalytics.model.Platform.X, false, () -> {}
                    );
                    Scene sceneConfigured = new Scene(dpConfigured, 560, 620);
                    sceneConfigured.getStylesheets().add(getClass().getResource("/css/modern-dark.css").toExternalForm());
                    dpConfigured.applyCss();
                    dpConfigured.layout();
                    saveSnapshot(sceneConfigured, "connect_dialog_x_configured_sage_green.png");
                } finally {
                    com.socialanalytics.service.ConfigService.getInstance().setProperty("X_CLIENT_ID", "");
                    com.socialanalytics.service.ConfigService.getInstance().setProperty("X_CLIENT_SECRET", "");
                }
            } catch (Exception e) {
                error[0] = e;
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(15, TimeUnit.SECONDS));
        if (error[0] != null) throw error[0];
    }
}
