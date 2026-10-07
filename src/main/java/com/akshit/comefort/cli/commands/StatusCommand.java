package com.akshit.comefort.cli.commands;

import com.akshit.comefort.cli.CliFormatter;
import com.akshit.comefort.core.ActivityEntry;
import com.akshit.comefort.core.enums.TaskStatus;
import com.akshit.comefort.service.*;
import picocli.CommandLine.Command;

import java.util.List;

/**
 * Global status command — shows overall counts and recent activity.
 */
@Command(
        name = "status",
        description = "Show global ComeFort status and statistics",
        mixinStandardHelpOptions = true
)
public class StatusCommand implements Runnable {

    private final ProjectService projectService;
    private final TaskService taskService;
    private final NoteService noteService;
    private final CaptureService captureService;
    private final ActivityService activityService;
    private final CliFormatter formatter;

    public StatusCommand(ProjectService projectService, TaskService taskService,
                         NoteService noteService, CaptureService captureService,
                         ActivityService activityService, CliFormatter formatter) {
        this.projectService = projectService;
        this.taskService = taskService;
        this.noteService = noteService;
        this.captureService = captureService;
        this.activityService = activityService;
        this.formatter = formatter;
    }

    @Override
    public void run() {
        formatter.printBanner();

        int projectCount = projectService.count();
        int totalTasks = taskService.count();
        int openTasks = taskService.countByStatus(TaskStatus.OPEN)
                + taskService.countByStatus(TaskStatus.IN_PROGRESS);
        int doneTasks = taskService.countByStatus(TaskStatus.DONE);
        int noteCount = noteService.count();
        int inboxCount = captureService.getInboxCount();

        formatter.printSectionHeader("Status");
        System.out.printf("  Projects:   %d%n", projectCount);
        System.out.printf("  Tasks:      %d (%d open, %d done)%n", totalTasks, openTasks, doneTasks);
        System.out.printf("  Notes:      %d%n", noteCount);
        System.out.printf("  Inbox:      %d unprocessed%n", inboxCount);

        // Recent activity
        List<ActivityEntry> recent = activityService.getRecentActivity(5);
        if (!recent.isEmpty()) {
            formatter.printSectionHeader("Recent Activity");
            for (ActivityEntry entry : recent) {
                formatter.printActivityLine(entry);
            }
        }

        formatter.newLine();
    }
}
