package com.akshit.comefort.service;

import com.akshit.comefort.core.Task;
import com.akshit.comefort.core.enums.TaskPriority;
import com.akshit.comefort.db.DatabaseManager;
import com.akshit.comefort.repository.ActivityRepository;
import com.akshit.comefort.repository.CaptureRepository;
import com.akshit.comefort.repository.TaskRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TodayService Integration Tests")
class TodayServiceTest {

    private DatabaseManager dbManager;
    private TodayService todayService;
    private TaskService taskService;
    private CaptureService captureService;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        dbManager = new DatabaseManager(tempDir);
        dbManager.initialize();

        TaskRepository taskRepository = new TaskRepository(dbManager);
        CaptureRepository captureRepository = new CaptureRepository(dbManager);
        ActivityRepository activityRepository = new ActivityRepository(dbManager);

        ActivityService activityService = new ActivityService(activityRepository);
        taskService = new TaskService(taskRepository, activityService);
        captureService = new CaptureService(captureRepository, activityService);
        todayService = new TodayService(taskService, captureService, activityService);
    }

    @AfterEach
    void tearDown() {
        if (dbManager != null) {
            dbManager.close();
        }
    }

    @Test
    @DisplayName("buildTodayView categorizes overdue, due today, and high priority tasks")
    void testBuildTodayView() {
        // Overdue task
        taskService.create("Past due task", null, TaskPriority.LOW, LocalDate.now().minusDays(2));

        // Due today task
        taskService.create("Finish today", null, TaskPriority.MEDIUM, LocalDate.now());

        // High priority task without due date
        taskService.create("High priority task", null, TaskPriority.HIGH, null);

        // Inbox item
        captureService.capture("idea: some thought");

        TodayService.TodayView view = todayService.buildTodayView();

        assertEquals(1, view.overdueTasks().size());
        assertEquals("Past due task", view.overdueTasks().get(0).getTitle());

        assertEquals(1, view.dueTodayTasks().size());
        assertEquals("Finish today", view.dueTodayTasks().get(0).getTitle());

        assertEquals(1, view.highPriorityTasks().size());
        assertEquals("High priority task", view.highPriorityTasks().get(0).getTitle());

        assertEquals(1, view.inboxCount());
    }
}
