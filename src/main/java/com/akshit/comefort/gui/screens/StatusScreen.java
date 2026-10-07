package com.akshit.comefort.gui.screens;

import com.akshit.comefort.core.ActivityEntry;
import com.akshit.comefort.core.enums.TaskStatus;
import com.akshit.comefort.service.*;
import com.akshit.comefort.util.DateTimeUtil;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;

/**
 * Status screen — GUI equivalent of `cmf status` (or `comefort status`).
 * Shows global statistics, counts, and recent activity.
 */
public class StatusScreen {

    private final ProjectService projectService;
    private final TaskService taskService;
    private final NoteService noteService;
    private final CaptureService captureService;
    private final ActivityService activityService;

    public StatusScreen(ProjectService projectService, TaskService taskService,
                        NoteService noteService, CaptureService captureService,
                        ActivityService activityService) {
        this.projectService = projectService;
        this.taskService = taskService;
        this.noteService = noteService;
        this.captureService = captureService;
        this.activityService = activityService;
    }

    public Region getView() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(32, 40, 32, 40));

        // Header
        Label title = new Label("📊 Status");
        title.getStyleClass().add("page-title");
        Label cliHint = new Label("CLI: cmf status (or comefort status)");
        cliHint.getStyleClass().add("page-subtitle");
        root.getChildren().addAll(title, cliHint);

        // Stats cards
        int projectCount = projectService.count();
        int totalTasks = taskService.count();
        int openTasks = taskService.countByStatus(TaskStatus.OPEN)
                + taskService.countByStatus(TaskStatus.IN_PROGRESS);
        int doneTasks = taskService.countByStatus(TaskStatus.DONE);
        int noteCount = noteService.count();
        int inboxCount = captureService.getInboxCount();

        HBox statsGrid = new HBox(16);
        statsGrid.getChildren().addAll(
                buildStatCard("🚀", "Projects", String.valueOf(projectCount)),
                buildStatCard("☐", "Tasks", totalTasks + " (" + openTasks + " open, " + doneTasks + " done)"),
                buildStatCard("📝", "Notes", String.valueOf(noteCount)),
                buildStatCard("📥", "Inbox", inboxCount + " unprocessed")
        );
        root.getChildren().add(statsGrid);

        // Recent activity
        List<ActivityEntry> recent = activityService.getRecentActivity(15);
        if (!recent.isEmpty()) {
            Label actHeader = new Label("Recent Activity");
            actHeader.getStyleClass().add("label-section");
            root.getChildren().addAll(new Separator(), actHeader);

            for (ActivityEntry entry : recent) {
                HBox row = new HBox(12);
                row.getStyleClass().add("activity-item");
                row.setPadding(new Insets(4, 0, 4, 0));

                Label action = new Label(entry.getAction().getDisplayName());
                action.setMinWidth(80);
                action.setStyle("-fx-font-weight: bold;");

                Label entity = new Label(entry.getEntityType().name());
                entity.getStyleClass().addAll("badge", "badge-open");
                entity.setMinWidth(60);

                Label summary = new Label(entry.getSummary() != null ? entry.getSummary() : "");
                HBox.setHgrow(summary, Priority.ALWAYS);

                Label time = new Label(DateTimeUtil.relativeTime(entry.getCreatedAt()));
                time.getStyleClass().add("label-muted");

                row.getChildren().addAll(action, entity, summary, time);
                root.getChildren().add(row);
            }
        }

        // CLI preview
        Label cliPreview = new Label("cmf status");
        cliPreview.getStyleClass().add("command-preview");
        root.getChildren().addAll(new Separator(), cliPreview);

        return root;
    }

    /**
     * Builds a stat card with icon, label, and value.
     */
    private VBox buildStatCard(String icon, String label, String value) {
        VBox card = new VBox(4);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(16, 20, 16, 20));
        card.setMinWidth(160);
        HBox.setHgrow(card, Priority.ALWAYS);

        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 24px;");

        Label nameLabel = new Label(label);
        nameLabel.getStyleClass().add("label-muted");
        nameLabel.setStyle("-fx-font-size: 12px;");

        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        card.getChildren().addAll(iconLabel, nameLabel, valueLabel);
        return card;
    }
}
