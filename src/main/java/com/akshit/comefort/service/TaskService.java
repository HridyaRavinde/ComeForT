package com.akshit.comefort.service;

import com.akshit.comefort.core.Task;
import com.akshit.comefort.core.enums.ActionType;
import com.akshit.comefort.core.enums.EntityType;
import com.akshit.comefort.core.enums.TaskPriority;
import com.akshit.comefort.core.enums.TaskStatus;
import com.akshit.comefort.exception.AmbiguousEntityException;
import com.akshit.comefort.exception.EntityNotFoundException;
import com.akshit.comefort.repository.TaskRepository;
import com.akshit.comefort.util.IdGenerator;

import java.time.LocalDate;
import java.util.List;

/**
 * Business logic for managing tasks.
 */
public class TaskService {

    private final TaskRepository taskRepository;
    private final ActivityService activityService;

    public TaskService(TaskRepository taskRepository, ActivityService activityService) {
        this.taskRepository = taskRepository;
        this.activityService = activityService;
    }

    /**
     * Creates a new task.
     */
    public Task create(String title, String projectId,
                       TaskPriority priority, LocalDate dueDate) {
        Task task = Task.builder(IdGenerator.generate(), title)
                .projectId(projectId)
                .priority(priority != null ? priority : TaskPriority.MEDIUM)
                .dueDate(dueDate)
                .build();

        taskRepository.save(task);

        activityService.log(EntityType.TASK, task.getId(),
                ActionType.CREATED, "Created task: " + title);

        return task;
    }

    /**
     * Marks a task as done.
     * Accepts a full ID, short ID prefix, or title fragment.
     */
    public Task complete(String idOrFragment) {
        Task task = resolve(idOrFragment);
        task.setStatus(TaskStatus.DONE);
        taskRepository.update(task);

        activityService.log(EntityType.TASK, task.getId(),
                ActionType.COMPLETED, "Completed task: " + task.getTitle());

        return task;
    }

    /**
     * Updates a task's fields. Only non-null values are applied.
     */
    public Task update(String id, String title, String description,
                       TaskPriority priority, LocalDate dueDate, boolean clearDueDate,
                       TaskStatus status, String projectId) {
        Task task = getById(id);

        if (title != null && !title.isBlank()) task.setTitle(title);
        if (description != null) task.setDescription(description);
        if (priority != null) task.setPriority(priority);
        if (clearDueDate) {
            task.setDueDate(null);
        } else if (dueDate != null) {
            task.setDueDate(dueDate);
        }
        if (status != null) task.setStatus(status);
        if (projectId != null) task.setProjectId(projectId);

        taskRepository.update(task);

        activityService.log(EntityType.TASK, task.getId(),
                ActionType.UPDATED, "Updated task: " + task.getTitle());

        return task;
    }

    /**
     * Overload for backward compatibility.
     */
    public Task update(String id, String title, String description,
                       TaskPriority priority, LocalDate dueDate,
                       TaskStatus status, String projectId) {
        return update(id, title, description, priority, dueDate, false, status, projectId);
    }

    /**
     * Deletes a task.
     */
    public void delete(String idOrFragment) {
        Task task = resolve(idOrFragment);
        taskRepository.delete(task.getId());

        activityService.log(EntityType.TASK, task.getId(),
                ActionType.DELETED, "Deleted task: " + task.getTitle());
    }

    /**
     * Finds a task by ID.
     */
    public Task getById(String id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Task", id));
    }

    /**
     * Resolves a task by full ID, short ID prefix, or title fragment.
     */
    public Task resolve(String idOrFragment) {
        // Try full ID
        var byId = taskRepository.findById(idOrFragment);
        if (byId.isPresent()) return byId.get();

        // Try ID prefix
        var byPrefix = taskRepository.findByIdPrefix(idOrFragment);
        if (byPrefix.size() == 1) return byPrefix.getFirst();
        if (byPrefix.size() > 1) {
            List<String> candidates = byPrefix.stream()
                    .map(t -> String.format("%s - %s", t.getId().substring(0, 8), t.getTitle()))
                    .toList();
            throw new AmbiguousEntityException("Task", idOrFragment, candidates);
        }

        // Try title fragment
        var byTitle = taskRepository.findByTitleFragment(idOrFragment);
        if (byTitle.isEmpty()) {
            throw new EntityNotFoundException("Task", idOrFragment);
        }
        if (byTitle.size() > 1) {
            List<String> candidates = byTitle.stream()
                    .map(t -> String.format("%s - %s", t.getId().substring(0, 8), t.getTitle()))
                    .toList();
            throw new AmbiguousEntityException("Task", idOrFragment, candidates);
        }
        return byTitle.getFirst();
    }

    /**
     * Lists all tasks with optional filters.
     */
    public List<Task> listAll(String projectId, TaskStatus status, TaskPriority priority) {
        List<Task> tasks;

        if (projectId != null) {
            tasks = taskRepository.findByProjectId(projectId);
        } else if (status != null) {
            tasks = taskRepository.findByStatus(status);
        } else {
            tasks = taskRepository.findAll();
        }

        // Apply additional filters in memory for combined filtering
        if (priority != null) {
            tasks = tasks.stream()
                    .filter(t -> t.getPriority() == priority)
                    .toList();
        }
        if (status != null && projectId != null) {
            tasks = tasks.stream()
                    .filter(t -> t.getStatus() == status)
                    .toList();
        }

        return tasks;
    }

    /**
     * Returns tasks relevant for the "today" view:
     * overdue + due today + high priority open tasks.
     */
    public List<Task> getTodayTasks() {
        return taskRepository.findDueOnOrBefore(LocalDate.now());
    }

    /**
     * Returns high-priority open tasks for the today view.
     */
    public List<Task> getHighPriorityOpen() {
        return taskRepository.findHighPriorityOpen();
    }

    /**
     * Returns task counts by status.
     */
    public int countByStatus(TaskStatus status) {
        return taskRepository.countByStatus(status);
    }

    /**
     * Returns total task count.
     */
    public int count() {
        return taskRepository.count();
    }
}
