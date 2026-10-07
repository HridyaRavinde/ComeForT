package com.akshit.comefort.service;

import com.akshit.comefort.core.Task;
import com.akshit.comefort.core.enums.TaskPriority;
import com.akshit.comefort.db.DatabaseManager;
import com.akshit.comefort.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SearchService Integration Tests")
class SearchServiceTest {

    private DatabaseManager dbManager;
    private SearchService searchService;
    private TaskService taskService;
    private ProjectService projectService;
    private NoteService noteService;
    private CaptureService captureService;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        dbManager = new DatabaseManager(tempDir);
        dbManager.initialize();

        TaskRepository taskRepository = new TaskRepository(dbManager);
        ProjectRepository projectRepository = new ProjectRepository(dbManager);
        NoteRepository noteRepository = new NoteRepository(dbManager);
        CaptureRepository captureRepository = new CaptureRepository(dbManager);
        ActivityRepository activityRepository = new ActivityRepository(dbManager);

        ActivityService activityService = new ActivityService(activityRepository);
        taskService = new TaskService(taskRepository, activityService);
        projectService = new ProjectService(projectRepository, activityService);
        noteService = new NoteService(noteRepository, activityService);
        captureService = new CaptureService(captureRepository, activityService);

        searchService = new SearchService(taskRepository, projectRepository,
                noteRepository, captureRepository);
    }

    @AfterEach
    void tearDown() {
        if (dbManager != null) {
            dbManager.close();
        }
    }

    @Test
    @DisplayName("Search finds keywords across all entity types")
    void testCrossEntitySearch() {
        projectService.create("KotlinApp", "Android kotlin project", null);
        taskService.create("Learn Kotlin coroutines", null, TaskPriority.HIGH, null);
        noteService.create("Kotlin Flow notes", "StateFlow vs SharedFlow", null);
        captureService.capture("idea: Rewrite backend in Kotlin");

        SearchService.SearchResults results = searchService.search("Kotlin");
        assertEquals(4, results.totalCount());
        assertEquals(1, results.projects().size());
        assertEquals(1, results.tasks().size());
        assertEquals(1, results.notes().size());
        assertEquals(1, results.captures().size());
    }

    @Test
    @DisplayName("Search with blank query returns empty results")
    void testEmptyQuery() {
        SearchService.SearchResults results = searchService.search("   ");
        assertTrue(results.isEmpty());
        assertEquals(0, results.totalCount());
    }
}
