package com.akshit.comefort.gui;

import com.akshit.comefort.AppContext;
import com.akshit.comefort.db.DatabaseManager;
import com.akshit.comefort.gui.screens.*;
import com.akshit.comefort.repository.*;
import com.akshit.comefort.service.*;
import com.akshit.comefort.gui.components.TerminalPanel;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
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
    private SplitPane centerSplit;
    private TerminalPanel terminalPanel;
    private boolean terminalVisible = false;
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

    // --- Dev Live Reload ---
    private WatchService styleWatchService;
    private Thread styleWatcherThread;

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

        // Global shortcuts for integrated terminal
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.BACK_QUOTE, KeyCombination.CONTROL_DOWN),
                this::toggleTerminal
        );
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.T, KeyCombination.CONTROL_DOWN),
                this::toggleTerminal
        );

        // Real-time Dev Hot Reload (F5 or Ctrl+R): Re-applies styles & refreshes screen
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.F5),
                () -> hotReload(scene)
        );
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.R, KeyCombination.CONTROL_DOWN),
                () -> hotReload(scene)
        );

        // Start live filesystem CSS watcher for instantaneous live-reloading during development
        setupStyleWatcher(scene);

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

        // Initialize integrated terminal panel
        terminalPanel = new TerminalPanel(this::refreshCurrentScreen, this::toggleTerminal);

        // Center SplitPane allowing vertical split between screens and terminal
        centerSplit = new SplitPane();
        centerSplit.setOrientation(Orientation.VERTICAL);
        centerSplit.getItems().add(scrollPane);
        rootLayout.setCenter(centerSplit);
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
        addNavItem(sidebarBox, "terminal", "⌨", "Terminal", "Toggle Terminal (Ctrl+`)");

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
     * Toggles the integrated terminal at the bottom of the window.
     */
    public void toggleTerminal() {
        terminalVisible = !terminalVisible;
        if (terminalVisible) {
            if (!centerSplit.getItems().contains(terminalPanel.getView())) {
                centerSplit.getItems().add(terminalPanel.getView());
                centerSplit.setDividerPositions(0.68);
            }
            terminalPanel.focusInput();
            updateTerminalNavState(true);
        } else {
            centerSplit.getItems().remove(terminalPanel.getView());
            updateTerminalNavState(false);
        }
    }

    private void updateTerminalNavState(boolean open) {
        Label terminalNav = navItems.get("terminal");
        if (terminalNav != null) {
            if (open) {
                if (!terminalNav.getStyleClass().contains("active")) {
                    terminalNav.getStyleClass().add("active");
                }
            } else {
                terminalNav.getStyleClass().remove("active");
            }
        }
    }

    /**
     * Navigates to a screen, updating the sidebar active state.
     */
    public void navigateTo(String screenId) {
        if ("terminal".equals(screenId)) {
            toggleTerminal();
            return;
        }

        // Update sidebar active state
        navItems.forEach((id, item) -> {
            if ("terminal".equals(id)) {
                // Keep terminal active state matching terminalVisible
                return;
            }
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
        String baseCss = getStylesheetUrl(Theme.getBaseStylesheet());
        String themeCss = getStylesheetUrl(Theme.getStylesheet(currentTheme));
        if (baseCss != null) {
            scene.getStylesheets().add(baseCss);
        }
        if (themeCss != null) {
            scene.getStylesheets().add(themeCss);
        }
    }

    /**
     * Resolves stylesheet URL, prioritizing source files on disk during development
     * with cache-busting to enable instant hot-reload without restarting.
     */
    private String getStylesheetUrl(String resourcePath) {
        File localFile = new File("src/main/resources" + resourcePath);
        if (localFile.exists()) {
            return localFile.toURI().toString() + "#t=" + System.currentTimeMillis();
        }
        var res = getClass().getResource(resourcePath);
        return res != null ? res.toExternalForm() : null;
    }

    /**
     * Sets up a filesystem WatchService on the styles directory to automatically
     * trigger hot reload when any .css file is modified or saved during development.
     */
    private void setupStyleWatcher(Scene scene) {
        Path styleDir = Paths.get("src", "main", "resources", "styles");
        if (!Files.exists(styleDir)) {
            return;
        }

        try {
            styleWatchService = FileSystems.getDefault().newWatchService();
            styleDir.register(styleWatchService, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_CREATE);

            styleWatcherThread = new Thread(() -> {
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        WatchKey key = styleWatchService.take();
                        boolean hasCssChange = false;
                        for (WatchEvent<?> event : key.pollEvents()) {
                            Path changed = (Path) event.context();
                            if (changed.toString().endsWith(".css")) {
                                hasCssChange = true;
                            }
                        }
                        if (hasCssChange) {
                            // Debounce write burst
                            Thread.sleep(100);
                            Platform.runLater(() -> hotReload(scene));
                        }
                        if (!key.reset()) {
                            break;
                        }
                    }
                } catch (InterruptedException | ClosedWatchServiceException ignored) {
                    // Thread terminated gracefully
                }
            }, "comefort-css-watcher");
            styleWatcherThread.setDaemon(true);
            styleWatcherThread.start();
        } catch (IOException e) {
            System.err.println("[Dev] Could not start CSS watcher: " + e.getMessage());
        }
    }

    /**
     * Manually or automatically triggered hot reload: Re-applies stylesheets and
     * re-renders the current screen with fresh database queries.
     */
    public void hotReload(Scene scene) {
        try {
            applyTheme(scene);
            refreshCurrentScreen();
            System.out.println("[Dev] Hot reload applied: Styles & active screen re-rendered.");
        } catch (Exception e) {
            System.err.println("[Dev] Failed to hot reload: " + e.getMessage());
        }
    }

    @Override
    public void stop() {
        if (styleWatcherThread != null) {
            styleWatcherThread.interrupt();
        }
        if (styleWatchService != null) {
            try {
                styleWatchService.close();
            } catch (IOException ignored) {
            }
        }
        if (terminalPanel != null) {
            terminalPanel.destroy();
        }
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
