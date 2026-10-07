package com.akshit.comefort.gui.screens;

import com.akshit.comefort.core.Note;
import com.akshit.comefort.gui.components.CommandFormBuilder;
import com.akshit.comefort.service.NoteService;
import com.akshit.comefort.service.ProjectService;
import com.akshit.comefort.util.DateTimeUtil;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;

/**
 * Notes screen — GUI equivalent of:
 *   - cf note add <title> [-p project] [-c content]
 *   - cf note list [-p project]
 *   - cf note show <id|title-fragment>
 *   - cf note edit <id> [-t title] [-c content] [-p project]
 *
 * Every flag and parameter has a GUI control.
 */
public class NotesScreen {

    private final NoteService noteService;
    private final ProjectService projectService;
    private final Runnable onDataChanged;

    public NotesScreen(NoteService noteService, ProjectService projectService,
                       Runnable onDataChanged) {
        this.noteService = noteService;
        this.projectService = projectService;
        this.onDataChanged = onDataChanged;
    }

    public Region getView() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(32, 40, 32, 40));

        // Header
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("📝 Notes");
        title.getStyleClass().add("page-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button addBtn = new Button("+ New Note");
        addBtn.getStyleClass().add("btn-primary");
        addBtn.setOnAction(e -> showAddNoteDialog());
        header.getChildren().addAll(title, spacer, addBtn);
        root.getChildren().add(header);

        // Filter by project — CLI: cf note list -p <project>
        HBox filterBar = new HBox(12);
        filterBar.setAlignment(Pos.CENTER_LEFT);
        Label filterLabel = new Label("Filter:");
        filterLabel.getStyleClass().add("form-label");
        ComboBox<String> projectFilter = new ComboBox<>();
        projectFilter.setPromptText("All Projects");
        projectFilter.getItems().add("All");
        projectService.listAll().forEach(p -> projectFilter.getItems().add(p.getName()));
        projectFilter.setTooltip(new Tooltip("CLI: cf note list -p <project>"));
        Button applyBtn = new Button("Apply");
        applyBtn.getStyleClass().add("btn-secondary");
        applyBtn.setOnAction(e -> {
            String projName = projectFilter.getValue();
            List<Note> filtered;
            if (projName != null && !"All".equals(projName)) {
                try {
                    String projectId = projectService.resolve(projName).getId();
                    filtered = noteService.listByProject(projectId);
                } catch (Exception ex) {
                    filtered = noteService.listAll();
                }
            } else {
                filtered = noteService.listAll();
            }
            // Rebuild list
            root.getChildren().removeIf(n -> n.getStyleClass().contains("card")
                    || n.getStyleClass().contains("empty-state"));
            if (filtered.isEmpty()) {
                VBox empty = new VBox(8);
                empty.getStyleClass().add("empty-state");
                Label emptyTitle = new Label("No notes found");
                emptyTitle.getStyleClass().add("empty-title");
                empty.getChildren().add(emptyTitle);
                root.getChildren().add(empty);
            } else {
                for (Note note : filtered) {
                    root.getChildren().add(buildNoteCard(note));
                }
            }
        });
        filterBar.getChildren().addAll(filterLabel, projectFilter, applyBtn);
        root.getChildren().add(filterBar);

        // CLI hint
        Label cliHint = new Label("CLI: cf note list [-p project]  |  cf note add <title> -p project -c \"content\"");
        cliHint.getStyleClass().add("page-subtitle");
        root.getChildren().add(cliHint);

        // Note list
        List<Note> notes = noteService.listAll();
        if (notes.isEmpty()) {
            VBox empty = new VBox(8);
            empty.getStyleClass().add("empty-state");
            Label emptyTitle = new Label("No notes yet");
            emptyTitle.getStyleClass().add("empty-title");
            Label emptySub = new Label("Create your first note with the button above");
            emptySub.getStyleClass().add("empty-subtitle");
            empty.getChildren().addAll(emptyTitle, emptySub);
            root.getChildren().add(empty);
        } else {
            for (Note note : notes) {
                root.getChildren().add(buildNoteCard(note));
            }
        }

        return root;
    }

    /**
     * Builds a note card — clickable to show detail (cf note show).
     */
    private VBox buildNoteCard(Note note) {
        VBox card = new VBox(6);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(12, 16, 12, 16));

        HBox titleRow = new HBox(8);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label(note.getTitle());
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        // Project label
        Label projLabel = new Label();
        if (note.getProjectId() != null) {
            try {
                projLabel.setText(projectService.getById(note.getProjectId()).getName());
            } catch (Exception ignored) {
            }
        }
        projLabel.getStyleClass().add("task-meta");

        // Updated time
        Label timeLabel = new Label(DateTimeUtil.relativeTime(note.getUpdatedAt()));
        timeLabel.getStyleClass().add("label-muted");

        // View button — CLI: cf note show <id>
        Button viewBtn = new Button("👁");
        viewBtn.getStyleClass().add("btn-ghost");
        viewBtn.setTooltip(new Tooltip("CLI: cf note show " + note.getShortId()));
        viewBtn.setOnAction(e -> showNoteDetail(note));

        // Edit button — CLI: cf note edit <id>
        Button editBtn = new Button("✏");
        editBtn.getStyleClass().add("btn-ghost");
        editBtn.setTooltip(new Tooltip("CLI: cf note edit " + note.getShortId()));
        editBtn.setOnAction(e -> showEditNoteDialog(note));

        titleRow.getChildren().addAll(titleLabel, projLabel, timeLabel, viewBtn, editBtn);
        card.getChildren().add(titleRow);

        // Content preview
        if (!note.getContentPreview().isEmpty()) {
            Label contentLabel = new Label(note.getContentPreview());
            contentLabel.getStyleClass().add("label-muted");
            card.getChildren().add(contentLabel);
        }

        return card;
    }

    /**
     * Add Note dialog — GUI equivalent of:
     *   cf note add <title> [-p project] [-c content]
     */
    private void showAddNoteDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Add Note");
        dialog.setHeaderText("Create a new note");

        List<String> projectNames = projectService.listAll().stream()
                .map(p -> p.getName()).toList();

        CommandFormBuilder form = new CommandFormBuilder("cf note add")
                .addTextParam("title", "Note Title", "e.g., Sepolia Architecture",
                        "<title> (positional, required)", true)
                .addChoiceOption("project", "Project", projectNames, null,
                        "-p", "-p <project-name>")
                .addTextAreaParam("content", "Content", "Note content...",
                        "-c", "-c \"content\"");

        form.onExecute(cmd -> {
            String titleVal = form.getFieldValueByName("title");
            String content = form.getFieldValueByName("content");
            String projectName = form.getFieldValueByName("project");

            if (titleVal == null || titleVal.isBlank()) {
                showAlert("Note title is required.");
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

            try {
                noteService.create(titleVal, content != null ? content : "", projectId);
                dialog.close();
                onDataChanged.run();
            } catch (Exception ex) {
                showAlert(ex.getMessage());
            }
        });

        VBox formContent = form.build();
        formContent.setPadding(new Insets(16));
        formContent.setPrefWidth(520);
        dialog.getDialogPane().setContent(formContent);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().lookupButton(ButtonType.CLOSE).setVisible(false);
        dialog.showAndWait();
    }

    /**
     * Show note detail — GUI equivalent of `cf note show <id|title>`.
     */
    private void showNoteDetail(Note note) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(note.getTitle());

        VBox content = new VBox(12);
        content.setPadding(new Insets(16));
        content.setPrefWidth(520);

        Label titleLabel = new Label(note.getTitle());
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        if (note.getProjectId() != null) {
            try {
                Label proj = new Label("Project: "
                        + projectService.getById(note.getProjectId()).getName());
                content.getChildren().add(proj);
            } catch (Exception ignored) {
            }
        }

        Label created = new Label("Created: " + DateTimeUtil.relativeTime(note.getCreatedAt()));
        created.getStyleClass().add("label-muted");
        Label updated = new Label("Updated: " + DateTimeUtil.relativeTime(note.getUpdatedAt()));
        updated.getStyleClass().add("label-muted");

        TextArea contentArea = new TextArea(note.getContent());
        contentArea.setEditable(false);
        contentArea.setWrapText(true);
        contentArea.setPrefRowCount(10);

        Label cliLabel = new Label("CLI: cf note show " + note.getShortId());
        cliLabel.getStyleClass().add("command-preview");

        content.getChildren().addAll(titleLabel, created, updated,
                new Separator(), contentArea, new Separator(), cliLabel);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(450);
        dialog.getDialogPane().setContent(scroll);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    /**
     * Edit note dialog — GUI equivalent of:
     *   cf note edit <id> [-t title] [-c content] [-p project]
     */
    private void showEditNoteDialog(Note note) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Edit Note");
        dialog.setHeaderText("Edit: " + note.getTitle());

        List<String> projectNames = projectService.listAll().stream()
                .map(p -> p.getName()).toList();

        CommandFormBuilder form = new CommandFormBuilder("cf note edit " + note.getShortId())
                .addTextOption("title", "Title", note.getTitle(),
                        "-t", "-t \"new title\"")
                .addTextAreaParam("content", "Content", note.getContent(),
                        "-c", "-c \"new content\"")
                .addChoiceOption("project", "Move to Project", projectNames,
                        null, "-p", "-p <project-name>");

        form.onExecute(cmd -> {
            String titleVal = form.getFieldValueByName("title");
            String content = form.getFieldValueByName("content");
            String projStr = form.getFieldValueByName("project");

            String projectId = null;
            if (projStr != null && !projStr.isBlank()) {
                try {
                    projectId = projectService.resolve(projStr).getId();
                } catch (Exception ignored) {
                }
            }

            try {
                noteService.update(note.getId(), titleVal, content, projectId);
                dialog.close();
                onDataChanged.run();
            } catch (Exception ex) {
                showAlert(ex.getMessage());
            }
        });

        VBox formContent = form.build();
        formContent.setPadding(new Insets(16));
        formContent.setPrefWidth(520);
        dialog.getDialogPane().setContent(formContent);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().lookupButton(ButtonType.CLOSE).setVisible(false);
        dialog.showAndWait();
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.showAndWait();
    }
}
