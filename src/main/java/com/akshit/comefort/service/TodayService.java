package com.akshit.comefort.service;

import com.akshit.comefort.core.ActivityEntry;
import com.akshit.comefort.core.Task;
import com.akshit.comefort.core.enums.TaskStatus;

import java.util.List;
import java.util.stream.Stream;

/**
 * Aggregates data for the "today" dashboard view.
 * Answers the four key questions:
 * 1. What's overdue?
 * 2. What's due today?
 * 3. What's high priority?
 * 4. What did I do recently?
 */
public class TodayService {

    private final TaskService taskService;
    private final CaptureService captureService;
    private final ActivityService activityService;

    public TodayService(TaskService taskService,
                        CaptureService captureService,
                        ActivityService activityService) {
        this.taskService = taskService;
        this.captureService = captureService;
        this.activityService = activityService;
    }

    /**
     * Holds the aggregated data for the today view.
     */
    public record TodayView(
            List<Task> overdueTasks,
            List<Task> dueTodayTasks,
            List<Task> highPriorityTasks,
            int inboxCount,
            List<ActivityEntry> recentActivity
    ) {
    }

    /**
     * Builds the complete today view.
     */
    public TodayView buildTodayView() {
        List<Task> dueOrOverdue = taskService.getTodayTasks();
        List<Task> highPriority = taskService.getHighPriorityOpen();

        // Separate overdue from due today
        List<Task> overdue = dueOrOverdue.stream()
                .filter(Task::isOverdue)
                .toList();

        List<Task> dueToday = dueOrOverdue.stream()
                .filter(Task::isDueToday)
                .toList();

        // Filter high priority to exclude those already in overdue/dueToday
        var alreadyShown = Stream.concat(overdue.stream(), dueToday.stream())
                .map(Task::getId)
                .collect(java.util.stream.Collectors.toSet());

        List<Task> uniqueHighPriority = highPriority.stream()
                .filter(t -> !alreadyShown.contains(t.getId()))
                .toList();

        int inboxCount = captureService.getInboxCount();
        List<ActivityEntry> recentActivity = activityService.getRecentActivity(5);

        return new TodayView(overdue, dueToday, uniqueHighPriority,
                inboxCount, recentActivity);
    }
}
