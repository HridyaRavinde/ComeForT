package com.akshit.comefort.gui.screens;

import com.akshit.comefort.core.*;
import com.akshit.comefort.service.ProjectService;
import com.akshit.comefort.service.SearchService;
import com.akshit.comefort.service.SearchService.SearchResults;
import com.akshit.comefort.util.DateTimeUtil;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * Search screen — GUI equivalent of:
 *   cf search <query>
 *
 * Searches across tasks, projects, notes, and captures with categorized results.
 */
public class SearchScreen {

    private final SearchService searchService;
    private final ProjectService projectService;

    public SearchScreen(SearchService searchService, ProjectService projectService) {
        this.searchService = searchService;
        this.projectService = projectService;
    }

    public Region getView() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(32, 40, 32, 40));

        // Header
        Label title = new Label("🔎 Search");
        title.getStyleClass().add("page-title");
        root.getChildren().add(title);

        // Search bar — CLI: cf search <query>
        TextField searchField = new TextField();
        searchField.getStyleClass().add("quick-capture");
        searchField.setPromptText("Search across everything... (CLI: cf search <query>)");

        // Results container
        VBox resultsContainer = new VBox(12);

        searchField.setOnAction(e -> {
            String query = searchField.getText().trim();
            if (!query.isEmpty()) {
                resultsContainer.getChildren().clear();
                performSearch(query, resultsContainer);
            }
        });

        // Search button
        HBox searchBar = new HBox(8);
        searchBar.setAlignment(Pos.CENTER_LEFT);
        Button searchBtn = new Button("Search");
        searchBtn.getStyleClass().add("btn-primary");
        searchBtn.setOnAction(e -> {
            String query = searchField.getText().trim();
            if (!query.isEmpty()) {
                resultsContainer.getChildren().clear();
                performSearch(query, resultsContainer);
            }
        });
        searchBar.getChildren().addAll(searchField, searchBtn);
        HBox.setHgrow(searchField, Priority.ALWAYS);
        root.getChildren().add(searchBar);

        // CLI hint
        Label cliHint = new Label("CLI: cf search <query>  — searches across tasks, projects, notes, and captures");
        cliHint.getStyleClass().add("page-subtitle");
        root.getChildren().add(cliHint);

        root.getChildren().add(resultsContainer);
        return root;
    }

    /**
     * Performs search and displays categorized results.
     */
    private void performSearch(String query, VBox container) {
        SearchResults results = searchService.search(query);

        if (results.isEmpty()) {
            VBox empty = new VBox(8);
            empty.getStyleClass().add("empty-state");
            Label emptyTitle = new Label("No results for \"" + query + "\"");
            emptyTitle.getStyleClass().add("empty-title");
            empty.getChildren().add(emptyTitle);
            container.getChildren().add(empty);
            return;
        }

        Label resultCount = new Label(results.totalCount() + " results for \"" + query + "\"");
        resultCount.getStyleClass().add("label-section");
        container.getChildren().add(resultCount);

        // CLI preview
        Label cliPreview = new Label("cf search " + (query.contains(" ")
                ? "\"" + query + "\"" : query));
        cliPreview.getStyleClass().add("command-preview");
        container.getChildren().add(cliPreview);

        // Projects
        if (!results.projects().isEmpty()) {
            Label header = new Label("PROJECTS (" + results.projects().size() + ")");
            header.getStyleClass().add("label-section");
            container.getChildren().add(header);
            for (Project p : results.projects()) {
                HBox row = new HBox(8);
                row.setPadding(new Insets(4, 0, 4, 16));
                Label nameLabel = new Label("🚀 " + p.getName());
                nameLabel.setStyle("-fx-font-weight: bold;");
                Label descLabel = new Label(p.getDescription() != null ? p.getDescription() : "");
                descLabel.getStyleClass().add("label-muted");
                row.getChildren().addAll(nameLabel, descLabel);
                container.getChildren().add(row);
            }
        }

        // Tasks
        if (!results.tasks().isEmpty()) {
            Label header = new Label("TASKS (" + results.tasks().size() + ")");
            header.getStyleClass().add("label-section");
            container.getChildren().add(header);
            for (Task t : results.tasks()) {
                HBox row = new HBox(8);
                row.setPadding(new Insets(4, 0, 4, 16));
                row.setAlignment(Pos.CENTER_LEFT);
                Label statusIcon = new Label(t.getStatus().getIcon());
                Label prioLabel = new Label(t.getPriority().getDisplayName());
                prioLabel.getStyleClass().addAll("badge",
                        "badge-" + t.getPriority().name().toLowerCase());
                Label titleLabel = new Label(t.getTitle());
                Label projLabel = new Label();
                if (t.getProjectId() != null) {
                    try {
                        projLabel.setText(projectService.getById(t.getProjectId()).getName());
                    } catch (Exception ignored) {
                    }
                }
                projLabel.getStyleClass().add("label-muted");
                row.getChildren().addAll(statusIcon, prioLabel, titleLabel, projLabel);
                container.getChildren().add(row);
            }
        }

        // Notes
        if (!results.notes().isEmpty()) {
            Label header = new Label("NOTES (" + results.notes().size() + ")");
            header.getStyleClass().add("label-section");
            container.getChildren().add(header);
            for (Note n : results.notes()) {
                HBox row = new HBox(8);
                row.setPadding(new Insets(4, 0, 4, 16));
                Label titleLabel = new Label("📝 " + n.getTitle());
                titleLabel.setStyle("-fx-font-weight: bold;");
                Label preview = new Label(n.getContentPreview());
                preview.getStyleClass().add("label-muted");
                row.getChildren().addAll(titleLabel, preview);
                container.getChildren().add(row);
            }
        }

        // Captures
        if (!results.captures().isEmpty()) {
            Label header = new Label("CAPTURES (" + results.captures().size() + ")");
            header.getStyleClass().add("label-section");
            container.getChildren().add(header);
            for (Capture c : results.captures()) {
                HBox row = new HBox(8);
                row.setPadding(new Insets(4, 0, 4, 16));
                Label icon = new Label(c.getType().getIcon());
                Label contentLabel = new Label(c.getContent());
                Label timeLabel = new Label(DateTimeUtil.relativeTime(c.getCreatedAt()));
                timeLabel.getStyleClass().add("label-muted");
                row.getChildren().addAll(icon, contentLabel, timeLabel);
                container.getChildren().add(row);
            }
        }
    }
}
