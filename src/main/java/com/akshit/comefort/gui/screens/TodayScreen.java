package com.akshit.comefort.gui.screens;

import com.akshit.comefort.core.ActivityEntry;
import com.akshit.comefort.core.Capture;
import com.akshit.comefort.core.Task;
import com.akshit.comefort.service.*;
import com.akshit.comefort.util.DateTimeUtil;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;
import java.util.function.Consumer;

/**
 * Today dashboard screen — GUI equivalent of `cf today`.
 * Shows: quick capture bar + overdue + due today + high priority + inbox count + activity.
 */
public class TodayScreen {

    private final TodayService todayService;
    private final ProjectService projectService;
    private final TaskService taskService;
    private final CaptureService captureService;
    private final Consumer<String> navigator;

    public TodayScreen(TodayService todayService, ProjectService projectService,
                       TaskService taskService, CaptureService captureService,
                       Consumer<String> navigator) {
        this.todayService = todayService;
        this.projectService = projectService;
        this.taskService = taskService;
        this.captureService = captureService;
        this.navigator = navigator;
    }

    /**
     * Builds and returns the today view.
     */
    public Region getView() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(32, 40, 32, 40));

        // Page title
        Label title = new Label("🔥 Today");
        title.getStyleClass().add("page-title");
        root.getChildren().add(title);

        // Quick capture bar — GUI equivalent of `cf c <text>`
        TextField captureField = new TextField();
        captureField.getStyleClass().add("quick-capture");
        captureField.setPromptText("⚡ Quick capture — type anything and press Enter  (CLI: cf c \"your thought\")");
        captureField.setOnAction(e -> {
            String text = captureField.getText().trim();
            if (!text.isEmpty()) {
                captureService.capture(text);
                captureField.clear();
                refreshView(root);
            }
        });
        root.getChildren().add(captureField);

        // Build the today view data
        TodayService.TodayView view = todayService.buildTodayView();

        // Overdue tasks
        if (!view.overdueTasks().isEmpty()) {
            root.getChildren().add(buildTaskSection("⚠ OVERDUE", view.overdueTasks(),
                    "badge-critical"));
        }

        // Due today
        if (!view.dueTodayTasks().isEmpty()) {
            root.getChildren().add(buildTaskSection("📅 DUE TODAY", view.dueTodayTasks(),
                    "badge-high"));
        }

        // High priority
        if (!view.highPriorityTasks().isEmpty()) {
            root.getChildren().add(buildTaskSection("🔥 HIGH PRIORITY",
                    view.highPriorityTasks(), "badge-medium"));
        }

        // Inbox count
        if (view.inboxCount() > 0) {
            HBox inboxBar = new HBox(8);
            inboxBar.setAlignment(Pos.CENTER_LEFT);
            inboxBar.setPadding(new Insets(8, 12, 8, 12));
            inboxBar.getStyleClass().add("card");

            Label inboxLabel = new Label("📥 " + view.inboxCount() + " unprocessed captures in inbox");
            Button goToInbox = new Button("View Inbox");
            goToInbox.getStyleClass().add("btn-secondary");
            goToInbox.setOnAction(e -> navigator.accept("inbox"));

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            inboxBar.getChildren().addAll(inboxLabel, spacer, goToInbox);
            root.getChildren().add(inboxBar);
        }

        // Recent activity
        if (!view.recentActivity().isEmpty()) {
            VBox activitySection = new VBox(4);
            Label actHeader = new Label("Recent Activity");
            actHeader.getStyleClass().add("label-section");
            activitySection.getChildren().add(actHeader);

            for (ActivityEntry entry : view.recentActivity()) {
                HBox row = new HBox(8);
                row.getStyleClass().add("activity-item");
                Label action = new Label(entry.getAction().getDisplayName());
                action.setMinWidth(80);
                Label summary = new Label(entry.getSummary() != null ? entry.getSummary() : "");
                Label time = new Label(DateTimeUtil.relativeTime(entry.getCreatedAt()));
                time.getStyleClass().add("label-muted");
                Region sp = new Region();
                HBox.setHgrow(sp, Priority.ALWAYS);
                row.getChildren().addAll(action, summary, sp, time);
                activitySection.getChildren().add(row);
            }
            root.getChildren().add(activitySection);
        }

        // Empty state
        if (view.overdueTasks().isEmpty() && view.dueTodayTasks().isEmpty()
                && view.highPriorityTasks().isEmpty() && view.inboxCount() == 0) {
            VBox emptyState = new VBox(8);
            emptyState.getStyleClass().add("empty-state");
            Label emptyTitle = new Label("Nothing urgent! You're all caught up. 🎉");
            emptyTitle.getStyleClass().add("empty-title");
            Label emptySub = new Label("Capture something with the bar above, or add tasks.");
            emptySub.getStyleClass().add("empty-subtitle");
            emptyState.getChildren().addAll(emptyTitle, emptySub);
            root.getChildren().add(emptyState);
        }

        return root;
    }

    /**
     * Builds a section showing a list of tasks.
     */
    private VBox buildTaskSection(String header, List<Task> tasks, String badgeClass) {
        VBox section = new VBox(4);
        section.setPadding(new Insets(8, 0, 0, 0));

        Label sectionHeader = new Label(header + " (" + tasks.size() + ")");
        sectionHeader.getStyleClass().add("label-section");
        section.getChildren().add(sectionHeader);

        for (Task task : tasks) {
            HBox row = new HBox(8);
            row.getStyleClass().add("task-item");
            row.setAlignment(Pos.CENTER_LEFT);

            // Status checkbox
            CheckBox statusCheck = new CheckBox();
            statusCheck.setSelected(false);
            statusCheck.setOnAction(e -> {
                if (statusCheck.isSelected()) {
                    taskService.complete(task.getId());
                }
            });

            // Priority badge
            Label priority = new Label(task.getPriority().getDisplayName());
            priority.getStyleClass().addAll("badge", getBadgeClass(task.getPriority()));
            priority.setMinWidth(60);

            // Title
            Label titleLabel = new Label(task.getTitle());
            titleLabel.getStyleClass().add("task-title");
            HBox.setHgrow(titleLabel, Priority.ALWAYS);

            // Project name
            Label projectLabel = new Label();
            if (task.getProjectId() != null) {
                try {
                    projectLabel.setText(projectService.getById(task.getProjectId()).getName());
                } catch (Exception ignored) {
                }
            }
            projectLabel.getStyleClass().add("task-meta");

            // Due date
            Label dueLabel = new Label(DateTimeUtil.formatDueDate(task.getDueDate()));
            dueLabel.getStyleClass().add("task-meta");

            row.getChildren().addAll(statusCheck, priority, titleLabel, projectLabel, dueLabel);
            section.getChildren().add(row);
        }

        return section;
    }

    private String getBadgeClass(com.akshit.comefort.core.enums.TaskPriority priority) {
        return switch (priority) {
            case CRITICAL -> "badge-critical";
            case HIGH -> "badge-high";
            case MEDIUM -> "badge-medium";
            case LOW -> "badge-low";
        };
    }

    private void refreshView(VBox root) {
        root.getChildren().clear();
        Region newView = getView();
        if (newView instanceof VBox vbox) {
            root.getChildren().addAll(vbox.getChildren());
        }
    }
}
