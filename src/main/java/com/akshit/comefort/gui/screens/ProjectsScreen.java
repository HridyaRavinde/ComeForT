package com.akshit.comefort.gui.screens;

import com.akshit.comefort.core.ActivityEntry;
import com.akshit.comefort.core.Note;
import com.akshit.comefort.core.Project;
import com.akshit.comefort.core.ProjectSummary;
import com.akshit.comefort.core.Task;
import com.akshit.comefort.gui.components.CommandFormBuilder;
import com.akshit.comefort.service.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;

/**
 * Projects screen — GUI equivalent of:
 *   - cmf project add <name> [--desc] [--path]
 *   - cmf project list
 *   - cmf project show <name>
 *
 * Every CLI parameter is represented as a GUI control.
 */
public class ProjectsScreen {

    private final ProjectService projectService;
    private final TaskService taskService;
    private final NoteService noteService;
    private final ActivityService activityService;
    private final Runnable onDataChanged;

    public ProjectsScreen(ProjectService projectService, TaskService taskService,
                          NoteService noteService, ActivityService activityService,
                          Runnable onDataChanged) {
        this.projectService = projectService;
        this.taskService = taskService;
        this.noteService = noteService;
        this.activityService = activityService;
        this.onDataChanged = onDataChanged;
    }

    public Region getView() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(32, 40, 32, 40));

        // Header with add button
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("🚀 Projects");
        title.getStyleClass().add("page-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button addBtn = new Button("+ New Project");
        addBtn.getStyleClass().add("btn-primary");
        addBtn.setOnAction(e -> showAddProjectDialog());
        header.getChildren().addAll(title, spacer, addBtn);
        root.getChildren().add(header);

        // CLI hint
        Label cliHint = new Label("CLI: cmf project list  |  cmf project add <name> --desc \"...\" --path \"...\"");
        cliHint.getStyleClass().add("page-subtitle");
        root.getChildren().add(cliHint);

        // Project list — equivalent of `cmf project list` (loaded via single aggregate query)
        List<ProjectSummary> summaries = projectService.getProjectSummaries();

        if (summaries.isEmpty()) {
            VBox empty = new VBox(8);
            empty.getStyleClass().add("empty-state");
            Label emptyTitle = new Label("No projects yet");
            emptyTitle.getStyleClass().add("empty-title");
            Label emptySub = new Label("Create your first project with the button above");
            emptySub.getStyleClass().add("empty-subtitle");
            empty.getChildren().addAll(emptyTitle, emptySub);
            root.getChildren().add(empty);
        } else {
            for (ProjectSummary summary : summaries) {
                root.getChildren().add(buildProjectCard(summary));
            }
        }

        return root;
    }

    /**
     * Builds a project card — equivalent of `cmf project show <name>`.
     * Shows tasks count, notes count, description, path.
     */
    private VBox buildProjectCard(ProjectSummary summary) {
        Project project = summary.project();
        VBox card = new VBox(8);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(16));

        // Project name + description
        HBox nameRow = new HBox(8);
        nameRow.setAlignment(Pos.CENTER_LEFT);
        Label name = new Label(project.getName());
        name.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        // Show details button — equivalent of `cmf project show <name>`
        Button showBtn = new Button("View Details");
        showBtn.getStyleClass().add("btn-ghost");
        showBtn.setTooltip(new Tooltip("CLI: cmf project show " + project.getName()));
        showBtn.setOnAction(e -> showProjectDetail(project));

        nameRow.getChildren().addAll(name, sp, showBtn);
        card.getChildren().add(nameRow);

        if (project.getDescription() != null && !project.getDescription().isBlank()) {
            Label desc = new Label(project.getDescription());
            desc.getStyleClass().add("label-muted");
            card.getChildren().add(desc);
        }

        if (project.getPath() != null && !project.getPath().isBlank()) {
            Label path = new Label("📁 " + project.getPath());
            path.getStyleClass().add("label-small");
            card.getChildren().add(path);
        }

        // Stats — pre-aggregated in single query
        HBox stats = new HBox(16);
        stats.setPadding(new Insets(4, 0, 0, 0));
        Label taskLabel = new Label("☐ " + summary.totalTasks() + " tasks (" + summary.openTasks() + " open)");
        taskLabel.getStyleClass().add("label-small");
        Label noteLabel = new Label("📝 " + summary.totalNotes() + " notes");
        noteLabel.getStyleClass().add("label-small");
        stats.getChildren().addAll(taskLabel, noteLabel);
        card.getChildren().add(stats);

        return card;
    }

    /**
     * Shows the Add Project dialog.
     * GUI equivalent of: cmf project add <name> --desc "..." --path "..."
     * Every flag has a matching control.
     */
    private void showAddProjectDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Add Project");
        dialog.setHeaderText("Create a new project");

        CommandFormBuilder form = new CommandFormBuilder("cmf project add")
                .addTextParam("name", "Project Name", "e.g., HoneyChain",
                        "<name> (positional, required)", true)
                .addTextOption("description", "Description", "Project description",
                        "--desc", "--desc \"description\"")
                .addTextOption("path", "Filesystem Path", "e.g., C:\\Projects\\HoneyChain",
                        "--path", "--path <path>");

        form.onExecute(cmd -> {
            String name = form.getFieldValueByName("name");
            String desc = form.getFieldValueByName("description");
            String path = form.getFieldValueByName("path");

            if (name == null || name.isBlank()) {
                showAlert("Project name is required.");
                return;
            }

            try {
                projectService.create(name, desc, path);
                dialog.close();
                onDataChanged.run();
            } catch (Exception ex) {
                showAlert(ex.getMessage());
            }
        });

        VBox content = form.build();
        content.setPadding(new Insets(16));
        content.setPrefWidth(480);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().lookupButton(ButtonType.CLOSE).setVisible(false);
        dialog.showAndWait();
    }

    /**
     * Shows project detail view — equivalent of `cmf project show <name>`.
     */
    private void showProjectDetail(Project project) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(project.getName());

        VBox content = new VBox(12);
        content.setPadding(new Insets(16));
        content.setPrefWidth(560);

        // Project info
        Label nameLabel = new Label("🚀 " + project.getName());
        nameLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        content.getChildren().add(nameLabel);

        if (project.getDescription() != null) {
            content.getChildren().add(new Label(project.getDescription()));
        }
        if (project.getPath() != null) {
            content.getChildren().add(new Label("Path: " + project.getPath()));
        }

        // Tasks section
        List<Task> tasks = taskService.listAll(project.getId(), null, null);
        Label tasksHeader = new Label("Tasks (" + tasks.size() + ")");
        tasksHeader.getStyleClass().add("label-section");
        content.getChildren().addAll(new Separator(), tasksHeader);

        if (tasks.isEmpty()) {
            Label noTasks = new Label("No tasks");
            noTasks.getStyleClass().add("label-muted");
            content.getChildren().add(noTasks);
        } else {
            for (Task task : tasks) {
                HBox row = new HBox(8);
                row.setAlignment(Pos.CENTER_LEFT);
                Label statusIcon = new Label(task.getStatus().getIcon());
                Label priorityBadge = new Label(task.getPriority().getDisplayName());
                priorityBadge.getStyleClass().addAll("badge",
                        "badge-" + task.getPriority().name().toLowerCase());
                Label taskTitle = new Label(task.getTitle());
                row.getChildren().addAll(statusIcon, priorityBadge, taskTitle);
                content.getChildren().add(row);
            }
        }

        // Notes section
        List<Note> notes = noteService.listByProject(project.getId());
        Label notesHeader = new Label("Notes (" + notes.size() + ")");
        notesHeader.getStyleClass().add("label-section");
        content.getChildren().addAll(new Separator(), notesHeader);

        if (notes.isEmpty()) {
            Label noNotes = new Label("No notes");
            noNotes.getStyleClass().add("label-muted");
            content.getChildren().add(noNotes);
        } else {
            for (Note note : notes) {
                Label noteLabel = new Label("📝 " + note.getTitle());
                content.getChildren().add(noteLabel);
            }
        }

        // Activity section
        List<ActivityEntry> activity = activityService.getProjectActivity(project.getId(), 10);
        Label actHeader = new Label("Recent Activity");
        actHeader.getStyleClass().add("label-section");
        content.getChildren().addAll(new Separator(), actHeader);

        if (activity.isEmpty()) {
            Label noAct = new Label("No activity");
            noAct.getStyleClass().add("label-muted");
            content.getChildren().add(noAct);
        } else {
            for (ActivityEntry entry : activity) {
                Label actLabel = new Label(entry.getAction().getDisplayName()
                        + " — " + (entry.getSummary() != null ? entry.getSummary() : ""));
                actLabel.getStyleClass().add("label-small");
                content.getChildren().add(actLabel);
            }
        }

        // CLI hint
        content.getChildren().add(new Separator());
        Label cliLabel = new Label("CLI: cmf project show " + project.getName());
        cliLabel.getStyleClass().add("command-preview");
        content.getChildren().add(cliLabel);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(500);
        dialog.getDialogPane().setContent(scroll);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.showAndWait();
    }
}
