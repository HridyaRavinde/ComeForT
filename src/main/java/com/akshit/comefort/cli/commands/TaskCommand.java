package com.akshit.comefort.cli.commands;

import com.akshit.comefort.cli.CliFormatter;
import com.akshit.comefort.core.Task;
import com.akshit.comefort.core.enums.TaskPriority;
import com.akshit.comefort.core.enums.TaskStatus;
import com.akshit.comefort.service.ProjectService;
import com.akshit.comefort.service.TaskService;
import com.akshit.comefort.util.DateTimeUtil;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Task management commands — add, list, done, edit, delete.
 */
@Command(
        name = "task",
        description = "Manage tasks",
        mixinStandardHelpOptions = true
)
public class TaskCommand implements Runnable {

    private final CliFormatter formatter;

    public TaskCommand(CliFormatter formatter) {
        this.formatter = formatter;
    }

    @Override
    public void run() {
        // Show help when no subcommand is given
        System.out.println("  Usage: cmf task <add|list|done|edit|delete> (or comefort task ...)");
        System.out.println();
        System.out.println("  Subcommands:");
        System.out.println("    add      Create a new task");
        System.out.println("    list     List tasks (with filters)");
        System.out.println("    done     Mark a task as completed");
        System.out.println("    edit     Edit task fields");
        System.out.println("    delete   Delete a task");
    }

    // --- Subcommands ---

    @Command(name = "add", description = "Create a new task", mixinStandardHelpOptions = true)
    public static class Add implements Runnable {

        private final TaskService taskService;
        private final ProjectService projectService;
        private final CliFormatter formatter;

        @Parameters(index = "0..*", description = "Task title")
        private List<String> titleWords;

        @Option(names = {"-p", "--project"}, description = "Project name")
        private String projectName;

        @Option(names = {"-d", "--due"}, description = "Due date (today, tomorrow, or yyyy-MM-dd)")
        private String dueDate;

        @Option(names = {"--priority"}, description = "Priority: low, medium, high, critical",
                defaultValue = "medium")
        private String priority;

        public Add(TaskService taskService, ProjectService projectService,
                   CliFormatter formatter) {
            this.taskService = taskService;
            this.projectService = projectService;
            this.formatter = formatter;
        }

        @Override
        public void run() {
            if (titleWords == null || titleWords.isEmpty()) {
                formatter.error("Task title is required. Usage: cmf task add \"Task title\"");
                return;
            }

            String title = String.join(" ", titleWords);

            // Resolve project if provided
            String projectId = null;
            if (projectName != null && !projectName.isBlank()) {
                try {
                    projectId = projectService.resolve(projectName).getId();
                } catch (Exception e) {
                    formatter.error("Project not found: " + projectName);
                    return;
                }
            }

            // Parse priority
            TaskPriority taskPriority;
            try {
                taskPriority = TaskPriority.fromString(priority);
            } catch (IllegalArgumentException e) {
                formatter.error(e.getMessage());
                return;
            }

            // Parse due date
            LocalDate parsedDue = null;
            if (dueDate != null && !dueDate.isBlank()) {
                parsedDue = DateTimeUtil.parseFriendlyDate(dueDate);
                if (parsedDue == null) {
                    formatter.error("Invalid due date: " + dueDate
                            + ". Use: today, tomorrow, or yyyy-MM-dd");
                    return;
                }
            }

            Task task = taskService.create(title, projectId, taskPriority, parsedDue);

            formatter.success("Created task: " + task.getTitle());
            if (parsedDue != null) {
                formatter.info("Due: " + DateTimeUtil.formatDueDate(parsedDue));
            }
        }
    }

    @Command(name = "list", description = "List tasks", mixinStandardHelpOptions = true)
    public static class ListTasks implements Runnable {

        private final TaskService taskService;
        private final ProjectService projectService;
        private final CliFormatter formatter;

        @Option(names = {"-p", "--project"}, description = "Filter by project name")
        private String projectName;

        @Option(names = {"-s", "--status"}, description = "Filter by status: open, done, archived")
        private String status;

        @Option(names = {"--priority"}, description = "Filter by priority: low, medium, high, critical")
        private String priority;

        public ListTasks(TaskService taskService, ProjectService projectService,
                         CliFormatter formatter) {
            this.taskService = taskService;
            this.projectService = projectService;
            this.formatter = formatter;
        }

        @Override
        public void run() {
            // Resolve filters
            String projectId = null;
            if (projectName != null) {
                try {
                    projectId = projectService.resolve(projectName).getId();
                } catch (Exception e) {
                    formatter.error("Project not found: " + projectName);
                    return;
                }
            }

            TaskStatus taskStatus = null;
            if (status != null) {
                try {
                    taskStatus = TaskStatus.fromString(status);
                } catch (IllegalArgumentException e) {
                    formatter.error(e.getMessage());
                    return;
                }
            }

            TaskPriority taskPriority = null;
            if (priority != null) {
                try {
                    taskPriority = TaskPriority.fromString(priority);
                } catch (IllegalArgumentException e) {
                    formatter.error(e.getMessage());
                    return;
                }
            }

            List<Task> tasks = taskService.listAll(projectId, taskStatus, taskPriority);

            if (tasks.isEmpty()) {
                formatter.printSectionHeader("Tasks");
                formatter.empty("No tasks found. Create one with: cmf task add \"My task\"");
                formatter.newLine();
                return;
            }

            Map<String, String> projectNames = projectService.getProjectNameMap();
            String header = "Tasks" + (projectName != null ? " — " + projectName : "");
            formatter.printTaskSection(header, tasks, task -> {
                if (task.getProjectId() == null) return null;
                return projectNames.get(task.getProjectId());
            });
            formatter.newLine();
        }
    }

    @Command(name = "done", description = "Mark a task as completed", mixinStandardHelpOptions = true)
    public static class Done implements Runnable {

        private final TaskService taskService;
        private final CliFormatter formatter;

        @Parameters(index = "0..*", description = "Task ID or title fragment")
        private List<String> identifierWords;

        public Done(TaskService taskService, CliFormatter formatter) {
            this.taskService = taskService;
            this.formatter = formatter;
        }

        @Override
        public void run() {
            if (identifierWords == null || identifierWords.isEmpty()) {
                formatter.error("Specify a task ID or title fragment. Usage: cmf task done \"Fix bug\"");
                return;
            }

            String identifier = String.join(" ", identifierWords);
            Task task = taskService.complete(identifier);
            formatter.success("Completed: " + task.getTitle());
        }
    }

    @Command(name = "edit", description = "Edit a task", mixinStandardHelpOptions = true)
    public static class Edit implements Runnable {

        private final TaskService taskService;
        private final ProjectService projectService;
        private final CliFormatter formatter;

        @Parameters(index = "0", description = "Task ID")
        private String taskId;

        @Option(names = {"-t", "--title"}, description = "New title")
        private String title;

        @Option(names = {"--desc"}, description = "New description")
        private String description;

        @Option(names = {"--priority"}, description = "New priority")
        private String priority;

        @Option(names = {"-d", "--due"}, description = "New due date")
        private String dueDate;

        @Option(names = {"--clear-due"}, description = "Clear the due date")
        private boolean clearDue;

        @Option(names = {"-s", "--status"}, description = "New status")
        private String status;

        @Option(names = {"-p", "--project"}, description = "Move to project")
        private String projectName;

        public Edit(TaskService taskService, ProjectService projectService,
                    CliFormatter formatter) {
            this.taskService = taskService;
            this.projectService = projectService;
            this.formatter = formatter;
        }

        @Override
        public void run() {
            TaskPriority taskPriority = null;
            if (priority != null) {
                try {
                    taskPriority = TaskPriority.fromString(priority);
                } catch (IllegalArgumentException e) {
                    formatter.error(e.getMessage());
                    return;
                }
            }

            LocalDate parsedDue = null;
            if (dueDate != null) {
                parsedDue = DateTimeUtil.parseFriendlyDate(dueDate);
                if (parsedDue == null) {
                    formatter.error("Invalid due date: " + dueDate);
                    return;
                }
            }

            TaskStatus taskStatus = null;
            if (status != null) {
                try {
                    taskStatus = TaskStatus.fromString(status);
                } catch (IllegalArgumentException e) {
                    formatter.error(e.getMessage());
                    return;
                }
            }

            String projectId = null;
            if (projectName != null) {
                try {
                    projectId = projectService.resolve(projectName).getId();
                } catch (Exception e) {
                    formatter.error("Project not found: " + projectName);
                    return;
                }
            }

            Task task = taskService.update(taskId, title, description,
                    taskPriority, parsedDue, clearDue, taskStatus, projectId);
            formatter.success("Updated task: " + task.getTitle());
        }
    }

    @Command(name = "delete", description = "Delete a task", mixinStandardHelpOptions = true)
    public static class Delete implements Runnable {

        private final TaskService taskService;
        private final CliFormatter formatter;

        @Parameters(index = "0..*", description = "Task ID or title fragment")
        private List<String> identifierWords;

        public Delete(TaskService taskService, CliFormatter formatter) {
            this.taskService = taskService;
            this.formatter = formatter;
        }

        @Override
        public void run() {
            if (identifierWords == null || identifierWords.isEmpty()) {
                formatter.error("Specify a task ID or title. Usage: cmf task delete \"task name\"");
                return;
            }

            String identifier = String.join(" ", identifierWords);
            taskService.delete(identifier);
            formatter.success("Deleted task: " + identifier);
        }
    }
}
