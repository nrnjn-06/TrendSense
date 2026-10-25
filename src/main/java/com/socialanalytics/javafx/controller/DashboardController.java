package com.socialanalytics.javafx.controller;

import com.socialanalytics.analytics.BestTimeAnalyzer;
import com.socialanalytics.analytics.MultithreadedAnalyticsEngine;
import com.socialanalytics.dao.PostDAO;
import com.socialanalytics.dao.impl.JdbcPostDAO;
import com.socialanalytics.javafx.ui.Toast;
import com.socialanalytics.javafx.ui.ViewNavigator;
import com.socialanalytics.model.AnalyticsSummary;
import com.socialanalytics.model.Comment;
import com.socialanalytics.model.Engagement;
import com.socialanalytics.model.Hashtag;
import com.socialanalytics.model.Platform;
import com.socialanalytics.model.Post;
import com.socialanalytics.model.Recommendation;
import com.socialanalytics.model.SentimentResult;
import com.socialanalytics.model.SocialAccount;
import com.socialanalytics.model.Trend;
import com.socialanalytics.model.User;
import com.socialanalytics.model.AppMode;
import com.socialanalytics.model.GoogleTrendItem;
import com.socialanalytics.service.AnalyticsService;
import com.socialanalytics.service.AuthService;
import com.socialanalytics.service.ConfigService;
import com.socialanalytics.service.GoogleTrendsService;
import com.socialanalytics.service.OAuthService;
import com.socialanalytics.service.SocialAccountService;
import com.socialanalytics.util.ReportExporter;
import java.io.File;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.awt.Desktop;
import java.net.URI;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Primary Dashboard Controller managing overview KPI metrics,
 * multithreaded background analytics, interactive charts, and account syncing.
 */
public class DashboardController {

    // Top Header & Mode Indicators
    @FXML private Label userGreetingLabel;
    @FXML private Label modeBadgeLabel;
    @FXML private Label demoNoticeLabel;
    @FXML private Label noticeBadgeLabel;
    @FXML private Button bannerConnectBtn;
    @FXML private Button bannerSyncBtn;
    @FXML private HBox demoNoticeBox;
    @FXML private ComboBox<String> platformFilterCombo;
    @FXML private ProgressBar analysisProgressBar;
    @FXML private Label analysisStatusLabel;
    @FXML private Button refreshButton;
    @FXML private Button modeSwitchBtn;
    @FXML private TabPane mainTabPane;

    // Empty State & Dashboard Content Containers
    @FXML private VBox welcomeEmptyStateBox;
    @FXML private VBox dashboardContentBox;

    // Overview KPIs
    @FXML private Label kpiTotalPosts;
    @FXML private Label kpiTotalEngagement;
    @FXML private Label kpiEngagementRate;
    @FXML private Label kpiEngagementRateSub;
    @FXML private Label kpiDominantSentiment;
    @FXML private Label kpiBestTime;
    @FXML private Label topPostContentLabel;
    @FXML private Label topPostStatsLabel;

    // Engagement Tab Summary Cards
    @FXML private Label engTotalLikesLabel;
    @FXML private Label engTotalCommentsLabel;
    @FXML private Label engTotalSharesLabel;
    @FXML private Label engTotalViewsLabel;
    @FXML private Label engTotalInteractionsLabel;

    // Engagement Tab Top Posts Table
    @FXML private BarChart<String, Number> platformEngagementChart;
    @FXML private TableView<Post> topPostsTable;
    @FXML private TableColumn<Post, String> colTopPlatform;
    @FXML private TableColumn<Post, String> colTopType;
    @FXML private TableColumn<Post, String> colTopContent;
    @FXML private TableColumn<Post, String> colTopLikes;
    @FXML private TableColumn<Post, String> colTopComments;
    @FXML private TableColumn<Post, String> colTopShares;
    @FXML private TableColumn<Post, String> colTopViews;
    @FXML private TableColumn<Post, String> colTopTotal;
    @FXML private TableColumn<Post, Void> colTopAction;

    // Trends Tab
    @FXML private BarChart<Number, String> trendScoreChart;
    @FXML private TableView<Trend> trendsTable;
    @FXML private TableColumn<Trend, String> colTrendTopic;
    @FXML private TableColumn<Trend, String> colTrendScore;
    @FXML private TableColumn<Trend, String> colTrendGrowth;
    @FXML private TableColumn<Trend, String> colTrendMentions;
    @FXML private TableColumn<Trend, String> colTrendStatus;
    @FXML private TableColumn<Trend, String> colTrendExplanation;

    // Google Trends Tab
    @FXML private ComboBox<String> googleTrendsRegionCombo;
    @FXML private TableView<GoogleTrendItem> googleTrendsTable;
    @FXML private TableColumn<GoogleTrendItem, String> colGTRank;
    @FXML private TableColumn<GoogleTrendItem, String> colGTTopic;
    @FXML private TableColumn<GoogleTrendItem, String> colGTTraffic;
    @FXML private TableColumn<GoogleTrendItem, String> colGTNews;
    @FXML private TableColumn<GoogleTrendItem, String> colGTSource;
    @FXML private TableColumn<GoogleTrendItem, String> colGTPubDate;

    // Sentiment Tab
    @FXML private PieChart sentimentPieChart;
    @FXML private Label sentimentPositiveLabel;
    @FXML private Label sentimentNeutralLabel;
    @FXML private Label sentimentNegativeLabel;
    @FXML private Label sentimentScoreLabel;
    @FXML private Label sentimentCorpusLabel;
    @FXML private ListView<String> topPositiveWordsList;
    @FXML private ListView<String> topNegativeWordsList;

    // Best Time Tab
    @FXML private BarChart<String, Number> bestTimeChart;
    @FXML private Label peakWindowLabel;
    @FXML private Label peakWindowStatsLabel;

    // Recommendations Tab
    @FXML private VBox recommendationsContainer;

    // Posts Explorer Tab
    @FXML private TableView<Post> allPostsTable;
    @FXML private TableColumn<Post, String> colAllId;
    @FXML private TableColumn<Post, String> colAllPlatform;
    @FXML private TableColumn<Post, String> colAllContent;
    @FXML private TableColumn<Post, String> colAllType;
    @FXML private TableColumn<Post, String> colAllTimestamp;
    @FXML private TableColumn<Post, String> colAllLikes;
    @FXML private TableColumn<Post, String> colAllComments;
    @FXML private TableColumn<Post, String> colAllShares;
    @FXML private TableColumn<Post, String> colAllViews;
    @FXML private TableColumn<Post, String> colAllInteractions;
    @FXML private TableColumn<Post, Void> colAllAction;
    @FXML private TextField postSearchField;
    @FXML private ComboBox<String> postFilterPlatformCombo;
    @FXML private Label postCountStatusLabel;

    private List<Post> cachedExplorerPosts = new java.util.ArrayList<>();

    // Accounts Tab
    @FXML private Label igStatusLabel;
    @FXML private Label igAccountLabel;
    @FXML private Label igSyncedLabel;
    @FXML private Label igReasonLabel;
    @FXML private Button igSyncBtn;
    @FXML private Button igDisconnectBtn;
    @FXML private Button igConnectBtn;
    @FXML private Button igDemoBtn;

    @FXML private Label fbStatusLabel;
    @FXML private Label fbAccountLabel;
    @FXML private Label fbSyncedLabel;
    @FXML private Label fbReasonLabel;
    @FXML private Button fbSyncBtn;
    @FXML private Button fbDisconnectBtn;
    @FXML private Button fbConnectBtn;
    @FXML private Button fbDemoBtn;

    @FXML private Label liStatusLabel;
    @FXML private Label liAccountLabel;
    @FXML private Label liSyncedLabel;
    @FXML private Label liReasonLabel;
    @FXML private Button liSyncBtn;
    @FXML private Button liDisconnectBtn;
    @FXML private Button liConnectBtn;
    @FXML private Button liDemoBtn;

    @FXML private Label xStatusLabel;
    @FXML private Label xAccountLabel;
    @FXML private Label xSyncedLabel;
    @FXML private Label xReasonLabel;
    @FXML private Button xSyncBtn;
    @FXML private Button xDisconnectBtn;
    @FXML private Button xConnectBtn;
    @FXML private Button xDemoBtn;

    // Settings Tab
    @FXML private TextField dbUrlField;
    @FXML private TextField dbUsernameField;
    @FXML private PasswordField dbPasswordField;
    @FXML private TextField weightMentionField;
    @FXML private TextField weightEngagementField;
    @FXML private TextField weightGrowthField;
    @FXML private TextField weightRecencyField;

    private final AuthService authService = AuthService.getInstance();
    private final SocialAccountService socialAccountService = SocialAccountService.getInstance();
    private final AnalyticsService analyticsService = AnalyticsService.getInstance();
    private final OAuthService oAuthService = OAuthService.getInstance();
    private final ConfigService configService = ConfigService.getInstance();
    private final GoogleTrendsService googleTrendsService = GoogleTrendsService.getInstance();
    private final PostDAO postDAO = new JdbcPostDAO();
    private final NumberFormat numFmt = NumberFormat.getNumberInstance(Locale.US);

    private User currentUser;
    private AnalyticsSummary currentSummary;
    private AppMode currentAppMode = AppMode.LIVE;
    private static AppMode preselectedMode = null;

    public static void setPreselectedMode(AppMode mode) {
        preselectedMode = mode;
    }

    @FXML
    public void initialize() {
        if (preselectedMode != null) {
            this.currentAppMode = preselectedMode;
            preselectedMode = null;
        }

        currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            userGreetingLabel.setText("Analyst Workspace");
        } else {
            userGreetingLabel.setText("Analyst: " + currentUser.getUsername());
        }

        // Setup Platform Filter Dropdown
        platformFilterCombo.setItems(FXCollections.observableArrayList(
                "All Platforms", "Instagram", "Facebook", "LinkedIn", "X"
        ));
        platformFilterCombo.setValue("All Platforms");
        platformFilterCombo.setOnAction(e -> triggerRefresh(false));

        // Setup Google Trends Region Dropdown
        if (googleTrendsRegionCombo != null) {
            googleTrendsRegionCombo.setItems(FXCollections.observableArrayList(
                    "India (IN)", "United States (US)", "United Kingdom (GB)", "Canada (CA)", "Australia (AU)"
            ));
            googleTrendsRegionCombo.setValue("India (IN)");
            googleTrendsRegionCombo.setOnAction(e -> loadGoogleTrends());
        }

        setupTables();
        loadSettingsValues();
        triggerRefresh(false);
        loadGoogleTrends();
    }

    private void setupTables() {
        // Top Posts Table columns
        colTopPlatform.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPlatform().getDisplayName()));
        if (colTopType != null) {
            colTopType.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getContentType() != null ? data.getValue().getContentType() : "POST"));
        }

        // Caption column: clean newlines for single-line cell, attach multiline Tooltip, prevent clipping
        colTopContent.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getContent() != null ? data.getValue().getContent() : ""));
        colTopContent.setCellFactory(col -> new TableCell<Post, String>() {
            private final Tooltip tooltip = new Tooltip();
            {
                tooltip.setWrapText(true);
                tooltip.setMaxWidth(480);
                tooltip.setStyle("-fx-font-size: 12px; -fx-background-color: #26372D; -fx-text-fill: #FFFFFF; -fx-padding: 8px 12px; -fx-background-radius: 6px;");
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setTooltip(null);
                } else {
                    String singleLine = item.replace("\r\n", " ").replace('\n', ' ').replace('\r', ' ').trim();
                    setText(singleLine);
                    setTextOverrun(OverrunStyle.ELLIPSIS);
                    tooltip.setText(item);
                    setTooltip(tooltip);
                }
            }
        });

        // Responsive width binding: expand colTopContent dynamically to absorb all remaining table width
        topPostsTable.widthProperty().addListener((obs, oldVal, newVal) -> {
            double tableWidth = newVal.doubleValue();
            if (tableWidth > 500) {
                double fixed = colTopPlatform.getWidth() + (colTopType != null ? colTopType.getWidth() : 0) +
                        colTopLikes.getWidth() + colTopComments.getWidth() +
                        colTopShares.getWidth() + (colTopViews != null ? colTopViews.getWidth() : 0) +
                        colTopTotal.getWidth() + (colTopAction != null ? colTopAction.getWidth() : 0);
                double remaining = tableWidth - fixed - 25;
                if (remaining > 180) {
                    colTopContent.setPrefWidth(remaining);
                }
            }
        });

        colTopLikes.setCellValueFactory(data -> new SimpleStringProperty(numFmt.format(data.getValue().getLikes())));
        colTopComments.setCellValueFactory(data -> new SimpleStringProperty(numFmt.format(data.getValue().getCommentsCount())));
        colTopShares.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().isSharesAvailable() ? numFmt.format(data.getValue().getShares()) : "N/A"));
        if (colTopViews != null) {
            colTopViews.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().isViewsAvailable() ? numFmt.format(data.getValue().getViews()) : "N/A"));
        }
        colTopTotal.setCellValueFactory(data -> new SimpleStringProperty(numFmt.format(data.getValue().getTotalInteractions())));

        // Action Column: "Inspect" button
        if (colTopAction != null) {
            colTopAction.setCellFactory(col -> new TableCell<>() {
                private final Button btn = new Button("Inspect");
                {
                    btn.setStyle("-fx-background-color: #397653; -fx-text-fill: #FFFFFF; -fx-font-size: 11px; -fx-font-weight: 600; -fx-padding: 3 9; -fx-background-radius: 4px; -fx-cursor: hand;");
                    btn.setOnAction(e -> {
                        Post p = getTableView().getItems().get(getIndex());
                        showPostDetailsDialog(p);
                    });
                }
                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : btn);
                }
            });
        }

        // Double-click row handler to show post details
        topPostsTable.setRowFactory(tv -> {
            TableRow<Post> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    showPostDetailsDialog(row.getItem());
                }
            });
            return row;
        });

        // Trends Table columns
        colTrendTopic.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTopic()));
        colTrendScore.setCellValueFactory(data -> new SimpleStringProperty(String.format("%.1f/100", data.getValue().getTrendScore())));
        colTrendGrowth.setCellValueFactory(data -> {
            Trend t = data.getValue();
            if (!t.isHasGrowthBaseline() || t.getStatus() == Trend.TrendStatus.EMERGING) {
                return new SimpleStringProperty("★ Emerging");
            } else {
                return new SimpleStringProperty(String.format("%+.1f%%", t.getGrowthPercentage()));
            }
        });
        colTrendMentions.setCellValueFactory(data -> new SimpleStringProperty(numFmt.format(data.getValue().getCurrentMentions())));
        colTrendStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus().getSymbol() + " " + data.getValue().getStatus().getLabel()));
        colTrendStatus.setCellFactory(col -> new TableCell<Trend, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.contains("Rising")) {
                        setStyle("-fx-text-fill: #3D8056; -fx-font-weight: bold;");
                    } else if (item.contains("Emerging")) {
                        setStyle("-fx-text-fill: #82966B; -fx-font-weight: bold;");
                    } else if (item.contains("Falling")) {
                        setStyle("-fx-text-fill: #B65C52; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #657568;");
                    }
                }
            }
        });

        // Score Calculation & Weight Justification column with concise summary and View Score Details button
        colTrendExplanation.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getExplanation()));
        colTrendExplanation.setCellFactory(col -> new TableCell<Trend, String>() {
            private final HBox container = new HBox(10);
            private final Label summaryLabel = new Label();
            private final Button detailsBtn = new Button("View Score Details");

            {
                container.setAlignment(Pos.CENTER_LEFT);
                summaryLabel.setMaxWidth(400);
                summaryLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
                summaryLabel.setStyle("-fx-text-fill: #657568; -fx-font-size: 11px;");
                HBox.setHgrow(summaryLabel, Priority.ALWAYS);

                detailsBtn.setStyle("-fx-background-color: #397653; -fx-text-fill: #FFFFFF; -fx-font-size: 11px; -fx-font-weight: 600; -fx-padding: 3 9; -fx-background-radius: 4px; -fx-cursor: hand;");
                detailsBtn.setOnAction(e -> {
                    if (getIndex() >= 0 && getIndex() < getTableView().getItems().size()) {
                        Trend t = getTableView().getItems().get(getIndex());
                        if (t != null) {
                            showTrendScoreDetailsDialog(t);
                        }
                    }
                });

                container.getChildren().addAll(summaryLabel, detailsBtn);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Trend t = getTableView().getItems().get(getIndex());
                    if (t != null) {
                        String summary = String.format("Score: %.1f | (0.25×%.0f) + (0.35×%.0f) + (0.25×%.0f) + (0.15×%.0f) | %s",
                                t.getTrendScore(), t.getMentionScore(), t.getEngagementScore(), t.getGrowthScore(), t.getRecencyScore(),
                                t.getStatus().getLabel());
                        summaryLabel.setText(summary);
                        setGraphic(container);
                        setText(null);
                    } else {
                        setGraphic(null);
                        setText(null);
                    }
                }
            }
        });

        // Responsive width binding: expand colTrendExplanation dynamically to absorb 100% of remaining width
        trendsTable.widthProperty().addListener((obs, oldVal, newVal) -> {
            double tableWidth = newVal.doubleValue();
            if (tableWidth > 600) {
                double fixed = colTrendTopic.getWidth() + colTrendScore.getWidth() +
                        colTrendGrowth.getWidth() + colTrendMentions.getWidth() +
                        colTrendStatus.getWidth();
                double remaining = tableWidth - fixed - 25;
                if (remaining > 220) {
                    colTrendExplanation.setPrefWidth(remaining);
                }
            }
        });

        // Double-click row handler to open score details dialog
        trendsTable.setRowFactory(tv -> {
            TableRow<Trend> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    Trend t = row.getItem();
                    if (t != null) {
                        showTrendScoreDetailsDialog(t);
                    }
                }
            });
            return row;
        });

        // Google Trends Table columns
        if (colGTRank != null) {
            colGTRank.setCellValueFactory(data -> new SimpleStringProperty("#" + data.getValue().getRank()));
            colGTTopic.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTopic()));
            colGTTraffic.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getApproxTraffic()));
            colGTNews.setCellValueFactory(data -> new SimpleStringProperty(truncate(data.getValue().getNewsTitle(), 80)));
            colGTSource.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNewsSource()));
            colGTPubDate.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPubDate()));
        }

        // All Posts Table columns
        colAllId.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getId()));
        colAllPlatform.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPlatform().getDisplayName()));
        colAllContent.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getContent() != null ? data.getValue().getContent() : ""));
        colAllContent.setCellFactory(col -> new TableCell<Post, String>() {
            private final Tooltip tooltip = new Tooltip();
            {
                tooltip.setWrapText(true);
                tooltip.setMaxWidth(480);
                tooltip.setStyle("-fx-font-size: 12px; -fx-background-color: #26372D; -fx-text-fill: #FFFFFF; -fx-padding: 8px 12px; -fx-background-radius: 6px;");
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setTooltip(null);
                } else {
                    String singleLine = item.replace("\r\n", " ").replace('\n', ' ').replace('\r', ' ').trim();
                    setText(singleLine);
                    setTextOverrun(OverrunStyle.ELLIPSIS);
                    tooltip.setText(item);
                    setTooltip(tooltip);
                }
            }
        });

        // Responsive width binding for allPostsTable
        allPostsTable.widthProperty().addListener((obs, oldVal, newVal) -> {
            double tableWidth = newVal.doubleValue();
            if (tableWidth > 600) {
                double fixed = colAllId.getWidth() + colAllPlatform.getWidth() +
                        colAllType.getWidth() + colAllTimestamp.getWidth() +
                        (colAllLikes != null ? colAllLikes.getWidth() : 0) +
                        (colAllComments != null ? colAllComments.getWidth() : 0) +
                        (colAllShares != null ? colAllShares.getWidth() : 0) +
                        (colAllViews != null ? colAllViews.getWidth() : 0) +
                        colAllInteractions.getWidth() +
                        (colAllAction != null ? colAllAction.getWidth() : 0);
                double remaining = tableWidth - fixed - 25;
                if (remaining > 180) {
                    colAllContent.setPrefWidth(remaining);
                }
            }
        });
        colAllType.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getContentType() != null ? data.getValue().getContentType() : "POST"));
        colAllTimestamp.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTimestamp() != null ? data.getValue().getTimestamp().toString().replace('T', ' ') : "N/A"));
        if (colAllLikes != null) {
            colAllLikes.setCellValueFactory(data -> new SimpleStringProperty(numFmt.format(data.getValue().getLikes())));
        }
        if (colAllComments != null) {
            colAllComments.setCellValueFactory(data -> new SimpleStringProperty(numFmt.format(data.getValue().getCommentsCount())));
        }
        if (colAllShares != null) {
            colAllShares.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().isSharesAvailable() ? numFmt.format(data.getValue().getShares()) : "N/A"));
        }
        if (colAllViews != null) {
            colAllViews.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().isViewsAvailable() ? numFmt.format(data.getValue().getViews()) : "N/A"));
        }
        colAllInteractions.setCellValueFactory(data -> new SimpleStringProperty(numFmt.format(data.getValue().getTotalInteractions())));

        // Action Column: "Inspect" button
        if (colAllAction != null) {
            colAllAction.setCellFactory(col -> new TableCell<>() {
                private final Button btn = new Button("Inspect");
                {
                    btn.setStyle("-fx-background-color: #397653; -fx-text-fill: #FFFFFF; -fx-font-size: 11px; -fx-font-weight: 600; -fx-padding: 3 9; -fx-background-radius: 4px; -fx-cursor: hand;");
                    btn.setOnAction(e -> {
                        Post p = getTableView().getItems().get(getIndex());
                        showPostDetailsDialog(p);
                    });
                }
                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : btn);
                }
            });
        }

        // Double-click row handler to show post details
        allPostsTable.setRowFactory(tv -> {
            TableRow<Post> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    showPostDetailsDialog(row.getItem());
                }
            });
            return row;
        });

        // Setup Posts Explorer platform filter and search listeners
        if (postFilterPlatformCombo != null) {
            postFilterPlatformCombo.setItems(FXCollections.observableArrayList(
                    "All Platforms", "Instagram", "Facebook", "LinkedIn", "X"
            ));
            postFilterPlatformCombo.setValue("All Platforms");
            postFilterPlatformCombo.setOnAction(e -> applyPostExplorerFilters());
        }
        if (postSearchField != null) {
            postSearchField.textProperty().addListener((obs, oldVal, newVal) -> applyPostExplorerFilters());
        }
    }

    @FXML
    public void handleRefreshAction(ActionEvent event) {
        triggerRefresh(true);
    }

    @FXML
    public void handleRefreshGoogleTrends(ActionEvent event) {
        loadGoogleTrends();
    }

    private void loadGoogleTrends() {
        if (googleTrendsTable == null) return;
        String selected = (googleTrendsRegionCombo != null && googleTrendsRegionCombo.getValue() != null)
                ? googleTrendsRegionCombo.getValue() : "India (IN)";
        String geo = "IN";
        if (selected.contains("US")) geo = "US";
        else if (selected.contains("GB")) geo = "GB";
        else if (selected.contains("CA")) geo = "CA";
        else if (selected.contains("AU")) geo = "AU";

        final String finalGeo = geo;
        Task<List<GoogleTrendItem>> gtTask = new Task<>() {
            @Override
            protected List<GoogleTrendItem> call() {
                return googleTrendsService.fetchDailyTrends(finalGeo);
            }
        };

        gtTask.setOnSucceeded(e -> {
            List<GoogleTrendItem> items = gtTask.getValue();
            if (items != null) {
                googleTrendsTable.setItems(FXCollections.observableArrayList(items));
            }
        });

        gtTask.setOnFailed(e -> {
            List<GoogleTrendItem> fallback = googleTrendsService.getFallbackTrends(finalGeo);
            googleTrendsTable.setItems(FXCollections.observableArrayList(fallback));
        });

        Thread gtThread = new Thread(gtTask, "GoogleTrendsWorkerThread");
        gtThread.setDaemon(true);
        gtThread.start();
    }

    @FXML
    public void handleExportReport(ActionEvent event) {
        if (currentSummary == null) {
            Toast.show(ViewNavigator.getPrimaryStage(), "Run analytics first before exporting.", true);
            return;
        }
        try {
            Long userId = (currentUser != null && currentUser.getId() != null) ? currentUser.getId() : 1L;
            List<Post> posts = analyticsService.getPostsForUser(userId, currentAppMode);
            File htmlReport = ReportExporter.exportHtmlReport(currentSummary, posts, "reports");
            File csvReport = ReportExporter.exportCsvReport(posts, "reports");

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Executive Report Exported");
            alert.setHeaderText("✓ Analytics Report Exported Successfully!");
            alert.setContentText(
                    "HTML Executive Report:\n" + htmlReport.getAbsolutePath() + "\n\n" +
                    "CSV Dataset File:\n" + csvReport.getAbsolutePath() + "\n\n" +
                    "Opening executive report in your default browser now..."
            );
            alert.show();

            if (java.awt.Desktop.isDesktopSupported() && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.OPEN)) {
                java.awt.Desktop.getDesktop().open(htmlReport);
            }
        } catch (Exception ex) {
            Toast.show(ViewNavigator.getPrimaryStage(), "Failed to export report: " + ex.getMessage(), true);
        }
    }

    @FXML
    public void handleToggleAppMode(ActionEvent event) {
        if (currentAppMode == AppMode.LIVE) {
            currentAppMode = AppMode.DEMO;
        } else {
            currentAppMode = AppMode.LIVE;
        }
        analyticsService.invalidateCache();
        triggerRefresh(false);
    }

    private void triggerRefresh(boolean forceSync) {
        Long userId = (currentUser != null && currentUser.getId() != null) ? currentUser.getId() : 1L;

        String filterVal = platformFilterCombo.getValue();
        Platform platformFilter = null;
        if (filterVal != null && !filterVal.equalsIgnoreCase("All Platforms")) {
            platformFilter = Platform.fromString(filterVal);
        }

        final Platform finalPlatformFilter = platformFilter;
        refreshButton.setDisable(true);
        analysisProgressBar.setVisible(true);

        Task<AnalyticsSummary> task = new Task<>() {
            @Override
            protected AnalyticsSummary call() {
                updateMessage("Starting multithreaded analysis...");
                updateProgress(0.05, 1.0);

                return analyticsService.runAnalyticsForUser(userId, finalPlatformFilter, currentAppMode, (progress, message) -> {
                    updateProgress(progress, 1.0);
                    updateMessage(message);
                });
            }
        };

        analysisProgressBar.progressProperty().bind(task.progressProperty());
        analysisStatusLabel.textProperty().bind(task.messageProperty());

        task.setOnSucceeded(e -> {
            currentSummary = task.getValue();
            populateDashboard(currentSummary);
            refreshButton.setDisable(false);
            analysisProgressBar.setVisible(false);
            analysisStatusLabel.textProperty().unbind();
            analysisStatusLabel.setText("Analysis updated.");
        });

        task.setOnFailed(e -> {
            refreshButton.setDisable(false);
            analysisProgressBar.setVisible(false);
            analysisStatusLabel.textProperty().unbind();
            analysisStatusLabel.setText("Analysis failed: " + task.getException().getMessage());
            Toast.show(ViewNavigator.getPrimaryStage(), "Error: " + task.getException().getMessage(), true);
        });

        Thread worker = new Thread(task, "DashboardAnalyticsThread");
        worker.setDaemon(true);
        worker.start();
    }

    private void populateDashboard(AnalyticsSummary s) {
        if (s == null) return;

        // Update mode toggle button
        if (modeSwitchBtn != null) {
            if (currentAppMode == AppMode.LIVE) {
                modeSwitchBtn.setText("Explore Demo Mode");
                modeSwitchBtn.setStyle("-fx-background-color: #DCE7DC; -fx-text-fill: #26372D; -fx-border-color: #C5D9C5; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-font-size: 11px; -fx-font-weight: 600; -fx-cursor: hand;");
            } else {
                modeSwitchBtn.setText("Return to Live Mode");
                modeSwitchBtn.setStyle("-fx-background-color: #DCE7DC; -fx-text-fill: #397653; -fx-border-color: #C5D9C5; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-font-size: 11px; -fx-font-weight: 600; -fx-cursor: hand;");
            }
        }

        // Overview Dashboard Content is always visible for both Live and Demo modes
        if (welcomeEmptyStateBox != null) {
            welcomeEmptyStateBox.setVisible(false);
            welcomeEmptyStateBox.setManaged(false);
        }
        if (dashboardContentBox != null) {
            dashboardContentBox.setVisible(true);
            dashboardContentBox.setManaged(true);
        }

        // 1. Mode Badge and Notice Banner
        Long userId = (currentUser != null && currentUser.getId() != null) ? currentUser.getId() : 1L;
        List<SocialAccount> userAccounts = socialAccountService.getAccountsForUser(userId);

        String selectedFilter = platformFilterCombo.getValue();
        Platform currentPlatform = (selectedFilter != null && !selectedFilter.equalsIgnoreCase("All Platforms"))
                ? Platform.fromString(selectedFilter) : null;

        demoNoticeBox.setVisible(true);
        demoNoticeBox.setManaged(true);

        if (s.isDemoData() || currentAppMode == AppMode.DEMO) {
            modeBadgeLabel.setText("DEMO MODE");
            modeBadgeLabel.getStyleClass().setAll("mode-badge-demo");

            demoNoticeBox.getStyleClass().setAll("notice-banner");
            if (noticeBadgeLabel != null) {
                noticeBadgeLabel.setText("DEMO MODE — SIMULATED DATA");
                noticeBadgeLabel.setStyle("-fx-text-fill: #B88738; -fx-font-weight: bold; -fx-font-size: 12px;");
            }

            demoNoticeLabel.getStyleClass().setAll("notice-banner-text");
            demoNoticeLabel.setText("Simulated multi-platform demo dataset active (Illustrative sample data for demonstration).");

            if (bannerConnectBtn != null) {
                bannerConnectBtn.setText("Connect Real Accounts");
                bannerConnectBtn.setStyle("-fx-background-color: #397653; -fx-text-fill: #FFFFFF; -fx-font-size: 11px; -fx-font-weight: 600; -fx-cursor: hand; -fx-background-radius: 6px;");
            }
        } else {
            // LIVE MODE: Distinctly separate connection status from data availability
            Optional<SocialAccount> filteredAcc = (currentPlatform != null)
                    ? userAccounts.stream().filter(a -> a.getPlatform() == currentPlatform && a.isConnected()).findFirst()
                    : userAccounts.stream().filter(SocialAccount::isConnected).findFirst();

            if (filteredAcc.isPresent()) {
                SocialAccount acc = filteredAcc.get();
                modeBadgeLabel.setText("LIVE: @" + acc.getUsername());
                modeBadgeLabel.getStyleClass().setAll("mode-badge-live");

                if (s.getTotalPosts() > 0) {
                    demoNoticeBox.getStyleClass().setAll("notice-banner-live");
                    if (noticeBadgeLabel != null) {
                        noticeBadgeLabel.setText("LIVE DATA");
                        noticeBadgeLabel.setStyle("-fx-text-fill: #3D8056; -fx-font-weight: bold; -fx-font-size: 12px;");
                    }
                    demoNoticeLabel.getStyleClass().setAll("notice-banner-live-text");
                    demoNoticeLabel.setText(s.getDataSourceDescription());
                } else {
                    demoNoticeBox.getStyleClass().setAll("notice-banner");
                    if (noticeBadgeLabel != null) {
                        noticeBadgeLabel.setText("CONNECTED — NO POSTS FOUND");
                        noticeBadgeLabel.setStyle("-fx-text-fill: #2D6042; -fx-font-weight: bold; -fx-font-size: 12px;");
                    }
                    demoNoticeLabel.getStyleClass().setAll("notice-banner-text");
                    demoNoticeLabel.setText(String.format("Account @%s is connected to %s, but has 0 published posts. Click Sync to pull new content or verify app permissions.",
                            acc.getUsername(), (currentPlatform != null ? currentPlatform.getDisplayName() : "platform")));
                }

                if (bannerConnectBtn != null) {
                    bannerConnectBtn.setText("Manage Account");
                    bannerConnectBtn.setStyle("-fx-background-color: #397653; -fx-text-fill: #FFFFFF; -fx-font-size: 11px; -fx-font-weight: 600; -fx-cursor: hand; -fx-background-radius: 6px;");
                }
            } else {
                // Not connected on selected platform
                String platName = currentPlatform != null ? currentPlatform.getDisplayName() : "All Platforms";
                modeBadgeLabel.setText("LIVE: " + platName + " (NOT CONNECTED)");
                modeBadgeLabel.getStyleClass().setAll("mode-badge-demo");

                demoNoticeBox.getStyleClass().setAll("notice-banner");
                if (noticeBadgeLabel != null) {
                    noticeBadgeLabel.setText("NOT CONNECTED");
                    noticeBadgeLabel.setStyle("-fx-text-fill: #657568; -fx-font-weight: bold; -fx-font-size: 12px;");
                }
                demoNoticeLabel.getStyleClass().setAll("notice-banner-text");
                demoNoticeLabel.setText("No " + platName + " account connected yet. Connect via official OAuth in Accounts tab to analyze real data.");

                if (bannerConnectBtn != null) {
                    bannerConnectBtn.setText("Connect Account");
                    bannerConnectBtn.setStyle("-fx-background-color: #397653; -fx-text-fill: #FFFFFF; -fx-font-size: 11px; -fx-font-weight: 600; -fx-cursor: hand; -fx-background-radius: 6px;");
                }
            }
        }

        // 2. Overview KPIs
        kpiTotalPosts.setText(numFmt.format(s.getTotalPosts()));
        kpiTotalEngagement.setText(numFmt.format(s.getTotalInteractions()));

        if (s.isEngagementRateAvailable() && s.getAverageEngagementRate() != null) {
            kpiEngagementRate.setText(String.format("%.2f%%", s.getAverageEngagementRate()));
            if (kpiEngagementRateSub != null) {
                String basis = s.getEngagementRateBasis();
                if ("IMPRESSIONS".equalsIgnoreCase(basis)) {
                    kpiEngagementRateSub.setText("Interactions / Total Impressions");
                } else if ("FOLLOWERS".equalsIgnoreCase(basis)) {
                    kpiEngagementRateSub.setText("Avg Interactions / Follower Baseline");
                } else {
                    kpiEngagementRateSub.setText("Interactions / Audience");
                }
            }
        } else {
            kpiEngagementRate.setText("N/A");
            if (kpiEngagementRateSub != null) {
                kpiEngagementRateSub.setText("Audience reach unavailable via API");
            }
        }

        kpiDominantSentiment.setText(s.getSentiment().getDominantSentiment());
        kpiBestTime.setText(s.getBestPostingTimeWindow());

        if (s.getTopPost() != null) {
            topPostContentLabel.setText("\"" + truncate(s.getTopPost().getContent(), 90) + "\"");
            String topShares = s.getTopPost().isSharesAvailable() ? numFmt.format(s.getTopPost().getShares()) : "N/A";
            topPostStatsLabel.setText(String.format("[%s] Likes: %s | Comments: %s | Shares: %s",
                    s.getTopPost().getPlatform().getDisplayName(),
                    numFmt.format(s.getTopPost().getLikes()),
                    numFmt.format(s.getTopPost().getCommentsCount()),
                    topShares));
        } else {
            topPostContentLabel.setText("No post data available");
            topPostStatsLabel.setText("");
        }

        // 3. Engagement Platform Chart & Tables - Explicit 4-Platform Support with Brand Colors
        platformEngagementChart.setAnimated(false);
        platformEngagementChart.getData().clear();

        CategoryAxis pXAxis = (CategoryAxis) platformEngagementChart.getXAxis();
        pXAxis.setAnimated(false);
        pXAxis.setCategories(FXCollections.observableArrayList(
                Platform.INSTAGRAM.getDisplayName(),
                Platform.FACEBOOK.getDisplayName(),
                Platform.LINKEDIN.getDisplayName(),
                Platform.X.getDisplayName()
        ));

        XYChart.Series<String, Number> seriesEng = new XYChart.Series<>();
        seriesEng.setName("Interactions");
        Map<Platform, Engagement.PlatformBreakdown> breakdowns = s.getEngagement().getPlatformBreakdowns();
        for (Platform p : List.of(Platform.INSTAGRAM, Platform.FACEBOOK, Platform.LINKEDIN, Platform.X)) {
            long count = (breakdowns != null && breakdowns.containsKey(p)) ? breakdowns.get(p).getTotalInteractions() : 0L;
            seriesEng.getData().add(new XYChart.Data<>(p.getDisplayName(), count));
        }
        platformEngagementChart.getData().add(seriesEng);

        // Apply platform brand colors and interactive tooltips to each bar node
        for (XYChart.Data<String, Number> d : seriesEng.getData()) {
            if (d.getNode() != null) {
                String brandColor = switch (d.getXValue()) {
                    case "Instagram" -> "#397653"; // Forest green
                    case "Facebook" -> "#82966B";  // Olive green
                    case "LinkedIn" -> "#91AD8B";  // Sage green
                    case "X" -> "#C6A96B";         // Muted gold
                    default -> "#26372D";          // Deep charcoal green
                };
                d.getNode().setStyle("-fx-bar-fill: " + brandColor + ";");
                Tooltip tooltip = new Tooltip(String.format("%s: %s total interactions", d.getXValue(), numFmt.format(d.getYValue().longValue())));
                tooltip.setStyle("-fx-font-size: 12px; -fx-background-color: #26372D; -fx-text-fill: #FFFFFF; -fx-padding: 6px 10px; -fx-background-radius: 4px;");
                Tooltip.install(d.getNode(), tooltip);
            }
        }

        topPostsTable.setItems(FXCollections.observableArrayList(s.getEngagement().getTopPosts()));

        // Populate Engagement Tab Summary Cards
        if (engTotalLikesLabel != null) {
            engTotalLikesLabel.setText(numFmt.format(s.getEngagement().getTotalLikes()));
        }
        if (engTotalCommentsLabel != null) {
            engTotalCommentsLabel.setText(numFmt.format(s.getEngagement().getTotalComments()));
        }
        if (engTotalSharesLabel != null) {
            if (s.getEngagement().isSharesAvailable() && s.getEngagement().getTotalShares() != null) {
                engTotalSharesLabel.setText(numFmt.format(s.getEngagement().getTotalShares()));
            } else {
                engTotalSharesLabel.setText("N/A");
            }
        }
        if (engTotalViewsLabel != null) {
            if (s.getEngagement().isViewsAvailable() && s.getEngagement().getTotalViews() != null) {
                engTotalViewsLabel.setText(numFmt.format(s.getEngagement().getTotalViews()));
            } else {
                engTotalViewsLabel.setText("N/A");
            }
        }
        if (engTotalInteractionsLabel != null) {
            engTotalInteractionsLabel.setText(numFmt.format(s.getTotalInteractions()));
        }

        // 4. Trends Tab - Horizontal BarChart & Rich Mathematical Detail Card
        trendScoreChart.setAnimated(false);
        trendScoreChart.getData().clear();
        XYChart.Series<Number, String> seriesTrend = new XYChart.Series<>();
        seriesTrend.setName("Trend Score (0-100)");

        List<Trend> allTrends = s.getTrends().getAllTrends();
        int maxChartItems = Math.min(8, allTrends.size());
        // Reverse order so top-ranked item appears at the top of the CategoryAxis
        for (int i = maxChartItems - 1; i >= 0; i--) {
            Trend t = allTrends.get(i);
            seriesTrend.getData().add(new XYChart.Data<>(t.getTrendScore(), t.getTopic()));
        }
        trendScoreChart.getData().add(seriesTrend);

        // Apply distinct colors and tooltips to trend bars
        for (XYChart.Data<Number, String> d : seriesTrend.getData()) {
            if (d.getNode() != null) {
                double val = d.getXValue().doubleValue();
                String barColor = val >= 70 ? "#397653" : (val >= 45 ? "#82966B" : "#C6A96B");
                d.getNode().setStyle("-fx-bar-fill: " + barColor + ";");
                Tooltip tt = new Tooltip(String.format("%s\nTrend Score: %.1f / 100", d.getYValue(), val));
                tt.setStyle("-fx-font-size: 12px; -fx-background-color: #26372D; -fx-text-fill: #FFFFFF; -fx-padding: 6px 10px; -fx-background-radius: 4px;");
                Tooltip.install(d.getNode(), tt);
            }
        }

        trendsTable.setItems(FXCollections.observableArrayList(allTrends));
        if (!allTrends.isEmpty()) {
            trendsTable.getSelectionModel().select(0);
        }

        // 5. Sentiment Tab
        sentimentPieChart.getData().clear();
        SentimentResult sent = s.getSentiment();
        sentimentPieChart.getData().add(new PieChart.Data(String.format("Positive (%.1f%%)", sent.getPositivePercent()), sent.getPositiveCount()));
        sentimentPieChart.getData().add(new PieChart.Data(String.format("Neutral (%.1f%%)", sent.getNeutralPercent()), sent.getNeutralCount()));
        sentimentPieChart.getData().add(new PieChart.Data(String.format("Negative (%.1f%%)", sent.getNegativePercent()), sent.getNegativeCount()));

        sentimentPositiveLabel.setText(String.format("%.1f%% (%d)", sent.getPositivePercent(), sent.getPositiveCount()));
        sentimentNeutralLabel.setText(String.format("%.1f%% (%d)", sent.getNeutralPercent(), sent.getNeutralCount()));
        sentimentNegativeLabel.setText(String.format("%.1f%% (%d)", sent.getNegativePercent(), sent.getNegativeCount()));
        sentimentScoreLabel.setText(String.format("%+.2f", sent.getAverageScore()));

        if (sentimentCorpusLabel != null && sent.getCorpusDescription() != null) {
            sentimentCorpusLabel.setText("📊 " + sent.getCorpusDescription());
        }

        ObservableList<String> posWords = FXCollections.observableArrayList();
        sent.getTopPositiveKeywords().forEach((k, v) -> posWords.add(k + " (" + v + ")"));
        topPositiveWordsList.setItems(posWords);

        ObservableList<String> negWords = FXCollections.observableArrayList();
        sent.getTopNegativeKeywords().forEach((k, v) -> negWords.add(k + " (" + v + ")"));
        topNegativeWordsList.setItems(negWords);

        // 6. Best Posting Time Tab
        bestTimeChart.getData().clear();
        XYChart.Series<String, Number> hourSeries = new XYChart.Series<>();
        hourSeries.setName("Avg Engagement");
        for (int h = 0; h < 24; h++) {
            double avg = s.getHourlyEngagement().getOrDefault(h, 0.0);
            String label = (h % 3 == 0) ? (h + "h") : "";
            hourSeries.getData().add(new XYChart.Data<>(label.isEmpty() ? String.valueOf(h) : label, avg));
        }
        bestTimeChart.getData().add(hourSeries);
        peakWindowLabel.setText("Peak Window: " + s.getBestPostingTimeWindow());
        peakWindowStatsLabel.setText("Scheduling posts during this window yields above-average interactions.");

        // 7. Recommendations Tab
        recommendationsContainer.getChildren().clear();
        for (Recommendation r : s.getRecommendations()) {
            VBox card = new VBox(6);
            String cssClass = switch (r.getPriority()) {
                case HIGH -> "rec-card-high";
                case MEDIUM -> "rec-card-medium";
                case LOW -> "rec-card-low";
            };
            card.getStyleClass().addAll(cssClass);

            Label titleLbl = new Label("[" + r.getPriority() + "] " + r.getTitle());
            titleLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #26372D;");

            Label descLbl = new Label(r.getDescription());
            descLbl.setWrapText(true);
            descLbl.setStyle("-fx-text-fill: #26372D; -fx-font-size: 12px;");

            Label ruleLbl = new Label("Rule Evaluated: " + r.getRuleTriggered() + " | Confidence: " + String.format("%.0f%%", r.getConfidenceScore()));
            ruleLbl.setStyle("-fx-text-fill: #657568; -fx-font-size: 11px; -fx-font-style: italic;");

            card.getChildren().addAll(titleLbl, descLbl, ruleLbl);
            recommendationsContainer.getChildren().add(card);
        }

        // 8. Posts Explorer Tab - Enforce Strict Platform and Mode Isolation
        List<Post> allPosts = analyticsService.getPostsForUser(userId, currentAppMode, currentPlatform);
        this.cachedExplorerPosts = (allPosts != null) ? allPosts : new java.util.ArrayList<>();
        if (postFilterPlatformCombo != null) {
            postFilterPlatformCombo.setValue(currentPlatform != null ? currentPlatform.getDisplayName() : "All Platforms");
        }
        applyPostExplorerFilters();

        // 9. Update Account Connection Status
        updateAccountStatuses(userId);
    }

    public DialogPane buildScoreDetailsDialogPane(Trend t) {
        if (t == null) return new DialogPane();

        DialogPane dp = new DialogPane();
        dp.setHeaderText("Detailed Score Breakdown: " + t.getTopic());
        dp.getButtonTypes().add(ButtonType.CLOSE);
        try {
            dp.getStylesheets().add(getClass().getResource("/css/modern-dark.css").toExternalForm());
        } catch (Exception ignored) {}
        dp.setPrefWidth(720);
        dp.setPrefHeight(640);

        VBox contentBox = new VBox(14);
        contentBox.setStyle("-fx-padding: 16px; -fx-background-color: #FFFFFF;");
        contentBox.setFillWidth(true);
        contentBox.setMinHeight(Region.USE_PREF_SIZE);

        // 1. Topic & Overall Status Header Card
        HBox headerCard = new HBox(14);
        headerCard.setAlignment(Pos.CENTER_LEFT);
        headerCard.setStyle("-fx-background-color: #DCE7DC; -fx-padding: 12px 16px; -fx-background-radius: 8px; -fx-border-color: #C5D9C5; -fx-border-radius: 8px;");

        VBox nameBox = new VBox(3);
        Label topicLabel = new Label(t.getTopic());
        topicLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: 800; -fx-text-fill: #26372D;");
        Label subLabel = new Label("Analyzed Keyword / Hashtag from Post Corpus");
        subLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #657568;");
        nameBox.getChildren().addAll(topicLabel, subLabel);
        HBox.setHgrow(nameBox, Priority.ALWAYS);

        // Status Badge
        Label statusBadge = new Label(t.getStatus().getSymbol() + " " + t.getStatus().getLabel());
        switch (t.getStatus()) {
            case RISING -> statusBadge.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #3D8056; -fx-background-color: #D2E3D2; -fx-padding: 4 10; -fx-background-radius: 6px;");
            case EMERGING -> statusBadge.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #82966B; -fx-background-color: #E2EBE2; -fx-padding: 4 10; -fx-background-radius: 6px;");
            case FALLING -> statusBadge.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #B65C52; -fx-background-color: #F9EAEA; -fx-padding: 4 10; -fx-background-radius: 6px;");
            default -> statusBadge.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #657568; -fx-background-color: #DCE7DC; -fx-padding: 4 10; -fx-background-radius: 6px;");
        }

        // Final Composite Score Box
        VBox scoreBox = new VBox(2);
        scoreBox.setAlignment(Pos.CENTER_RIGHT);
        Label scoreTitle = new Label("COMPOSITE SCORE");
        scoreTitle.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #657568;");
        Label scoreVal = new Label(String.format("%.1f / 100", t.getTrendScore()));
        scoreVal.setStyle("-fx-font-size: 22px; -fx-font-weight: 900; -fx-text-fill: #2D6042;");
        scoreBox.getChildren().addAll(scoreTitle, scoreVal);

        headerCard.getChildren().addAll(nameBox, statusBadge, scoreBox);

        // 2. Component Factor Breakdown Cards (4 horizontal cards)
        double wMention = 0.25;
        double wEngagement = 0.35;
        double wGrowth = 0.25;
        double wRecency = 0.15;

        double mContrib = Math.round(t.getMentionScore() * wMention * 10.0) / 10.0;
        double eContrib = Math.round(t.getEngagementScore() * wEngagement * 10.0) / 10.0;
        double gContrib = Math.round(t.getGrowthScore() * wGrowth * 10.0) / 10.0;
        double rContrib = Math.round(t.getRecencyScore() * wRecency * 10.0) / 10.0;

        HBox factorCards = new HBox(10);
        factorCards.getChildren().addAll(
                createFactorCard("Mention Factor (S_m)", "Weight: 25%", t.getMentionScore(), mContrib, "#397653"),
                createFactorCard("Engagement Factor (S_e)", "Weight: 35%", t.getEngagementScore(), eContrib, "#82966B"),
                createFactorCard("Growth Factor (S_g)", "Weight: 25%", t.getGrowthScore(), gContrib, "#C6A96B"),
                createFactorCard("Recency Factor (S_r)", "Weight: 15%", t.getRecencyScore(), rContrib, "#91AD8B")
        );

        // 3. Raw Underlying Metric Inputs Card
        VBox rawMetricsCard = new VBox(8);
        rawMetricsCard.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #C5D9C5; -fx-border-radius: 8px; -fx-padding: 12px;");
        rawMetricsCard.setMinHeight(Region.USE_PREF_SIZE);
        Label rawTitle = new Label("RAW INPUT METRICS (MEASURED FROM POST CORPUS)");
        rawTitle.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #657568;");

        GridPane rawGrid = new GridPane();
        rawGrid.setHgap(24);
        rawGrid.setVgap(6);

        rawGrid.add(createMetricLabel("Current Mentions:", String.valueOf(t.getCurrentMentions())), 0, 0);
        rawGrid.add(createMetricLabel("Previous Mentions:", String.valueOf(t.getPreviousMentions())), 1, 0);
        rawGrid.add(createMetricLabel("Average Interactions:", String.format("%,.0f", t.getAverageInteractions())), 0, 1);
        String growthStr = t.isHasGrowthBaseline() ? String.format("%+.1f%%", t.getGrowthPercentage()) : "Baseline unavailable (Emerging new topic)";
        rawGrid.add(createMetricLabel("Growth Rate:", growthStr), 1, 1);
        rawGrid.add(createMetricLabel("Recency Factor Score:", String.format("%.1f / 100", t.getRecencyScore())), 0, 2);
        rawGrid.add(createMetricLabel("Baseline Status:", t.isHasGrowthBaseline() ? "Established history" : "New topic (Initial detection)"), 1, 2);

        rawMetricsCard.getChildren().addAll(rawTitle, rawGrid);

        // 4. Mathematical Formula Card
        VBox formulaCard = new VBox(6);
        formulaCard.setStyle("-fx-background-color: #DCE7DC; -fx-border-color: #C5D9C5; -fx-border-radius: 8px; -fx-padding: 12px;");
        formulaCard.setMinHeight(Region.USE_PREF_SIZE);
        Label formulaTitle = new Label("MATHEMATICAL FORMULA & SUBSTITUTION:");
        formulaTitle.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #2D6042;");

        Label formulaGeneral = new Label("Trend Score = (0.25 × S_m) + (0.35 × S_e) + (0.25 × S_g) + (0.15 × S_r)");
        formulaGeneral.setStyle("-fx-font-family: monospace; -fx-font-size: 12px; -fx-text-fill: #26372D; -fx-font-weight: bold;");

        String substitutedStr = String.format("Substituted: (0.25 × %.1f) + (0.35 × %.1f) + (0.25 × %.1f) + (0.15 × %.1f)\n           = %.1f + %.1f + %.1f + %.1f = %.1f / 100",
                t.getMentionScore(), t.getEngagementScore(), t.getGrowthScore(), t.getRecencyScore(),
                mContrib, eContrib, gContrib, rContrib, t.getTrendScore());
        Label formulaSub = new Label(substitutedStr);
        formulaSub.setStyle("-fx-font-family: monospace; -fx-font-size: 11px; -fx-text-fill: #2D6042;");

        formulaCard.getChildren().addAll(formulaTitle, formulaGeneral, formulaSub);

        // 5. Strategic Justification & Explanation
        VBox explanationCard = new VBox(6);
        explanationCard.setStyle("-fx-background-color: #F4F8F4; -fx-border-color: #C5D9C5; -fx-border-radius: 8px; -fx-padding: 14px;");
        explanationCard.setMinHeight(Region.USE_PREF_SIZE);
        explanationCard.setFillWidth(true);

        Label explTitle = new Label("STRATEGIC EXPLANATION & RATIONALE:");
        explTitle.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #657568;");

        String growthDesc = t.isHasGrowthBaseline() ? String.format("%+.1f%%", t.getGrowthPercentage()) : "Emerging (New topic)";
        String rationaleText = String.format(
                "Topic \"%s\" was identified in %d posts with an average interaction volume of %,.0f interactions per post. " +
                "Its growth trajectory is classified as %s (%s). With weighted synthesis across volume, audience engagement, velocity, and recency, " +
                "this topic achieves a composite trend score of %.1f / 100, placing it in the %s category.",
                t.getTopic(), t.getCurrentMentions(), t.getAverageInteractions(),
                t.getStatus().getLabel(), growthDesc, t.getTrendScore(), t.getStatus().getLabel()
        );
        Label explText = new Label(rationaleText);
        explText.setWrapText(true);
        explText.setMinHeight(Region.USE_PREF_SIZE);
        explText.setMaxWidth(Double.MAX_VALUE);
        explText.setStyle("-fx-font-size: 12px; -fx-text-fill: #26372D; -fx-line-spacing: 3px;");
        VBox.setVgrow(explText, Priority.ALWAYS);

        explanationCard.getChildren().addAll(explTitle, explText);

        contentBox.getChildren().addAll(headerCard, factorCards, rawMetricsCard, formulaCard, explanationCard);

        ScrollPane scrollPane = new ScrollPane(contentBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle("-fx-background: #FFFFFF; -fx-background-color: #FFFFFF; -fx-border-color: transparent; -fx-padding: 0;");

        dp.setContent(scrollPane);
        return dp;
    }

    public void showTrendScoreDetailsDialog(Trend t) {
        if (t == null) return;

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Topic Trend Score Breakdown & Mathematical Justification");
        dialog.setResizable(true);
        if (ViewNavigator.getPrimaryStage() != null) {
            dialog.initOwner(ViewNavigator.getPrimaryStage());
        }
        DialogPane dp = buildScoreDetailsDialogPane(t);
        dialog.setDialogPane(dp);
        dialog.showAndWait();
    }

    private VBox createFactorCard(String factorName, String weight, double score, double contrib, String color) {
        VBox card = new VBox(4);
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setStyle("-fx-background-color: #F4F8F4; -fx-padding: 10px 12px; -fx-background-radius: 6px; -fx-border-color: #C5D9C5; -fx-border-radius: 6px;");

        Label nameLbl = new Label(factorName);
        nameLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #657568; -fx-font-weight: 600;");

        Label scoreLbl = new Label(String.format("%.1f", score));
        scoreLbl.setStyle("-fx-font-size: 17px; -fx-font-weight: 800; -fx-text-fill: " + color + ";");

        Label weightLbl = new Label(weight);
        weightLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #657568;");

        Label contribLbl = new Label(String.format("+%.1f pts", contrib));
        contribLbl.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");

        card.getChildren().addAll(nameLbl, scoreLbl, weightLbl, contribLbl);
        return card;
    }

    private HBox createMetricLabel(String label, String value) {
        HBox box = new HBox(6);
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #657568; -fx-font-weight: 600;");
        Label val = new Label(value);
        val.setStyle("-fx-font-size: 11px; -fx-text-fill: #26372D; -fx-font-weight: bold;");
        box.getChildren().addAll(lbl, val);
        return box;
    }

    private void updateAccountStatuses(Long userId) {
        List<SocialAccount> accounts = socialAccountService.getAccountsForUser(userId);

        updateCard(Platform.INSTAGRAM, accounts, igStatusLabel, igAccountLabel, igSyncedLabel, igReasonLabel, igSyncBtn, igDisconnectBtn, igConnectBtn, igDemoBtn);
        updateCard(Platform.FACEBOOK, accounts, fbStatusLabel, fbAccountLabel, fbSyncedLabel, fbReasonLabel, fbSyncBtn, fbDisconnectBtn, fbConnectBtn, fbDemoBtn);
        updateCard(Platform.LINKEDIN, accounts, liStatusLabel, liAccountLabel, liSyncedLabel, liReasonLabel, liSyncBtn, liDisconnectBtn, liConnectBtn, liDemoBtn);
        updateCard(Platform.X, accounts, xStatusLabel, xAccountLabel, xSyncedLabel, xReasonLabel, xSyncBtn, xDisconnectBtn, xConnectBtn, xDemoBtn);
    }

    private void updateCard(Platform p, List<SocialAccount> accounts,
                            Label statusLbl, Label accountLbl, Label syncedLbl, Label reasonLbl,
                            Button syncBtn, Button discBtn, Button connBtn, Button demoBtn) {
        if (statusLbl == null) return;
        Optional<SocialAccount> matched = accounts.stream().filter(a -> a.getPlatform() == p).findFirst();

        if (matched.isPresent() && matched.get().isConnected()) {
            SocialAccount acc = matched.get();
            statusLbl.setText(acc.isDemo() ? "✓ Connected [Demo]" : "✓ Connected [Live API]");
            statusLbl.setStyle(acc.isDemo() ? "-fx-text-fill: #B88738; -fx-font-weight: bold;" : "-fx-text-fill: #3D8056; -fx-font-weight: bold;");
            accountLbl.setText("Account: @" + acc.getUsername() + " (" + acc.getDisplayName() + ")");
            syncedLbl.setText("Last Synced: " + (acc.getLastSyncedAt() != null ? acc.getLastSyncedAt().toString().replace('T', ' ') : "Just now"));
            reasonLbl.setVisible(false);
            reasonLbl.setManaged(false);

            if (syncBtn != null) { syncBtn.setVisible(true); syncBtn.setManaged(true); }
            if (discBtn != null) { discBtn.setVisible(true); discBtn.setManaged(true); }
            if (connBtn != null) { connBtn.setVisible(false); connBtn.setManaged(false); }
            if (demoBtn != null) { demoBtn.setVisible(false); demoBtn.setManaged(false); }

        } else if (matched.isPresent() && matched.get().isConnectionFailed()) {
            SocialAccount acc = matched.get();
            statusLbl.setText("⚠ Connection Failed");
            statusLbl.setStyle("-fx-text-fill: #B65C52; -fx-font-weight: bold;");
            accountLbl.setText("Account: Not connected");
            syncedLbl.setText("");
            reasonLbl.setText("Reason: " + (acc.getStatusReason() != null ? acc.getStatusReason() : "Authorization error"));
            reasonLbl.setVisible(true);
            reasonLbl.setManaged(true);

            if (syncBtn != null) { syncBtn.setVisible(false); syncBtn.setManaged(false); }
            if (discBtn != null) { discBtn.setVisible(false); discBtn.setManaged(false); }
            if (connBtn != null) {
                connBtn.setText("Try Again");
                connBtn.setVisible(true);
                connBtn.setManaged(true);
            }
            if (demoBtn != null) {
                demoBtn.setText("Use Demo Mode");
                demoBtn.setVisible(true);
                demoBtn.setManaged(true);
            }

        } else {
            statusLbl.setText("Disconnected");
            statusLbl.setStyle("-fx-text-fill: #657568; -fx-font-weight: bold;");
            accountLbl.setText("Account: Not connected");
            syncedLbl.setText("");
            reasonLbl.setVisible(false);
            reasonLbl.setManaged(false);

            if (syncBtn != null) { syncBtn.setVisible(false); syncBtn.setManaged(false); }
            if (discBtn != null) { discBtn.setVisible(false); discBtn.setManaged(false); }
            if (connBtn != null) {
                connBtn.setText("Connect OAuth");
                connBtn.setVisible(true);
                connBtn.setManaged(true);
            }
            if (demoBtn != null) {
                demoBtn.setText("Demo Mode");
                demoBtn.setVisible(true);
                demoBtn.setManaged(true);
            }
        }
    }

    // Individual Platform Action Handlers
    @FXML public void handleSyncInstagram(ActionEvent event) { syncPlatform(Platform.INSTAGRAM); }
    @FXML public void handleDisconnectInstagram(ActionEvent event) { disconnectPlatform(Platform.INSTAGRAM); }
    @FXML public void handleConnectInstagram(ActionEvent event) { showOAuthConnectionDialog(Platform.INSTAGRAM); }
    @FXML public void handleDemoInstagram(ActionEvent event) { connectDemoPlatform(Platform.INSTAGRAM); }

    @FXML public void handleSyncFacebook(ActionEvent event) { syncPlatform(Platform.FACEBOOK); }
    @FXML public void handleDisconnectFacebook(ActionEvent event) { disconnectPlatform(Platform.FACEBOOK); }
    @FXML public void handleConnectFacebook(ActionEvent event) { showOAuthConnectionDialog(Platform.FACEBOOK); }
    @FXML public void handleDemoFacebook(ActionEvent event) { connectDemoPlatform(Platform.FACEBOOK); }

    @FXML public void handleSyncLinkedIn(ActionEvent event) { syncPlatform(Platform.LINKEDIN); }
    @FXML public void handleDisconnectLinkedIn(ActionEvent event) { disconnectPlatform(Platform.LINKEDIN); }
    @FXML public void handleConnectLinkedIn(ActionEvent event) { showOAuthConnectionDialog(Platform.LINKEDIN); }
    @FXML public void handleDemoLinkedIn(ActionEvent event) { connectDemoPlatform(Platform.LINKEDIN); }

    @FXML public void handleSyncX(ActionEvent event) { syncPlatform(Platform.X); }
    @FXML public void handleDisconnectX(ActionEvent event) { disconnectPlatform(Platform.X); }
    @FXML public void handleConnectX(ActionEvent event) { showOAuthConnectionDialog(Platform.X); }
    @FXML public void handleDemoX(ActionEvent event) { connectDemoPlatform(Platform.X); }

    private void syncPlatform(Platform platform) {
        Long userId = (currentUser != null && currentUser.getId() != null) ? currentUser.getId() : 1L;
        Optional<SocialAccount> acc = socialAccountService.getAccountForUserAndPlatform(userId, platform);
        if (acc.isPresent()) {
            Toast.show(ViewNavigator.getPrimaryStage(), "Syncing data for " + platform.getDisplayName() + "...", false);
            if (analysisStatusLabel != null) {
                analysisStatusLabel.setText("Syncing " + platform.getDisplayName() + " data...");
            }
            if (analysisProgressBar != null) {
                analysisProgressBar.setVisible(true);
                analysisProgressBar.setProgress(-1);
            }
            Task<Integer> task = new Task<>() {
                @Override
                protected Integer call() throws Exception {
                    return socialAccountService.syncAccountData(acc.get(), acc.get().isDemo());
                }
            };
            task.setOnSucceeded(e -> {
                if (analysisProgressBar != null) analysisProgressBar.setVisible(false);
                int count = task.getValue();
                Toast.show(ViewNavigator.getPrimaryStage(), "Synced " + count + " posts for " + platform.getDisplayName(), false);
                triggerRefresh(true);
            });
            task.setOnFailed(e -> {
                if (analysisProgressBar != null) analysisProgressBar.setVisible(false);
                Throwable ex = task.getException();
                Toast.show(ViewNavigator.getPrimaryStage(), "Sync failed: " + (ex != null ? ex.getMessage() : "Unknown error"), true);
                triggerRefresh(false);
            });
            Thread t = new Thread(task, "PlatformSyncThread-" + platform.name());
            t.setDaemon(true);
            t.start();
        } else {
            Toast.show(ViewNavigator.getPrimaryStage(), platform.getDisplayName() + " is not connected yet.", true);
        }
    }

    private void disconnectPlatform(Platform platform) {
        Long userId = (currentUser != null && currentUser.getId() != null) ? currentUser.getId() : 1L;
        socialAccountService.disconnectPlatformForUser(userId, platform);
        Toast.show(ViewNavigator.getPrimaryStage(), "Disconnected " + platform.getDisplayName(), false);
        triggerRefresh(false);
    }

    private void connectDemoPlatform(Platform platform) {
        Long userId = (currentUser != null && currentUser.getId() != null) ? currentUser.getId() : 1L;
        socialAccountService.connectDemoAccount(userId, platform);
        Toast.show(ViewNavigator.getPrimaryStage(), "Connected " + platform.getDisplayName() + " in Demo Mode!", false);
        triggerRefresh(true);
    }

    @FXML
    public void handleNavigateToConnectAccounts(ActionEvent event) {
        if (mainTabPane != null) {
            for (Tab tab : mainTabPane.getTabs()) {
                if (tab.getText() != null && tab.getText().equalsIgnoreCase("Connect Accounts")) {
                    mainTabPane.getSelectionModel().select(tab);
                    return;
                }
            }
            if (mainTabPane.getTabs().size() > 8) {
                mainTabPane.getSelectionModel().select(8);
            }
        }
    }

    @FXML
    public void handleConnectOAuthDialog(ActionEvent event) {
        showOAuthConnectionDialog(Platform.INSTAGRAM);
    }

    @FXML
    public void handleConnectLiveDialog(ActionEvent event) {
        showOAuthConnectionDialog(Platform.INSTAGRAM);
    }

    @FXML
    public void handleConnectCurrentPlatform(ActionEvent event) {
        String filterVal = platformFilterCombo.getValue();
        Platform p = (filterVal != null && !filterVal.equalsIgnoreCase("All Platforms"))
                ? Platform.fromString(filterVal) : Platform.INSTAGRAM;
        showOAuthConnectionDialog(p);
    }

    @FXML
    public void handleSyncCurrentPlatform(ActionEvent event) {
        String filterVal = platformFilterCombo.getValue();
        if (filterVal != null && !filterVal.equalsIgnoreCase("All Platforms")) {
            Platform p = Platform.fromString(filterVal);
            if (p != null) {
                syncPlatform(p);
                return;
            }
        }
        triggerRefresh(true);
    }

    public void showOAuthConnectionDialog(Platform initialPlatform) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Connect Social Media Account");
        if (ViewNavigator.getPrimaryStage() != null && ViewNavigator.getPrimaryStage().getScene() != null) {
            dialog.initOwner(ViewNavigator.getPrimaryStage());
        }

        DialogPane pane = buildOAuthConnectionDialogPane(initialPlatform, false, dialog::close);
        dialog.setDialogPane(pane);
        dialog.setResizable(true);
        dialog.showAndWait();
    }

    public DialogPane buildOAuthConnectionDialogPane(Platform initialPlatform, boolean expandDev, Runnable onClose) {
        Long userId = (currentUser != null && currentUser.getId() != null) ? currentUser.getId() : 1L;

        DialogPane pane = new DialogPane();
        pane.setHeaderText("CONNECT SOCIAL MEDIA ACCOUNT\nOfficial OAuth 2.0 Authorization");
        if (ViewNavigator.getPrimaryStage() != null && ViewNavigator.getPrimaryStage().getScene() != null) {
            pane.getStylesheets().addAll(ViewNavigator.getPrimaryStage().getScene().getStylesheets());
        }
        pane.setStyle("-fx-background-color: #FFFFFF;");
        pane.getButtonTypes().add(ButtonType.CLOSE);

        VBox content = new VBox(12);
        content.setPrefWidth(500);
        content.setStyle("-fx-padding: 8px;");

        // 1. Select Platform Dropdown
        Label selectLabel = new Label("Select Platform:");
        selectLabel.setStyle("-fx-text-fill: #26372D; -fx-font-weight: bold; -fx-font-size: 13px;");

        ComboBox<Platform> platformCombo = new ComboBox<>(FXCollections.observableArrayList(Platform.values()));
        platformCombo.setValue(initialPlatform != null ? initialPlatform : Platform.INSTAGRAM);
        platformCombo.setMaxWidth(Double.MAX_VALUE);
        platformCombo.setStyle("-fx-background-color: #FFFFFF; -fx-text-fill: #26372D; -fx-font-size: 13px; -fx-border-color: #C5D9C5; -fx-border-radius: 6px;");

        // 2. Account Type Requirement Box
        VBox reqBox = new VBox(3);
        reqBox.setStyle("-fx-background-color: #DCE7DC; -fx-border-color: #C5D9C5; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-padding: 8px;");
        Label reqTitle = new Label("Account Type Requirement:");
        reqTitle.setStyle("-fx-text-fill: #2D6042; -fx-font-weight: bold; -fx-font-size: 11px;");
        Label reqDesc = new Label();
        reqDesc.setWrapText(true);
        reqDesc.setStyle("-fx-text-fill: #26372D; -fx-font-size: 12px;");
        reqBox.getChildren().addAll(reqTitle, reqDesc);

        // 3. Status indicator
        Label statusIndicator = new Label();
        statusIndicator.setWrapText(true);
        statusIndicator.setStyle("-fx-font-size: 12px;");

        // 4. Dynamic Action Button (Connect Instagram, Connect Facebook, Connect LinkedIn, Connect X)
        Button connectBtn = new Button();
        connectBtn.setMaxWidth(Double.MAX_VALUE);
        connectBtn.getStyleClass().add("btn-success");
        connectBtn.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-padding: 8px 16px;");

        // 5. Transparency disclaimer box
        VBox noticeBox = new VBox(3);
        noticeBox.setStyle("-fx-background-color: #E2EBE2; -fx-border-color: #C5D9C5; -fx-border-width: 1px; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-padding: 8px; -fx-alignment: center;");
        Label notice1 = new Label("You will be redirected to the official platform authorization page.");
        notice1.setStyle("-fx-text-fill: #657568; -fx-font-size: 12px;");
        Label notice2 = new Label("🔒 We never ask for your social-media password.");
        notice2.setStyle("-fx-text-fill: #2D6042; -fx-font-weight: bold; -fx-font-size: 12px;");
        noticeBox.getChildren().addAll(notice1, notice2);

        // Quick demo mode option
        Button demoBtn = new Button("⚡ Connect in Demo Mode");
        demoBtn.setMaxWidth(Double.MAX_VALUE);
        demoBtn.getStyleClass().add("btn-primary");
        demoBtn.setStyle("-fx-font-size: 12px;");
        demoBtn.setOnAction(e -> {
            Platform p = platformCombo.getValue();
            socialAccountService.connectDemoAccount(userId, p);
            if (onClose != null) onClose.run();
            Toast.show(ViewNavigator.getPrimaryStage(), "Connected " + p.getDisplayName() + " in Demo Mode!", false);
            triggerRefresh(true);
        });

        // 6. Developer Testing / Fallback Mode (Collapsible TitledPane with full visibility & scrolling)
        VBox devBox = new VBox(8);
        devBox.setStyle("-fx-background-color: #F4F8F4; -fx-border-color: #C5D9C5; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 10px;");

        Label devBadge = new Label("🛠 Developer Testing Only (Manual Token Fallback)");
        devBadge.setStyle("-fx-text-fill: #B88738; -fx-font-weight: bold; -fx-font-size: 12px;");

        Label devSub = new Label("Normal users should use official OAuth connection above. Use this fallback only for manual token testing if OAuth application credentials are not yet configured in .env.");
        devSub.setWrapText(true);
        devSub.setStyle("-fx-text-fill: #657568; -fx-font-size: 11px;");

        Label tokenPrompt = new Label("API Access Token / Bearer Token:");
        tokenPrompt.setStyle("-fx-text-fill: #26372D; -fx-font-size: 12px; -fx-font-weight: 600;");

        PasswordField devTokenField = new PasswordField();
        devTokenField.setPromptText("Paste official API access token / Bearer key");
        devTokenField.setStyle("-fx-background-color: #FFFFFF; -fx-text-fill: #26372D; -fx-border-color: #C5D9C5; -fx-border-radius: 6px; -fx-padding: 6px 10px;");

        Button devSubmitBtn = new Button("Authorize with Developer Token");
        devSubmitBtn.getStyleClass().add("btn-secondary");
        devSubmitBtn.setMaxWidth(Double.MAX_VALUE);
        devSubmitBtn.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-padding: 8px 16px;");

        devSubmitBtn.setOnAction(e -> {
            String token = devTokenField.getText().trim();
            if (token.isBlank()) {
                Toast.show(ViewNavigator.getPrimaryStage(), "Developer token cannot be empty.", true);
                return;
            }
            Platform p = platformCombo.getValue();
            devSubmitBtn.setDisable(true);
            devSubmitBtn.setText("Verifying & Connecting Token...");

            Task<SocialAccount> task = new Task<>() {
                @Override
                protected SocialAccount call() throws Exception {
                    return socialAccountService.connectDeveloperTokenAccount(userId, p, token);
                }
            };
            task.setOnSucceeded(ev -> {
                devSubmitBtn.setDisable(false);
                devSubmitBtn.setText("Authorize with Developer Token");
                if (onClose != null) onClose.run();
                SocialAccount acc = task.getValue();

                int postCount = postDAO.findByAccountId(acc.getId()).size();
                Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                successAlert.setTitle("Developer Token Authorized");
                successAlert.setHeaderText("✓ Connected " + p.getDisplayName() + " via Developer Token!");
                successAlert.setContentText(
                        "Platform: " + p.getDisplayName() + "\n" +
                        "Account Username: @" + acc.getUsername() + "\n" +
                        "Display Name: " + acc.getDisplayName() + "\n" +
                        "Mode: Developer Testing Token\n" +
                        "Live Posts Synchronized: " + postCount + "\n\n" +
                        (postCount > 0
                                ? "Real posts and metrics are now active and displayed."
                                : "Notice: 0 posts were returned from this token.\nThe dashboard will reflect 0 posts in Live Mode until posts are published or you switch to Demo Mode.")
                );
                successAlert.showAndWait();
                platformFilterCombo.setValue(p.getDisplayName());
                triggerRefresh(true);
            });
            task.setOnFailed(ev -> {
                devSubmitBtn.setDisable(false);
                devSubmitBtn.setText("Authorize with Developer Token");
                Throwable ex = task.getException();
                Alert failAlert = new Alert(Alert.AlertType.ERROR);
                failAlert.setTitle("Authentication Failed");
                failAlert.setHeaderText("❌ Could Not Authorize Developer Token");
                failAlert.setContentText(
                        "Developer token verification failed:\n\n" +
                        (ex != null ? ex.getMessage() : "Unknown error") + "\n\n" +
                        "Please verify your API token permissions, expiration date, or test using Demo Mode."
                );
                failAlert.showAndWait();
                triggerRefresh(false);
            });
            Thread t = new Thread(task, "DevTokenAuthThread");
            t.setDaemon(true);
            t.start();
        });

        devBox.getChildren().addAll(devBadge, devSub, tokenPrompt, devTokenField, devSubmitBtn);

        TitledPane devPane = new TitledPane("Developer Testing / Advanced Configuration", devBox);
        devPane.setExpanded(expandDev);
        devPane.setStyle("-fx-text-fill: #26372D; -fx-font-weight: 600; -fx-font-size: 12px;");

        // Refresh configuration once when constructing dialog
        ConfigService.getInstance().reload();

        // Dynamic update function
        Runnable updateUI = () -> {
            Platform p = platformCombo.getValue();
            connectBtn.setText("Connect " + p.getDisplayName());
            reqDesc.setText(p.getRequiredAccountType());

            boolean configured = oAuthService.isConfigured(p);
            OAuthService.PlatformConfigGuide guide = oAuthService.getConfigGuide(p);
            if (configured) {
                statusIndicator.setText("✓ Official OAuth application configured (" + guide.configSourceDescription() + ")");
                statusIndicator.setStyle("-fx-text-fill: #16A34A; -fx-font-weight: bold;");
            } else {
                statusIndicator.setText("⚠ OAuth application not yet configured in .env (Missing: " + guide.missingVariables() + ")");
                statusIndicator.setStyle("-fx-text-fill: #D97706;");
            }
        };

        platformCombo.valueProperty().addListener((obs, oldVal, newVal) -> updateUI.run());
        updateUI.run();

        // Connect button action
        connectBtn.setOnAction(e -> {
            Platform p = platformCombo.getValue();
            if (!oAuthService.isConfigured(p)) {
                // Show clear configuration setup alert
                OAuthService.PlatformConfigGuide guide = oAuthService.getConfigGuide(p);
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("OAuth Setup Required – " + p.getDisplayName());
                alert.setHeaderText("Official OAuth 2.0 Credentials Missing in .env");

                StringBuilder sb = new StringBuilder();
                sb.append("Official OAuth application for ").append(p.getDisplayName()).append(" has not been configured.\n\n");
                sb.append("To enable live authorization:\n");
                sb.append("1. Register a developer app at: ").append(guide.developerPortalUrl()).append("\n");
                sb.append("2. Set ").append(guide.missingVariables()).append(" in your .env file.\n");
                sb.append("3. Redirect URI: ").append(guide.redirectUri()).append("\n");
                sb.append("4. Account Requirement: ").append(guide.requiredAccountType()).append("\n\n");

                if (p == Platform.X && guide.hasBearerToken()) {
                    sb.append("Notice: A Developer Bearer Token is detected in application.properties.\n")
                      .append("You can click 'Use Developer Testing' below to connect directly using that Bearer Token!\n\n");
                }
                sb.append("In the meantime, you can connect in Demo Mode or use the Developer Testing token fallback.");

                alert.setContentText(sb.toString());

                ButtonType demoType = new ButtonType("Connect Demo Mode");
                ButtonType devType = new ButtonType("Use Developer Testing");
                ButtonType cancelType = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
                alert.getButtonTypes().setAll(demoType, devType, cancelType);

                Optional<ButtonType> chosen = alert.showAndWait();
                if (chosen.isPresent() && chosen.get() == demoType) {
                    socialAccountService.connectDemoAccount(userId, p);
                    if (onClose != null) onClose.run();
                    Toast.show(ViewNavigator.getPrimaryStage(), "Connected " + p.getDisplayName() + " in Demo Mode!", false);
                    triggerRefresh(true);
                } else if (chosen.isPresent() && chosen.get() == devType) {
                    devPane.setExpanded(true);
                    if (p == Platform.X && guide.hasBearerToken()) {
                        String bearer = ConfigService.getInstance().get("x.bearer.token", "");
                        if (!bearer.isBlank() && devTokenField.getText().isBlank()) {
                            devTokenField.setText(bearer);
                        }
                    }
                }
                return;
            }

            // Platform is configured: launch official authorization flow
            try {
                String state = oAuthService.generateState(p);
                String authUrl = oAuthService.buildAuthorizationUrl(p, state);

                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(URI.create(authUrl));
                }

                // Show code input dialog for completing authorization
                TextInputDialog codeDialog = new TextInputDialog();
                codeDialog.setTitle("Authorize " + p.getDisplayName());
                codeDialog.setHeaderText("Official Platform Authorization Opened\n" +
                        "Complete the authorization in your browser window, then paste the authorization code or full redirect URL below:");
                codeDialog.setContentText("Authorization Code / Callback URL:");

                codeDialog.showAndWait().ifPresent(input -> {
                    String raw = input.trim();
                    if (raw.isBlank()) {
                        Toast.show(ViewNavigator.getPrimaryStage(), "Authorization was cancelled or code was empty.", false);
                        return;
                    }

                    // Handle user cancellation on platform callback (e.g. ?error=access_denied)
                    if (raw.contains("error=access_denied") || raw.contains("error=user_cancelled_authorize")) {
                        Toast.show(ViewNavigator.getPrimaryStage(), "Authorization was cancelled by the user on " + p.getDisplayName() + ".", false);
                        return;
                    }
                    if (raw.contains("error=")) {
                        Toast.show(ViewNavigator.getPrimaryStage(), "Authorization was not completed: " + raw, true);
                        return;
                    }

                    try {
                        SocialAccount account = socialAccountService.connectOAuthAccount(userId, p, raw);
                        if (onClose != null) onClose.run();

                        int postCount = postDAO.findByAccountId(account.getId()).size();
                        Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                        successAlert.setTitle("Account Connected Successfully");
                        successAlert.setHeaderText("✓ Official OAuth 2.0 Connection Established!");
                        successAlert.setContentText(
                                "Platform: " + p.getDisplayName() + "\n" +
                                "Account Username: @" + account.getUsername() + "\n" +
                                "Display Name: " + (account.getDisplayName() != null ? account.getDisplayName() : p.getDisplayName()) + "\n" +
                                "Connection Status: Connected (Live API Mode)\n" +
                                "Live Posts Synchronized: " + postCount + "\n\n" +
                                (postCount > 0
                                        ? "Your real-time posts and engagement metrics are now active on the dashboard."
                                        : "Notice: 0 live posts were returned by the official API.\nThe dashboard will reflect 0 posts in Live Mode until posts are published or you switch to Demo Mode.")
                        );
                        successAlert.showAndWait();

                        // Automatically filter to newly connected platform
                        platformFilterCombo.setValue(p.getDisplayName());
                        triggerRefresh(true);
                    } catch (Exception ex) {
                        Alert failAlert = new Alert(Alert.AlertType.ERROR);
                        failAlert.setTitle("Authentication Failed");
                        failAlert.setHeaderText("❌ Could Not Connect " + p.getDisplayName());
                        failAlert.setContentText(
                                "Official OAuth 2.0 authorization could not be completed.\n\n" +
                                "Error Details:\n" + ex.getMessage() + "\n\n" +
                                "Troubleshooting Steps:\n" +
                                "1. Ensure you approved permissions on the official platform login screen.\n" +
                                "2. Verify that your redirect URI matches " + p.getDefaultRedirectUri() + "\n" +
                                "3. In the meantime, you can continue exploring with Demo Mode."
                        );
                        failAlert.showAndWait();
                        triggerRefresh(false);
                    }
                });

            } catch (Exception ex) {
                Toast.show(ViewNavigator.getPrimaryStage(), "Error launching browser for OAuth: " + ex.getMessage(), true);
            }
        });

        content.getChildren().addAll(
                selectLabel, platformCombo,
                reqBox, statusIndicator,
                connectBtn, noticeBox,
                demoBtn, new Separator(),
                devPane
        );

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle("-fx-background: #FFFFFF; -fx-background-color: #FFFFFF; -fx-border-color: transparent;");

        pane.setContent(scrollPane);
        pane.setPrefWidth(540);

        devPane.expandedProperty().addListener((obs, wasExpanded, isExpanded) -> {
            javafx.application.Platform.runLater(() -> {
                if (isExpanded) {
                    scrollPane.setVvalue(1.0);
                }
                if (pane.getScene() != null && pane.getScene().getWindow() != null) {
                    pane.getScene().getWindow().sizeToScene();
                }
            });
        });

        return pane;
    }

    private void loadSettingsValues() {
        dbUrlField.setText(configService.get("db.url"));
        dbUsernameField.setText(configService.get("db.username"));
        dbPasswordField.setText("********");

        weightMentionField.setText(String.valueOf(configService.getDouble("trend.weight.mention", 0.25)));
        weightEngagementField.setText(String.valueOf(configService.getDouble("trend.weight.engagement", 0.35)));
        weightGrowthField.setText(String.valueOf(configService.getDouble("trend.weight.growth", 0.25)));
        weightRecencyField.setText(String.valueOf(configService.getDouble("trend.weight.recency", 0.15)));
    }

    @FXML
    public void handleSaveSettings(ActionEvent event) {
        try {
            configService.setProperty("trend.weight.mention", weightMentionField.getText().trim());
            configService.setProperty("trend.weight.engagement", weightEngagementField.getText().trim());
            configService.setProperty("trend.weight.growth", weightGrowthField.getText().trim());
            configService.setProperty("trend.weight.recency", weightRecencyField.getText().trim());
            Toast.show(ViewNavigator.getPrimaryStage(), "Settings updated. Re-evaluating trends...", false);
            triggerRefresh(false);
        } catch (Exception e) {
            Toast.show(ViewNavigator.getPrimaryStage(), "Error saving settings: " + e.getMessage(), true);
        }
    }

    @FXML
    public void handleConnectAllDemo(ActionEvent event) {
        Long userId = (currentUser != null && currentUser.getId() != null) ? currentUser.getId() : 1L;
        socialAccountService.connectAllDemoAccounts(userId);
        Toast.show(ViewNavigator.getPrimaryStage(), "All 4 platforms connected in Demo Mode!", false);
        triggerRefresh(true);
    }

    @FXML
    public void handleLogout(ActionEvent event) {
        authService.logout();
        Toast.show(ViewNavigator.getPrimaryStage(), "Logged out successfully", false);
        ViewNavigator.loadLoginView();
    }

    private void applyPostExplorerFilters() {
        if (cachedExplorerPosts == null) return;
        String query = (postSearchField != null && postSearchField.getText() != null)
                ? postSearchField.getText().trim().toLowerCase() : "";
        String selectedPlatform = (postFilterPlatformCombo != null && postFilterPlatformCombo.getValue() != null)
                ? postFilterPlatformCombo.getValue() : "All Platforms";

        List<Post> filtered = cachedExplorerPosts.stream()
                .filter(p -> {
                    if (!selectedPlatform.equalsIgnoreCase("All Platforms")) {
                        if (p.getPlatform() == null || !p.getPlatform().getDisplayName().equalsIgnoreCase(selectedPlatform)) {
                            return false;
                        }
                    }
                    if (!query.isEmpty()) {
                        String id = p.getId() != null ? p.getId().toLowerCase() : "";
                        String content = p.getContent() != null ? p.getContent().toLowerCase() : "";
                        String type = p.getContentType() != null ? p.getContentType().toLowerCase() : "";
                        return id.contains(query) || content.contains(query) || type.contains(query);
                    }
                    return true;
                })
                .toList();

        allPostsTable.setItems(FXCollections.observableArrayList(filtered));
        if (postCountStatusLabel != null) {
            postCountStatusLabel.setText(String.format("Showing %d of %d posts across platforms", filtered.size(), cachedExplorerPosts.size()));
        }
    }

    @FXML
    public void handleResetPostFilters(ActionEvent event) {
        if (postSearchField != null) postSearchField.clear();
        if (postFilterPlatformCombo != null) postFilterPlatformCombo.setValue("All Platforms");
        applyPostExplorerFilters();
    }

    private void showPostDetailsDialog(Post p) {
        if (p == null) return;

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Post Inspection — " + p.getId());
        dialog.setHeaderText("Platform: " + (p.getPlatform() != null ? p.getPlatform().getDisplayName() : "Unknown") +
                " | Content Type: " + (p.getContentType() != null ? p.getContentType() : "POST"));

        DialogPane dp = dialog.getDialogPane();
        dp.getButtonTypes().add(ButtonType.CLOSE);
        try {
            dp.getStylesheets().add(getClass().getResource("/css/modern-dark.css").toExternalForm());
        } catch (Exception ignored) {}
        dp.setPrefWidth(650);

        VBox contentBox = new VBox(12);
        contentBox.setStyle("-fx-padding: 16px;");

        // Metadata grid
        GridPane metaGrid = new GridPane();
        metaGrid.setHgap(16);
        metaGrid.setVgap(8);

        Label idLbl = new Label("Post ID: " + p.getId());
        idLbl.setStyle("-fx-text-fill: #657568; -fx-font-family: monospace; -fx-font-size: 12px;");

        String timeStr = (p.getTimestamp() != null) ? p.getTimestamp().toString().replace('T', ' ') : "Unavailable";
        Label timeLbl = new Label("Published: " + timeStr);
        timeLbl.setStyle("-fx-text-fill: #26372D; -fx-font-size: 12px;");

        metaGrid.add(idLbl, 0, 0);
        metaGrid.add(timeLbl, 1, 0);

        // Metrics Banner
        HBox metricsBox = new HBox(16);
        metricsBox.setStyle("-fx-background-color: #DCE7DC; -fx-padding: 10px 14px; -fx-background-radius: 8px; -fx-border-color: #C5D9C5; -fx-border-radius: 8px;");

        String sharesStr = p.isSharesAvailable() ? numFmt.format(p.getShares()) : "N/A (API limited)";
        String viewsStr = p.isViewsAvailable() ? numFmt.format(p.getViews()) : "N/A (API limited)";

        Label lk = new Label("Likes: " + numFmt.format(p.getLikes()));
        lk.setStyle("-fx-text-fill: #397653; -fx-font-weight: bold; -fx-font-size: 12px;");
        Label cm = new Label("Comments: " + numFmt.format(p.getCommentsCount()));
        cm.setStyle("-fx-text-fill: #82966B; -fx-font-weight: bold; -fx-font-size: 12px;");
        Label sh = new Label("Shares: " + sharesStr);
        sh.setStyle("-fx-text-fill: #C6A96B; -fx-font-weight: bold; -fx-font-size: 12px;");
        Label vw = new Label("Views: " + viewsStr);
        vw.setStyle("-fx-text-fill: #657568; -fx-font-weight: bold; -fx-font-size: 12px;");
        Label tot = new Label("Total: " + numFmt.format(p.getTotalInteractions()));
        tot.setStyle("-fx-text-fill: #26372D; -fx-font-weight: bold; -fx-font-size: 12px;");

        metricsBox.getChildren().addAll(lk, cm, sh, vw, tot);

        // Full Caption
        Label captionTitle = new Label("Full Caption / Content:");
        captionTitle.setStyle("-fx-text-fill: #26372D; -fx-font-weight: bold; -fx-font-size: 13px;");

        TextArea captionArea = new TextArea(p.getContent() != null ? p.getContent() : "[Empty Caption]");
        captionArea.setEditable(false);
        captionArea.setWrapText(true);
        captionArea.setPrefRowCount(8);
        captionArea.setStyle("-fx-font-size: 12px; -fx-line-spacing: 2px;");

        // Action buttons
        HBox btnBox = new HBox(10);
        Button copyBtn = new Button("📋 Copy Caption");
        copyBtn.setStyle("-fx-background-color: #FFFFFF; -fx-text-fill: #26372D; -fx-border-color: #C5D9C5; -fx-border-radius: 6px; -fx-cursor: hand; -fx-font-size: 11px;");
        copyBtn.setOnAction(e -> {
            javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
            javafx.scene.input.ClipboardContent cc = new javafx.scene.input.ClipboardContent();
            cc.putString(p.getContent() != null ? p.getContent() : "");
            clipboard.setContent(cc);
            copyBtn.setText("✓ Copied!");
        });

        btnBox.getChildren().add(copyBtn);

        if (p.getPermalink() != null && !p.getPermalink().isBlank()) {
            Button openLinkBtn = new Button("🌐 Open in Browser ↗");
            openLinkBtn.setStyle("-fx-background-color: #397653; -fx-text-fill: white; -fx-font-weight: 600; -fx-cursor: hand; -fx-font-size: 11px; -fx-background-radius: 6px;");
            openLinkBtn.setOnAction(e -> {
                try {
                    if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                        Desktop.getDesktop().browse(new URI(p.getPermalink()));
                    }
                } catch (Exception ex) {
                    Toast.show(ViewNavigator.getPrimaryStage(), "Could not open URL: " + ex.getMessage(), true);
                }
            });
            btnBox.getChildren().add(openLinkBtn);
        }

        // API Metrics Diagnostic Card when metrics are unavailable
        if (!p.isSharesAvailable() || !p.isViewsAvailable()) {
            VBox diagBox = new VBox(4);
            diagBox.setStyle("-fx-background-color: #E2EBE2; -fx-padding: 10px; -fx-border-color: #C5D9C5; -fx-border-radius: 6px; -fx-background-radius: 6px;");
            Label diagTitle = new Label("ℹ️ Metric Availability Diagnostic (Meta Graph API):");
            diagTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #2D6042;");

            String msg = (p.getPlatform() == Platform.INSTAGRAM)
                    ? "• Shares & Views require an active OAuth session with 'instagram_manage_insights' permission.\n" +
                      "• Video/Reel media support 'plays/views'; static Photos provide reach/impressions.\n" +
                      "• If your Meta session has expired (OAuth 190), reconnect your account in the Accounts tab."
                    : "• Shares/Views are not published by this platform or require additional API tier permissions.";
            Label diagMsg = new Label(msg);
            diagMsg.setWrapText(true);
            diagMsg.setStyle("-fx-font-size: 11px; -fx-text-fill: #26372D;");
            diagBox.getChildren().addAll(diagTitle, diagMsg);
            contentBox.getChildren().add(diagBox);
        }

        contentBox.getChildren().addAll(metaGrid, metricsBox, captionTitle, captionArea, btnBox);
        dp.setContent(contentBox);

        dialog.showAndWait();
    }

    private String truncate(String text, int max) {
        if (text == null) return "";
        if (text.length() <= max) return text;
        return text.substring(0, max) + "...";
    }
}
