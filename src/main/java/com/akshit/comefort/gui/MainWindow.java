package com.akshit.comefort.gui;

import com.akshit.comefort.AppContext;
import com.akshit.comefort.db.DatabaseManager;
import com.akshit.comefort.gui.screens.*;
import com.akshit.comefort.repository.*;
import com.akshit.comefort.service.*;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Main JavaFX application for ComeFort GUI.
 * Implements the sidebar navigation layout with Notion-inspired theme.
 *
 * <p>Uses the same Service layer as the CLI — both interfaces are
 * thin wrappers over the shared core engine.</p>
 */
public class MainWindow extends Application {

    // --- Infrastructure ---
    private DatabaseManager dbManager;

    // --- Services (same instances as CLI would use) ---
    private ActivityService activityService;
    private ProjectService projectService;
    private TaskService taskService;
    private NoteService noteService;
    private CaptureService captureService;
    private SearchService searchService;
    private TodayService todayService;

    // --- UI components ---
    private BorderPane rootLayout;
    private VBox sidebar;
    private StackPane contentPane;
    private Theme.Mode currentTheme = Theme.Mode.DARK;
    private final Map<String, Label> navItems = new LinkedHashMap<>();
    private String activeNavId = "today";

    // --- Screens ---
    private TodayScreen todayScreen;
    private ProjectsScreen projectsScreen;
    private TasksScreen tasksScreen;
    private NotesScreen notesScreen;
    private InboxScreen inboxScreen;
    private SearchScreen searchScreen;
    private StatusScreen statusScreen;

    @Override
    public void start(Stage primaryStage) {
        initializeServices();
        initializeScreens();
        buildUI(primaryStage);

        primaryStage.setTitle("ComeFort — Developer Life OS");
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);
        primaryStage.setWidth(1200);
        primaryStage.setHeight(780);

        Scene scene = new Scene(rootLayout);
        applyTheme(scene);
        primaryStage.setScene(scene);
        primaryStage.show();

        // Show today screen by default
        navigateTo("today");
    }

    /**
     * Wire services from the shared AppContext (same composition root as CLI).
     */
    private void initializeServices() {
        AppContext context = AppContext.getInstance();
        context.ensureInitialized();

        dbManager = context.getDbManager();
        activityService = context.getActivityService();
        projectService = context.getProjectService();
        taskService = context.getTaskService();
        noteService = context.getNoteService();
        captureService = context.getCaptureService();
        searchService = context.getSearchService();
        todayService = context.getTodayService();
    }

    private void initializeScreens() {
        todayScreen = new TodayScreen(todayService, projectService, taskService,
                captureService, this::navigateTo);
        projectsScreen = new ProjectsScreen(projectService, taskService, noteService,
                activityService, this::refreshCurrentScreen);
        tasksScreen = new TasksScreen(taskService, projectService, this::refreshCurrentScreen);
        notesScreen = new NotesScreen(noteService, projectService, this::refreshCurrentScreen);
        inboxScreen = new InboxScreen(captureService, projectService, this::refreshCurrentScreen);
        searchScreen = new SearchScreen(searchService, projectService);
        statusScreen = new StatusScreen(projectService, taskService, noteService,
                captureService, activityService);
    }

    /**
     * Builds the main layout: sidebar + content area.
     */
    private void buildUI(Stage stage) {
        rootLayout = new BorderPane();

        // Build sidebar
        sidebar = buildSidebar(stage);
        rootLayout.setLeft(sidebar);

        // Content pane (screens swap here)
        contentPane = new StackPane();
        contentPane.getStyleClass().add("content-area");

        ScrollPane scrollPane = new ScrollPane(contentPane);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background-color: transparent;");
        rootLayout.setCenter(scrollPane);
    }

    /**
     * Builds the sidebar with navigation items.
     */
    private VBox buildSidebar(Stage stage) {
        VBox sidebarBox = new VBox();
        sidebarBox.getStyleClass().add("sidebar");

        // Brand
        Label brand = new Label("◈ ComeFort");
        brand.getStyleClass().add("brand");
        sidebarBox.getChildren().add(brand);

        // Main navigation
        addNavItem(sidebarBox, "today", "🔥", "Today", "cmf today");
        addNavItem(sidebarBox, "inbox", "📥", "Inbox", "cmf inbox");

        // Section: Manage
        addSectionLabel(sidebarBox, "MANAGE");
        addNavItem(sidebarBox, "projects", "🚀", "Projects", "cmf project list");
        addNavItem(sidebarBox, "tasks", "☐", "Tasks", "cmf task list");
        addNavItem(sidebarBox, "notes", "📝", "Notes", "cmf note list");

        // Section: Tools
        addSectionLabel(sidebarBox, "TOOLS");
        addNavItem(sidebarBox, "search", "🔎", "Search", "cmf search <query>");

        // Spacer
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        sidebarBox.getChildren().add(spacer);

        // Bottom section
        sidebarBox.getChildren().add(new Separator());
        addNavItem(sidebarBox, "status", "📊", "Status", "cmf status");

        // Theme toggle
        Label themeToggle = new Label("🌙 Dark Mode");
        themeToggle.getStyleClass().addAll("nav-item");
        themeToggle.setMaxWidth(Double.MAX_VALUE);
        themeToggle.setOnMouseClicked(e -> {
            currentTheme = (currentTheme == Theme.Mode.DARK)
                    ? Theme.Mode.LIGHT : Theme.Mode.DARK;
            applyTheme(stage.getScene());
            themeToggle.setText(currentTheme == Theme.Mode.DARK
                    ? "🌙 Dark Mode" : "☀ Light Mode");
        });
        sidebarBox.getChildren().add(themeToggle);

        return sidebarBox;
    }

    /**
     * Adds a navigation item to the sidebar.
     */
    private void addNavItem(VBox sidebar, String id, String icon, String label,
                            String cliEquivalent) {
        Label navItem = new Label(icon + "  " + label);
        navItem.getStyleClass().add("nav-item");
        navItem.setMaxWidth(Double.MAX_VALUE);
        navItem.setTooltip(new Tooltip("CLI: " + cliEquivalent));
        navItem.setOnMouseClicked(e -> navigateTo(id));
        sidebar.getChildren().add(navItem);
        navItems.put(id, navItem);
    }

    /**
     * Adds a section label to the sidebar.
     */
    private void addSectionLabel(VBox sidebar, String text) {
        Label label = new Label(text);
        label.getStyleClass().add("section-label");
        sidebar.getChildren().add(label);
    }

    /**
     * Navigates to a screen, updating the sidebar active state.
     */
    public void navigateTo(String screenId) {
        // Update sidebar active state
        navItems.forEach((id, item) -> {
            if (id.equals(screenId)) {
                if (!item.getStyleClass().contains("active")) {
                    item.getStyleClass().add("active");
                }
            } else {
                item.getStyleClass().remove("active");
            }
        });

        activeNavId = screenId;

        // Swap content
        contentPane.getChildren().clear();
        Region screen = switch (screenId) {
            case "today" -> todayScreen.getView();
            case "inbox" -> inboxScreen.getView();
            case "projects" -> projectsScreen.getView();
            case "tasks" -> tasksScreen.getView();
            case "notes" -> notesScreen.getView();
            case "search" -> searchScreen.getView();
            case "status" -> statusScreen.getView();
            default -> todayScreen.getView();
        };
        contentPane.getChildren().add(screen);
    }

    /**
     * Refreshes the currently active screen (after data changes).
     */
    private void refreshCurrentScreen() {
        navigateTo(activeNavId);
    }

    /**
     * Applies the selected theme to the scene.
     */
    private void applyTheme(Scene scene) {
        scene.getStylesheets().clear();
        String baseCss = getClass().getResource(Theme.getBaseStylesheet()).toExternalForm();
        String themeCss = getClass().getResource(Theme.getStylesheet(currentTheme)).toExternalForm();
        scene.getStylesheets().addAll(baseCss, themeCss);
    }

    @Override
    public void stop() {
        if (dbManager != null) {
            dbManager.close();
        }
    }

    /**
     * Launches the GUI. Called from App.java when --gui flag is used.
     */
    public static void launchGui(String[] args) {
        launch(args);
    }
}
