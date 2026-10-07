package com.akshit.comefort;

import com.akshit.comefort.db.DatabaseManager;
import com.akshit.comefort.repository.*;
import com.akshit.comefort.service.*;

/**
 * Single Composition Root for ComeFort.
 * Both CLI and JavaFX GUI share this context and its underlying
 * single SQLite database connection, avoiding redundant DatabaseManager instances.
 */
public class AppContext {

    private static AppContext instance;

    private final DatabaseManager dbManager;

    // Repositories
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final NoteRepository noteRepository;
    private final CaptureRepository captureRepository;
    private final ActivityRepository activityRepository;
    private final ConfigRepository configRepository;

    // Services
    private final ActivityService activityService;
    private final ProjectService projectService;
    private final TaskService taskService;
    private final NoteService noteService;
    private final CaptureService captureService;
    private final SearchService searchService;
    private final TodayService todayService;

    public AppContext() {
        this(new DatabaseManager());
    }

    public AppContext(DatabaseManager dbManager) {
        this.dbManager = dbManager;

        // Initialize repositories
        this.projectRepository = new ProjectRepository(dbManager);
        this.taskRepository = new TaskRepository(dbManager);
        this.noteRepository = new NoteRepository(dbManager);
        this.captureRepository = new CaptureRepository(dbManager);
        this.activityRepository = new ActivityRepository(dbManager);
        this.configRepository = new ConfigRepository(dbManager);

        // Initialize services
        this.activityService = new ActivityService(activityRepository);
        this.projectService = new ProjectService(projectRepository, activityService);
        this.taskService = new TaskService(taskRepository, activityService);
        this.noteService = new NoteService(noteRepository, activityService);
        this.captureService = new CaptureService(captureRepository, activityService, taskService, noteService);
        this.searchService = new SearchService(taskRepository, projectRepository, noteRepository, captureRepository);
        this.todayService = new TodayService(taskService, captureService, activityService);
    }

    public static synchronized AppContext getInstance() {
        if (instance == null) {
            instance = new AppContext();
        }
        return instance;
    }

    public static synchronized void setInstance(AppContext customContext) {
        instance = customContext;
    }

    public synchronized void ensureInitialized() {
        if (!dbManager.isDatabaseInitialized()) {
            dbManager.initialize();
        }
    }

    public synchronized void close() {
        dbManager.close();
    }

    // --- Accessors ---

    public DatabaseManager getDbManager() {
        return dbManager;
    }

    public ProjectRepository getProjectRepository() {
        return projectRepository;
    }

    public TaskRepository getTaskRepository() {
        return taskRepository;
    }

    public NoteRepository getNoteRepository() {
        return noteRepository;
    }

    public CaptureRepository getCaptureRepository() {
        return captureRepository;
    }

    public ActivityRepository getActivityRepository() {
        return activityRepository;
    }

    public ConfigRepository getConfigRepository() {
        return configRepository;
    }

    public ActivityService getActivityService() {
        return activityService;
    }

    public ProjectService getProjectService() {
        return projectService;
    }

    public TaskService getTaskService() {
        return taskService;
    }

    public NoteService getNoteService() {
        return noteService;
    }

    public CaptureService getCaptureService() {
        return captureService;
    }

    public SearchService getSearchService() {
        return searchService;
    }

    public TodayService getTodayService() {
        return todayService;
    }
}
