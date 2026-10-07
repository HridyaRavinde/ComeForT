package com.akshit.comefort.cli;

import com.akshit.comefort.core.*;
import com.akshit.comefort.core.enums.TaskPriority;
import com.akshit.comefort.util.ConsoleColors;
import com.akshit.comefort.util.DateTimeUtil;
import com.akshit.comefort.util.IdGenerator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles all CLI output formatting — tables, boxes, colors, banners.
 * Centralizes display logic so commands stay clean.
 */
public class CliFormatter {

    private static final int DEFAULT_WIDTH = 56;

    /**
     * Prints the ASCII art banner from the classpath resource.
     */
    public void printBanner() {
        try (InputStream is = getClass().getResourceAsStream("/banner.txt")) {
            if (is == null) {
                System.out.println(ConsoleColors.bold("ComeFort v0.1.0"));
                return;
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String banner = reader.lines().collect(Collectors.joining("\n"));
                System.out.println(ConsoleColors.colorize(banner, ConsoleColors.BOLD_CYAN));
            }
        } catch (IOException e) {
            System.out.println(ConsoleColors.bold("ComeFort v0.1.0"));
        }
    }

    // --- Task formatting ---

    /**
     * Prints a single task in a compact one-line format.
     */
    public void printTaskLine(Task task, String projectName) {
        String status = task.getStatus().getIcon();
        String priority = formatPriorityTag(task.getPriority());
        String title = task.getTitle();
        String due = DateTimeUtil.formatDueDate(task.getDueDate());
        String project = projectName != null ? ConsoleColors.dim(projectName) : "";
        String id = ConsoleColors.dim("[" + task.getShortId() + "]");

        System.out.printf("  %s %s %-35s %s  %s  %s%n",
                status, priority, title, project, due, id);
    }

    /**
     * Prints a list of tasks under a section header.
     */
    public void printTaskSection(String header, List<Task> tasks,
                                 java.util.function.Function<Task, String> projectNameResolver) {
        if (tasks.isEmpty()) return;

        System.out.println();
        System.out.println("  " + ConsoleColors.bold(header) + ConsoleColors.dim(" (" + tasks.size() + ")"));
        System.out.println("  " + "─".repeat(DEFAULT_WIDTH - 4));

        for (Task task : tasks) {
            String projectName = task.getProjectId() != null
                    ? projectNameResolver.apply(task) : null;
            printTaskLine(task, projectName);
        }
    }

    // --- Project formatting ---

    /**
     * Prints a project summary line.
     */
    public void printProjectLine(Project project, int taskCount, int noteCount) {
        String name = ConsoleColors.bold(project.getName());
        String desc = project.getDescription() != null
                ? ConsoleColors.dim(" — " + project.getDescription()) : "";
        String counts = ConsoleColors.dim(taskCount + " tasks  " + noteCount + " notes");

        System.out.printf("  %-40s %s%n", name + desc, counts);
    }

    // --- Note formatting ---

    /**
     * Prints a note summary line.
     */
    public void printNoteLine(Note note, String projectName) {
        String title = ConsoleColors.bold(note.getTitle());
        String preview = ConsoleColors.dim(note.getContentPreview());
        String project = projectName != null ? ConsoleColors.dim("[" + projectName + "]") : "";
        String time = ConsoleColors.dim(DateTimeUtil.relativeTime(note.getUpdatedAt()));
        String id = ConsoleColors.dim("[" + note.getShortId() + "]");

        System.out.printf("  %s %s  %s  %s%n", title, project, time, id);
        if (!note.getContentPreview().isEmpty()) {
            System.out.println("    " + preview);
        }
    }

    /**
     * Prints the full content of a note.
     */
    public void printNoteDetail(Note note, String projectName) {
        printSectionHeader(note.getTitle());
        if (projectName != null) {
            System.out.println("  Project: " + ConsoleColors.info(projectName));
        }
        System.out.println("  Created: " + DateTimeUtil.relativeTime(note.getCreatedAt()));
        System.out.println("  Updated: " + DateTimeUtil.relativeTime(note.getUpdatedAt()));
        System.out.println();
        System.out.println(note.getContent());
    }

    // --- Capture formatting ---

    /**
     * Prints a capture/inbox item.
     */
    public void printCaptureLine(Capture capture) {
        String type = capture.getType().getIcon();
        String content = capture.getContent();
        String time = ConsoleColors.dim(DateTimeUtil.relativeTime(capture.getCreatedAt()));
        String id = ConsoleColors.dim("[" + capture.getShortId() + "]");

        System.out.printf("  %s  %-45s %s  %s%n", type, content, time, id);
    }

    // --- Activity formatting ---

    /**
     * Prints an activity log entry.
     */
    public void printActivityLine(ActivityEntry entry) {
        String action = entry.getAction().getDisplayName();
        String summary = entry.getSummary() != null ? entry.getSummary() : "";
        String time = ConsoleColors.dim(DateTimeUtil.relativeTime(entry.getCreatedAt()));

        System.out.printf("  %s  %-40s %s%n", action, summary, time);
    }

    // --- Generic formatting ---

    /**
     * Prints a section header with a line underneath.
     */
    public void printSectionHeader(String title) {
        System.out.println();
        System.out.println("  " + ConsoleColors.bold(title));
        System.out.println("  " + "─".repeat(DEFAULT_WIDTH - 4));
    }

    /**
     * Prints a success message.
     */
    public void success(String message) {
        System.out.println("  " + ConsoleColors.success("✓ " + message));
    }

    /**
     * Prints an error message.
     */
    public void error(String message) {
        System.out.println("  " + ConsoleColors.error("✗ " + message));
    }

    /**
     * Prints a warning message.
     */
    public void warning(String message) {
        System.out.println("  " + ConsoleColors.warning("⚠ " + message));
    }

    /**
     * Prints an info message.
     */
    public void info(String message) {
        System.out.println("  " + ConsoleColors.info("ℹ " + message));
    }

    /**
     * Prints an empty state message.
     */
    public void empty(String message) {
        System.out.println("  " + ConsoleColors.dim(message));
    }

    /**
     * Prints a blank line.
     */
    public void newLine() {
        System.out.println();
    }

    // --- Helpers ---

    private String formatPriorityTag(TaskPriority priority) {
        return switch (priority) {
            case CRITICAL -> ConsoleColors.colorize("[CRIT]", ConsoleColors.BOLD_RED);
            case HIGH -> ConsoleColors.colorize("[HIGH]", ConsoleColors.RED);
            case MEDIUM -> ConsoleColors.colorize("[MED] ", ConsoleColors.YELLOW);
            case LOW -> ConsoleColors.colorize("[LOW] ", ConsoleColors.DIM);
        };
    }
}
