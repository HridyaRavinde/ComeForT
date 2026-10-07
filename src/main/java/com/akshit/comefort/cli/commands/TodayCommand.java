package com.akshit.comefort.cli.commands;

import com.akshit.comefort.cli.CliFormatter;
import com.akshit.comefort.core.ActivityEntry;
import com.akshit.comefort.core.Task;
import com.akshit.comefort.service.ProjectService;
import com.akshit.comefort.service.TodayService;
import com.akshit.comefort.service.TodayService.TodayView;
import picocli.CommandLine.Command;

/**
 * Today dashboard — answers "What should I do right now?"
 */
@Command(
        name = "today",
        description = "Your daily dashboard — overdue, due today, high priority",
        mixinStandardHelpOptions = true
)
public class TodayCommand implements Runnable {

    private final TodayService todayService;
    private final ProjectService projectService;
    private final CliFormatter formatter;

    public TodayCommand(TodayService todayService, ProjectService projectService,
                        CliFormatter formatter) {
        this.todayService = todayService;
        this.projectService = projectService;
        this.formatter = formatter;
    }

    @Override
    public void run() {
        TodayView view = todayService.buildTodayView();

        formatter.printSectionHeader("🔥 TODAY");

        boolean hasContent = false;

        // Overdue
        if (!view.overdueTasks().isEmpty()) {
            hasContent = true;
            formatter.printTaskSection("⚠ OVERDUE", view.overdueTasks(), this::resolveProjectName);
        }

        // Due Today
        if (!view.dueTodayTasks().isEmpty()) {
            hasContent = true;
            formatter.printTaskSection("📅 DUE TODAY", view.dueTodayTasks(), this::resolveProjectName);
        }

        // High Priority
        if (!view.highPriorityTasks().isEmpty()) {
            hasContent = true;
            formatter.printTaskSection("🔥 HIGH PRIORITY", view.highPriorityTasks(), this::resolveProjectName);
        }

        // Inbox
        if (view.inboxCount() > 0) {
            hasContent = true;
            System.out.println();
            formatter.info("📥 Inbox: " + view.inboxCount() + " unprocessed captures");
        }

        // Recent Activity
        if (!view.recentActivity().isEmpty()) {
            hasContent = true;
            formatter.printSectionHeader("Recent Activity");
            for (ActivityEntry entry : view.recentActivity()) {
                formatter.printActivityLine(entry);
            }
        }

        if (!hasContent) {
            formatter.newLine();
            formatter.empty("Nothing urgent! You're all caught up. 🎉");
            formatter.info("Capture something: cmf c \"your next thought\"");
        }

        formatter.newLine();
    }

    private String resolveProjectName(Task task) {
        if (task.getProjectId() == null) return null;
        try {
            return projectService.getById(task.getProjectId()).getName();
        } catch (Exception e) {
            return null;
        }
    }
}
