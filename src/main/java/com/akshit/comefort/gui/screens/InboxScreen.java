package com.akshit.comefort.gui.screens;

import com.akshit.comefort.core.Capture;
import com.akshit.comefort.core.enums.TaskPriority;
import com.akshit.comefort.gui.components.CommandFormBuilder;
import com.akshit.comefort.service.CaptureService;
import com.akshit.comefort.service.ProjectService;
import com.akshit.comefort.util.DateTimeUtil;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/**
 * Inbox screen — Developer inbox processing engine.
 * Full CLI and GUI parity for:
 *   - cmf inbox              (list unprocessed captures)
 *   - cmf c <text>           (quick capture)
 *   - cmf inbox convert <id> --to <task|note|idea> [-p project] [-d due] [--priority priority]
 *   - cmf inbox done <id>    (mark processed)
 *   - cmf inbox delete <id>  (delete capture)
 */
public class InboxScreen {

    private final CaptureService captureService;
    private final ProjectService projectService;
    private final Runnable onDataChanged;

    public InboxScreen(CaptureService captureService, Runnable onDataChanged) {
        this(captureService, null, onDataChanged);
    }

    public InboxScreen(CaptureService captureService, ProjectService projectService, Runnable onDataChanged) {
        this.captureService = captureService;
        this.projectService = projectService;
        this.onDataChanged = onDataChanged;
    }

    public Region getView() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(32, 40, 32, 40));

        // Header
        Label title = new Label("📥 Inbox");
        title.getStyleClass().add("page-title");
        root.getChildren().add(title);

        // Quick capture bar — CLI: cmf c <text>
        TextField captureField = new TextField();
        captureField.getStyleClass().add("quick-capture");
        captureField.setPromptText("⚡ Quick capture — type anything and press Enter  (CLI: cmf c \"your thought\")");
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
        Label cliHint = new Label("CLI: cmf inbox  |  cmf c \"thought\"  |  cmf inbox convert <id> --to <task|note|idea>  |  cmf inbox done <id>");
        cliHint.getStyleClass().add("page-subtitle");
        root.getChildren().add(cliHint);

        // Inbox list
        List<Capture> inbox = captureService.getInbox();

        if (inbox.isEmpty()) {
            VBox empty = new VBox(8);
            empty.getStyleClass().add("empty-state");
            Label emptyTitle = new Label("Inbox is empty");
            emptyTitle.getStyleClass().add("empty-title");
            Label emptySub = new Label("Capture something using the bar above or CLI: cmf c \"your thought\"");
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
     * Builds a capture row with type icon, content, time, and conversion actions.
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

        // Convert to Task button
        Button toTaskBtn = new Button("➔ Task");
        toTaskBtn.getStyleClass().add("btn-ghost");
        toTaskBtn.setTooltip(new Tooltip("Convert to Task (CLI: cmf inbox convert " + capture.getShortId() + " --to task)"));
        toTaskBtn.setOnAction(e -> showConvertToTaskDialog(capture));

        // Convert to Note button
        Button toNoteBtn = new Button("➔ Note");
        toNoteBtn.getStyleClass().add("btn-ghost");
        toNoteBtn.setTooltip(new Tooltip("Convert to Note (CLI: cmf inbox convert " + capture.getShortId() + " --to note)"));
        toNoteBtn.setOnAction(e -> showConvertToNoteDialog(capture));

        // Convert to Idea button
        Button toIdeaBtn = new Button("💡 Idea");
        toIdeaBtn.getStyleClass().add("btn-ghost");
        toIdeaBtn.setTooltip(new Tooltip("Convert to Idea (CLI: cmf inbox convert " + capture.getShortId() + " --to idea)"));
        toIdeaBtn.setOnAction(e -> {
            try {
                captureService.convertToIdea(capture.getId(), null);
                onDataChanged.run();
            } catch (Exception ex) {
                showAlert(ex.getMessage());
            }
        });

        // Mark processed (archive) button
        Button processBtn = new Button("✓");
        processBtn.getStyleClass().add("btn-ghost");
        processBtn.setTooltip(new Tooltip("Mark as processed / Archive (CLI: cmf inbox done " + capture.getShortId() + ")"));
        processBtn.setOnAction(e -> {
            captureService.markProcessed(capture.getId());
            onDataChanged.run();
        });

        // Delete button
        Button deleteBtn = new Button("🗑");
        deleteBtn.getStyleClass().add("btn-ghost");
        deleteBtn.setTooltip(new Tooltip("Delete capture (CLI: cmf inbox delete " + capture.getShortId() + ")"));
        deleteBtn.setOnAction(e -> {
            captureService.delete(capture.getId());
            onDataChanged.run();
        });

        row.getChildren().addAll(typeIcon, typeBadge, contentLabel,
                timeLabel, idLabel, toTaskBtn, toNoteBtn, toIdeaBtn, processBtn, deleteBtn);
        return row;
    }

    private void showConvertToTaskDialog(Capture capture) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Convert Capture to Task");
        dialog.setHeaderText("Convert to Task: " + capture.getContentPreview());

        List<String> projectNames = (projectService != null)
                ? projectService.listAll().stream().map(p -> p.getName()).toList()
                : Collections.emptyList();

        CommandFormBuilder form = new CommandFormBuilder("cmf inbox convert " + capture.getShortId() + " --to task")
                .addTextOption("title", "Task Title", capture.getContent(),
                        "-t", "-t \"task title\"")
                .addChoiceOption("project", "Assign Project", projectNames,
                        null, "-p", "-p <project-name>")
                .addChoiceOption("priority", "Priority",
                        List.of("Low", "Medium", "High", "Critical"),
                        "Medium", "--priority", "--priority <low|medium|high|critical>")
                .addDateOption("due", "Due Date", "-d",
                        "-d <today|tomorrow|yyyy-MM-dd>");

        form.onExecute(cmd -> {
            String titleVal = form.getFieldValueByName("title");
            String projStr = form.getFieldValueByName("project");
            String prioStr = form.getFieldValueByName("priority");
            String dueStr = form.getFieldValueByName("due");

            TaskPriority priority = null;
            if (prioStr != null && !prioStr.isBlank()) {
                try {
                    priority = TaskPriority.fromString(prioStr);
                } catch (Exception ignored) {
                }
            }

            LocalDate due = null;
            if (dueStr != null && !dueStr.isBlank()) {
                due = DateTimeUtil.parseFriendlyDate(dueStr);
                if (due == null) {
                    try {
                        due = LocalDate.parse(dueStr);
                    } catch (Exception ignored) {
                    }
                }
            }

            String projectId = null;
            if (projectService != null && projStr != null && !projStr.isBlank()) {
                try {
                    projectId = projectService.resolve(projStr).getId();
                } catch (Exception ignored) {
                }
            }

            try {
                captureService.convertToTask(capture.getId(), titleVal, projectId, priority, due);
                dialog.close();
                onDataChanged.run();
            } catch (Exception ex) {
                showAlert(ex.getMessage());
            }
        });

        VBox content = form.build();
        content.setPadding(new Insets(16));
        content.setPrefWidth(520);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().lookupButton(ButtonType.CLOSE).setVisible(false);
        dialog.showAndWait();
    }

    private void showConvertToNoteDialog(Capture capture) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Convert Capture to Note");
        dialog.setHeaderText("Convert to Note: " + capture.getContentPreview());

        List<String> projectNames = (projectService != null)
                ? projectService.listAll().stream().map(p -> p.getName()).toList()
                : Collections.emptyList();

        String defaultTitle = capture.getContent().split("\n")[0].trim();
        if (defaultTitle.length() > 60) defaultTitle = defaultTitle.substring(0, 57) + "...";

        CommandFormBuilder form = new CommandFormBuilder("cmf inbox convert " + capture.getShortId() + " --to note")
                .addTextOption("title", "Note Title", defaultTitle,
                        "-t", "-t \"note title\"")
                .addChoiceOption("project", "Assign Project", projectNames,
                        null, "-p", "-p <project-name>")
                .addTextAreaParam("content", "Content", capture.getContent(),
                        "-c", "-c \"content\"");

        form.onExecute(cmd -> {
            String titleVal = form.getFieldValueByName("title");
            String projStr = form.getFieldValueByName("project");
            String contentVal = form.getFieldValueByName("content");

            String projectId = null;
            if (projectService != null && projStr != null && !projStr.isBlank()) {
                try {
                    projectId = projectService.resolve(projStr).getId();
                } catch (Exception ignored) {
                }
            }

            try {
                captureService.convertToNote(capture.getId(), titleVal, contentVal, projectId);
                dialog.close();
                onDataChanged.run();
            } catch (Exception ex) {
                showAlert(ex.getMessage());
            }
        });

        VBox content = form.build();
        content.setPadding(new Insets(16));
        content.setPrefWidth(520);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().lookupButton(ButtonType.CLOSE).setVisible(false);
        dialog.showAndWait();
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.showAndWait();
    }
}
