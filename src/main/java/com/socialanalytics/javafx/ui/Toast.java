package com.socialanalytics.javafx.ui;

import javafx.animation.FadeTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Clean floating toast notifications for user actions in JavaFX.
 */
public final class Toast {

    private Toast() {
    }

    public static void show(Stage stage, String message, boolean isError) {
        if (stage == null || !stage.isShowing()) return;

        Popup popup = new Popup();
        popup.setAutoFix(true);

        Label label = new Label(message);
        label.setStyle(
                "-fx-background-color: " + (isError ? "#dc2626" : "#059669") + ";" +
                "-fx-text-fill: white;" +
                "-fx-font-weight: bold;" +
                "-fx-font-size: 13px;" +
                "-fx-padding: 10px 20px;" +
                "-fx-background-radius: 8px;" +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 8, 0, 0, 3);"
        );

        StackPane root = new StackPane(label);
        root.setAlignment(Pos.CENTER);
        popup.getContent().add(root);

        popup.show(stage);
        popup.setX(stage.getX() + (stage.getWidth() / 2) - 150);
        popup.setY(stage.getY() + stage.getHeight() - 90);

        FadeTransition fade = new FadeTransition(Duration.millis(2500), root);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);
        fade.setDelay(Duration.millis(1500));
        fade.setOnFinished(e -> popup.hide());
        fade.play();
    }
}
