package com.akshit.comefort.gui.screens;

import com.akshit.comefort.core.Task;
import com.akshit.comefort.core.enums.TaskPriority;
import com.akshit.comefort.core.enums.TaskStatus;
import com.akshit.comefort.gui.components.CommandFormBuilder;
import com.akshit.comefort.service.ProjectService;
import com.akshit.comefort.service.TaskService;
import com.akshit.comefort.util.DateTimeUtil;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Tasks screen — GUI equivalent of:
 *   - cmf task add <title> [-p project] [-d due] [--priority priority]
 *   - cmf task list [--project name] [--status status] [--priority priority]
 *   - cmf task done <id|title-fragment>
 *   - cmf task edit <id> [-t title] [--desc desc] [--priority] [-d due] [-s status] [-p project]
 *   - cmf task delete <id|title-fragment>
 *
 * Every single flag and parameter is represented in the GUI.
 */
public class TasksScreen {

    private final TaskService taskService;
    private final ProjectService projectService;
    private final Runnable onDataChanged;

    public TasksScreen(TaskService taskService, ProjectService projectService,
                       Runnable onDataChanged) {
        this.taskService = taskService;
        this.projectService = projectService;
        this.onDataChanged = onDataChanged;
    }

    public Region getView() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(32, 40, 32, 40));

        // Header
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("☐ Tasks");
        title.getStyleClass().add("page-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button addBtn = new Button("+ New Task");
        addBtn.getStyleClass().add("btn-primary");
        addBtn.setOnAction(e -> showAddTaskDialog());
        header.getChildren().addAll(title, spacer, addBtn);
        root.getChildren().add(header);

        // Filter bar — GUI equivalent of cmf task list --project X --status Y --priority Z
        HBox filterBar = buildFilterBar(root);
        root.getChildren().add(filterBar);

        // CLI hint
        Label cliHint = new Label("CLI: cmf task list [--project name] [--status status] [--priority priority]");
        cliHint.getStyleClass().add("page-subtitle");
        root.getChildren().add(cliHint);

        // Task list
        List<Task> tasks = taskService.listAll(null, null, null);
        Map<String, String> projectNames = projectService.getProjectNameMap();
        if (tasks.isEmpty()) {
            VBox empty = new VBox(8);
            empty.getStyleClass().add("empty-state");
            Label emptyTitle = new Label("No tasks yet");
            emptyTitle.getStyleClass().add("empty-title");
            Label emptySub = new Label("Create your first task with the button above");
            emptySub.getStyleClass().add("empty-subtitle");
            empty.getChildren().addAll(emptyTitle, emptySub);
            root.getChildren().add(empty);
        } else {
            for (Task task : tasks) {
                root.getChildren().add(buildTaskRow(task, projectNames));
            }
        }

        return root;
    }

    /**
     * Filter bar with dropdowns matching CLI flags.
     */
    private HBox buildFilterBar(VBox root) {
        HBox bar = new HBox(12);
        bar.setAlignment(Pos.CENTER_LEFT);

        // Project filter — CLI: --project
        ComboBox<String> projectFilter = new ComboBox<>();
        projectFilter.setPromptText("All Projects");
        projectFilter.getItems().add("All");
        projectService.listAll().forEach(p -> projectFilter.getItems().add(p.getName()));

        // Status filter — CLI: --status
        ComboBox<String> statusFilter = new ComboBox<>();
        statusFilter.setPromptText("All Statuses");
        statusFilter.getItems().add("All");
        for (TaskStatus s : TaskStatus.values()) {
            statusFilter.getItems().add(s.getDisplayName());
        }

        // Priority filter — CLI: --priority
        ComboBox<String> priorityFilter = new ComboBox<>();
        priorityFilter.setPromptText("All Priorities");
        priorityFilter.getItems().add("All");
        for (TaskPriority p : TaskPriority.values()) {
            priorityFilter.getItems().add(p.getDisplayName());
        }

        Button applyBtn = new Button("Apply Filters");
        applyBtn.getStyleClass().add("btn-secondary");
        applyBtn.setTooltip(new Tooltip("CLI: cmf task list --project X --status Y --priority Z"));
        applyBtn.setOnAction(e -> {
            String projName = projectFilter.getValue();
            String statusVal = statusFilter.getValue();
            String prioVal = priorityFilter.getValue();

            String projectId = null;
            if (projName != null && !"All".equals(projName)) {
                try {
                    projectId = projectService.resolve(projName).getId();
                } catch (Exception ignored) {
                }
            }

            TaskStatus status = null;
            if (statusVal != null && !"All".equals(statusVal)) {
                try {
                    status = TaskStatus.fromString(statusVal);
                } catch (Exception ignored) {
                }
            }

            TaskPriority priority = null;
            if (prioVal != null && !"All".equals(prioVal)) {
                try {
                    priority = TaskPriority.fromString(prioVal);
                } catch (Exception ignored) {
                }
            }

            // Rebuild task list with filters
            List<Task> filtered = taskService.listAll(projectId, status, priority);
            // Remove old task items (keep header, filters, cli hint)
            root.getChildren().removeIf(node ->
                    node.getStyleClass().contains("task-item") ||
                    node.getStyleClass().contains("empty-state"));

            if (filtered.isEmpty()) {
                VBox empty = new VBox(8);
                empty.getStyleClass().add("empty-state");
                Label emptyTitle = new Label("No tasks match filters");
                emptyTitle.getStyleClass().add("empty-title");
                empty.getChildren().add(emptyTitle);
                root.getChildren().add(empty);
            } else {
                Map<String, String> pNames = projectService.getProjectNameMap();
                for (Task task : filtered) {
                    root.getChildren().add(buildTaskRow(task, pNames));
                }
            }
        });

        Label filterLabel = new Label("Filters:");
        filterLabel.getStyleClass().add("form-label");
        bar.getChildren().addAll(filterLabel, projectFilter, statusFilter, priorityFilter, applyBtn);
        return bar;
    }

    /**
     * Builds a single task row with action buttons.
     */
    private HBox buildTaskRow(Task task, Map<String, String> projectNames) {
        HBox row = new HBox(8);
        row.getStyleClass().add("task-item");
        row.setAlignment(Pos.CENTER_LEFT);

        // Done checkbox — CLI: cmf task done <id>
        CheckBox doneCheck = new CheckBox();
        doneCheck.setSelected(task.getStatus() == TaskStatus.DONE);
        doneCheck.setTooltip(new Tooltip("CLI: cmf task done " + task.getShortId()));
        doneCheck.setOnAction(e -> {
            if (doneCheck.isSelected() && task.getStatus() != TaskStatus.DONE) {
                taskService.complete(task.getId());
                onDataChanged.run();
            }
        });

        // Priority badge
        Label priority = new Label(task.getPriority().getDisplayName());
        priority.getStyleClass().addAll("badge",
                "badge-" + task.getPriority().name().toLowerCase());
        priority.setMinWidth(60);

        // Title
        Label titleLabel = new Label(task.getTitle());
        titleLabel.getStyleClass().add("task-title");
        if (task.getStatus() == TaskStatus.DONE) {
            titleLabel.setStyle("-fx-strikethrough: true; -fx-opacity: 0.5;");
        }
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        // Project name (loaded from map — 0 queries)
        Label projectLabel = new Label();
        if (task.getProjectId() != null) {
            String pName = projectNames.get(task.getProjectId());
            if (pName != null) {
                projectLabel.setText(pName);
            }
        }
        projectLabel.getStyleClass().add("task-meta");

        // Due date
        Label dueLabel = new Label(DateTimeUtil.formatDueDate(task.getDueDate()));
        dueLabel.getStyleClass().add("task-meta");
        if (task.isOverdue()) {
            dueLabel.setStyle("-fx-text-fill: #e74c3c;");
        }

        // Status
        Label statusLabel = new Label(task.getStatus().getIcon() + " " + task.getStatus().getDisplayName());
        statusLabel.getStyleClass().add("label-small");
        statusLabel.setMinWidth(80);

        // Edit button — CLI: cmf task edit <id>
        Button editBtn = new Button("✏");
        editBtn.getStyleClass().add("btn-ghost");
        editBtn.setTooltip(new Tooltip("CLI: cmf task edit " + task.getShortId()));
        editBtn.setOnAction(e -> showEditTaskDialog(task));

        // Delete button — CLI: cmf task delete <id>
        Button deleteBtn = new Button("🗑");
        deleteBtn.getStyleClass().add("btn-ghost");
        deleteBtn.setTooltip(new Tooltip("CLI: cmf task delete " + task.getShortId()));
        deleteBtn.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Delete task: " + task.getTitle() + "?",
                    ButtonType.YES, ButtonType.NO);
            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.YES) {
                    taskService.delete(task.getId());
                    onDataChanged.run();
                }
            });
        });

        row.getChildren().addAll(doneCheck, priority, titleLabel, projectLabel,
                dueLabel, statusLabel, editBtn, deleteBtn);
        return row;
    }

    /**
     * Add task dialog — GUI equivalent of:
     *   cmf task add <title> -p <project> -d <due> --priority <priority>
     *
     * Every flag has a matching form control.
     */
    private void showAddTaskDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Add Task");
        dialog.setHeaderText("Create a new task");

        // Build project name choices
        List<String> projectNames = projectService.listAll().stream()
                .map(p -> p.getName()).toList();

        CommandFormBuilder form = new CommandFormBuilder("cmf task add")
                .addTextParam("title", "Task Title", "e.g., Fix batch registration bug",
                        "<title> (positional, required)", true)
                .addChoiceOption("project", "Project", projectNames, null,
                        "-p", "-p <project-name>")
                .addDateOption("due", "Due Date", "-d",
                        "-d <today|tomorrow|yyyy-MM-dd>")
                .addChoiceOption("priority", "Priority",
                        List.of("Low", "Medium", "High", "Critical"),
                        "Medium", "--priority", "--priority <low|medium|high|critical>");

        form.onExecute(cmd -> {
            String titleVal = form.getFieldValueByName("title");
            String projectName = form.getFieldValueByName("project");
            String dueStr = form.getFieldValueByName("due");
            String prioStr = form.getFieldValueByName("priority");

            if (titleVal == null || titleVal.isBlank()) {
                showAlert("Task title is required.");
                return;
            }

            String projectId = null;
            if (projectName != null && !projectName.isBlank()) {
                try {
                    projectId = projectService.resolve(projectName).getId();
                } catch (Exception ex) {
                    showAlert("Project not found: " + projectName);
                    return;
                }
            }

            TaskPriority priority = TaskPriority.MEDIUM;
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
                        showAlert("Invalid due date: " + dueStr);
                        return;
                    }
                }
            }

            try {
                taskService.create(titleVal, projectId, priority, due);
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

    /**
     * Edit task dialog — GUI equivalent of:
     *   cmf task edit <id> [-t title] [--desc desc] [--priority priority]
     *       [-d due] [-s status] [-p project]
     *
     * Every single flag has a matching GUI control.
     */
    private void showEditTaskDialog(Task task) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Edit Task");
        dialog.setHeaderText("Edit: " + task.getTitle());

        List<String> projectNames = projectService.listAll().stream()
                .map(p -> p.getName()).toList();
        List<String> statuses = List.of("Open", "In Progress", "Done", "Archived");

        CommandFormBuilder form = new CommandFormBuilder("cmf task edit " + task.getShortId())
                .addTextOption("title", "Title", task.getTitle(),
                        "-t", "-t \"new title\"")
                .addTextOption("description", "Description",
                        task.getDescription() != null ? task.getDescription() : "",
                        "--desc", "--desc \"description\"")
                .addChoiceOption("priority", "Priority",
                        List.of("Low", "Medium", "High", "Critical"),
                        task.getPriority().getDisplayName(),
                        "--priority", "--priority <low|medium|high|critical>")
                .addDateOption("due", "Due Date", "-d",
                        "-d <today|tomorrow|yyyy-MM-dd>")
                .addBooleanFlag("clearDue", "Clear Due Date", "--clear-due", "--clear-due")
                .addChoiceOption("status", "Status", statuses,
                        task.getStatus().getDisplayName(),
                        "-s", "-s <open|in_progress|done|archived>")
                .addChoiceOption("project", "Move to Project", projectNames,
                        null, "-p", "-p <project-name>");

        form.onExecute(cmd -> {
            String titleVal = form.getFieldValueByName("title");
            String desc = form.getFieldValueByName("description");
            String prioStr = form.getFieldValueByName("priority");
            String dueStr = form.getFieldValueByName("due");
            boolean clearDue = "true".equals(form.getFieldValueByName("clearDue"));
            String statusStr = form.getFieldValueByName("status");
            String projStr = form.getFieldValueByName("project");

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

            TaskStatus status = null;
            if (statusStr != null && !statusStr.isBlank()) {
                try {
                    status = TaskStatus.fromString(statusStr);
                } catch (Exception ignored) {
                }
            }

            String projectId = null;
            if (projStr != null && !projStr.isBlank()) {
                try {
                    projectId = projectService.resolve(projStr).getId();
                } catch (Exception ignored) {
                }
            }

            try {
                taskService.update(task.getId(), titleVal, desc, priority, due, clearDue, status, projectId);
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
