package com.akshit.comefort.gui.screens;

import com.akshit.comefort.core.Capture;
import com.akshit.comefort.service.CaptureService;
import com.akshit.comefort.util.DateTimeUtil;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;

/**
 * Inbox screen — GUI equivalent of:
 *   - cf inbox              (list unprocessed captures)
 *   - cf c <text>           (quick capture)
 *
 * Shows unprocessed captures and allows instant capture.
 */
public class InboxScreen {

    private final CaptureService captureService;
    private final Runnable onDataChanged;

    public InboxScreen(CaptureService captureService, Runnable onDataChanged) {
        this.captureService = captureService;
        this.onDataChanged = onDataChanged;
    }

    public Region getView() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(32, 40, 32, 40));

        // Header
        Label title = new Label("📥 Inbox");
        title.getStyleClass().add("page-title");
        root.getChildren().add(title);

        // Quick capture bar — CLI: cf c <text>
        TextField captureField = new TextField();
        captureField.getStyleClass().add("quick-capture");
        captureField.setPromptText("⚡ Quick capture — type anything and press Enter  (CLI: cf c \"your thought\")");
        captureField.setOnAction(e -> {
            String text = captureField.getText().trim();
            if (!text.isEmpty()) {
                captureService.capture(text);
                captureField.clear();
                onDataChanged.run();
            }
        });
        root.getChildren().add(captureField);

        // CLI hint
        Label cliHint = new Label("CLI: cf inbox  |  cf c \"your thought\"  |  Prefixes: task: note: idea:");
        cliHint.getStyleClass().add("page-subtitle");
        root.getChildren().add(cliHint);

        // Inbox list
        List<Capture> inbox = captureService.getInbox();

        if (inbox.isEmpty()) {
            VBox empty = new VBox(8);
            empty.getStyleClass().add("empty-state");
            Label emptyTitle = new Label("Inbox is empty");
            emptyTitle.getStyleClass().add("empty-title");
            Label emptySub = new Label("Capture something using the bar above or CLI: cf c \"your thought\"");
            emptySub.getStyleClass().add("empty-subtitle");
            empty.getChildren().addAll(emptyTitle, emptySub);
            root.getChildren().add(empty);
        } else {
            Label countLabel = new Label(inbox.size() + " unprocessed captures");
            countLabel.getStyleClass().add("label-section");
            root.getChildren().add(countLabel);

            for (Capture capture : inbox) {
                root.getChildren().add(buildCaptureRow(capture));
            }
        }

        return root;
    }

    /**
     * Builds a capture row with type icon, content, time, and action buttons.
     */
    private HBox buildCaptureRow(Capture capture) {
        HBox row = new HBox(10);
        row.getStyleClass().add("capture-item");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 16, 10, 16));

        // Type icon
        Label typeIcon = new Label(capture.getType().getIcon());
        typeIcon.setMinWidth(24);

        // Type badge
        Label typeBadge = new Label(capture.getType().getDisplayName());
        typeBadge.getStyleClass().addAll("badge", "badge-open");
        typeBadge.setMinWidth(50);

        // Content
        Label contentLabel = new Label(capture.getContent());
        contentLabel.getStyleClass().add("task-title");
        contentLabel.setWrapText(true);
        HBox.setHgrow(contentLabel, Priority.ALWAYS);

        // Time
        Label timeLabel = new Label(DateTimeUtil.relativeTime(capture.getCreatedAt()));
        timeLabel.getStyleClass().add("label-muted");

        // ID
        Label idLabel = new Label("[" + capture.getShortId() + "]");
        idLabel.getStyleClass().add("label-muted");

        // Mark processed button
        Button processBtn = new Button("✓");
        processBtn.getStyleClass().add("btn-ghost");
        processBtn.setTooltip(new Tooltip("Mark as processed"));
        processBtn.setOnAction(e -> {
            captureService.markProcessed(capture.getId());
            onDataChanged.run();
        });

        // Delete button
        Button deleteBtn = new Button("🗑");
        deleteBtn.getStyleClass().add("btn-ghost");
        deleteBtn.setTooltip(new Tooltip("Delete capture"));
        deleteBtn.setOnAction(e -> {
            captureService.delete(capture.getId());
            onDataChanged.run();
        });

        row.getChildren().addAll(typeIcon, typeBadge, contentLabel,
                timeLabel, idLabel, processBtn, deleteBtn);
        return row;
    }
}
