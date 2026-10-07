package com.akshit.comefort.service;

import com.akshit.comefort.core.Task;
import com.akshit.comefort.core.enums.TaskPriority;
import com.akshit.comefort.core.enums.TaskStatus;
import com.akshit.comefort.db.DatabaseManager;
import com.akshit.comefort.exception.EntityNotFoundException;
import com.akshit.comefort.repository.ActivityRepository;
import com.akshit.comefort.repository.TaskRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TaskService Integration Tests")
class TaskServiceTest {

    private DatabaseManager dbManager;
    private TaskService taskService;
    private ActivityService activityService;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        dbManager = new DatabaseManager(tempDir);
        dbManager.initialize();

        TaskRepository taskRepository = new TaskRepository(dbManager);
        ActivityRepository activityRepository = new ActivityRepository(dbManager);
        activityService = new ActivityService(activityRepository);
        taskService = new TaskService(taskRepository, activityService);
    }

    @AfterEach
    void tearDown() {
        if (dbManager != null) {
            dbManager.close();
        }
    }

    @Test
    @DisplayName("Create task and fetch by ID")
    void testCreateAndGet() {
        Task task = taskService.create("Finish architecture doc", null,
                TaskPriority.HIGH, LocalDate.now().plusDays(2));

        assertNotNull(task.getId());
        assertEquals("Finish architecture doc", task.getTitle());
        assertEquals(TaskPriority.HIGH, task.getPriority());
        assertEquals(TaskStatus.OPEN, task.getStatus());

        Task fetched = taskService.getById(task.getId());
        assertEquals(task.getTitle(), fetched.getTitle());
    }

    @Test
    @DisplayName("Complete task changes status to DONE")
    void testCompleteTask() {
        Task task = taskService.create("Fix memory leak", null,
                TaskPriority.CRITICAL, null);
        assertEquals(TaskStatus.OPEN, task.getStatus());

        Task completed = taskService.complete(task.getShortId());
        assertEquals(TaskStatus.DONE, completed.getStatus());
        assertNotNull(completed.getCompletedAt());
    }

    @Test
    @DisplayName("Resolve task by title fragment")
    void testResolveByFragment() {
        taskService.create("Prepare SIH submission", null, TaskPriority.MEDIUM, null);

        Task resolved = taskService.resolve("submission");
        assertEquals("Prepare SIH submission", resolved.getTitle());
    }

    @Test
    @DisplayName("Update task updates only specified fields")
    void testUpdateTask() {
        Task task = taskService.create("Initial title", null, TaskPriority.LOW, null);

        Task updated = taskService.update(task.getId(), "Updated title",
                "New description", TaskPriority.HIGH, LocalDate.now(), null, null);

        assertEquals("Updated title", updated.getTitle());
        assertEquals("New description", updated.getDescription());
        assertEquals(TaskPriority.HIGH, updated.getPriority());
    }

    @Test
    @DisplayName("Delete task removes it from database")
    void testDeleteTask() {
        Task task = taskService.create("Task to delete", null, TaskPriority.LOW, null);
        taskService.delete(task.getId());

        assertThrows(EntityNotFoundException.class, () -> taskService.getById(task.getId()));
    }

    @Test
    @DisplayName("List tasks with status and priority filters")
    void testListWithFilters() {
        taskService.create("Task 1", null, TaskPriority.LOW, null);
        taskService.create("Task 2", null, TaskPriority.HIGH, null);
        Task t3 = taskService.create("Task 3", null, TaskPriority.HIGH, null);
        taskService.complete(t3.getId());

        List<Task> highOpen = taskService.listAll(null, TaskStatus.OPEN, TaskPriority.HIGH);
        assertEquals(1, highOpen.size());
        assertEquals("Task 2", highOpen.get(0).getTitle());
    }

    @Test
    @DisplayName("Ambiguous fragment match throws AmbiguousEntityException")
    void testAmbiguousResolve() {
        taskService.create("Fix login bug", null, TaskPriority.MEDIUM, null);
        taskService.create("Fix QR bug", null, TaskPriority.MEDIUM, null);

        com.akshit.comefort.exception.AmbiguousEntityException ex =
                assertThrows(com.akshit.comefort.exception.AmbiguousEntityException.class,
                        () -> taskService.resolve("Fix"));

        assertEquals(2, ex.getCandidates().size());
    }

    @Test
    @DisplayName("Clear due date removes due date from task")
    void testClearDueDate() {
        Task task = taskService.create("Task with due date", null,
                TaskPriority.MEDIUM, LocalDate.now().plusDays(5));
        assertNotNull(task.getDueDate());

        Task updated = taskService.update(task.getId(), null, null, null, null, true, null, null);
        assertNull(updated.getDueDate());
    }

    @Test
    @DisplayName("Status transition from DONE to IN_PROGRESS resets completedAt")
    void testStatusResetCompletedAt() {
        Task task = taskService.create("Completed task test", null, TaskPriority.MEDIUM, null);
        taskService.complete(task.getId());

        Task completed = taskService.getById(task.getId());
        assertEquals(TaskStatus.DONE, completed.getStatus());
        assertNotNull(completed.getCompletedAt());

        taskService.update(task.getId(), null, null, null, null, false, TaskStatus.IN_PROGRESS, null);
        Task reopened = taskService.getById(task.getId());
        assertEquals(TaskStatus.IN_PROGRESS, reopened.getStatus());
        assertNull(reopened.getCompletedAt());
    }
}
