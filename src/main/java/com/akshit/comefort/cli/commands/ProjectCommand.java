package com.akshit.comefort.cli.commands;

import com.akshit.comefort.cli.CliFormatter;
import com.akshit.comefort.core.ActivityEntry;
import com.akshit.comefort.core.Note;
import com.akshit.comefort.core.Project;
import com.akshit.comefort.core.Task;
import com.akshit.comefort.service.ActivityService;
import com.akshit.comefort.service.NoteService;
import com.akshit.comefort.service.ProjectService;
import com.akshit.comefort.service.TaskService;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.List;

/**
 * Project management commands — add, list, show.
 */
@Command(
        name = "project",
        description = "Manage projects",
        mixinStandardHelpOptions = true
)
public class ProjectCommand implements Runnable {

    private final CliFormatter formatter;

    public ProjectCommand(CliFormatter formatter) {
        this.formatter = formatter;
    }

    @Override
    public void run() {
        System.out.println("  Usage: cf project <add|list|show>");
        System.out.println();
        System.out.println("  Subcommands:");
        System.out.println("    add      Create a new project");
        System.out.println("    list     List all projects");
        System.out.println("    show     Show project details");
    }

    @Command(name = "add", description = "Create a new project", mixinStandardHelpOptions = true)
    public static class Add implements Runnable {

        private final ProjectService projectService;
        private final CliFormatter formatter;

        @Parameters(index = "0", description = "Project name")
        private String name;

        @Option(names = {"--desc"}, description = "Project description")
        private String description;

        @Option(names = {"--path"}, description = "Filesystem path to the project")
        private String path;

        public Add(ProjectService projectService, CliFormatter formatter) {
            this.projectService = projectService;
            this.formatter = formatter;
        }

        @Override
        public void run() {
            Project project = projectService.create(name, description, path);
            formatter.success("Created project: " + project.getName());
        }
    }

    @Command(name = "list", description = "List all projects", mixinStandardHelpOptions = true)
    public static class ListProjects implements Runnable {

        private final ProjectService projectService;
        private final TaskService taskService;
        private final NoteService noteService;
        private final CliFormatter formatter;

        public ListProjects(ProjectService projectService, TaskService taskService,
                            NoteService noteService, CliFormatter formatter) {
            this.projectService = projectService;
            this.taskService = taskService;
            this.noteService = noteService;
            this.formatter = formatter;
        }

        @Override
        public void run() {
            List<Project> projects = projectService.listAll();

            if (projects.isEmpty()) {
                formatter.printSectionHeader("🚀 Projects");
                formatter.empty("No projects yet. Create one with: cf project add MyProject");
                formatter.newLine();
                return;
            }

            formatter.printSectionHeader("🚀 Projects (" + projects.size() + ")");
            for (Project project : projects) {
                int taskCount = taskService.listAll(project.getId(), null, null).size();
                int noteCount = noteService.listByProject(project.getId()).size();
                formatter.printProjectLine(project, taskCount, noteCount);
            }
            formatter.newLine();
        }
    }

    @Command(name = "show", description = "Show project details and recent activity", mixinStandardHelpOptions = true)
    public static class Show implements Runnable {

        private final ProjectService projectService;
        private final TaskService taskService;
        private final NoteService noteService;
        private final ActivityService activityService;
        private final CliFormatter formatter;

        @Parameters(index = "0..*", description = "Project name or fragment")
        private List<String> nameWords;

        public Show(ProjectService projectService, TaskService taskService,
                    NoteService noteService, ActivityService activityService,
                    CliFormatter formatter) {
            this.projectService = projectService;
            this.taskService = taskService;
            this.noteService = noteService;
            this.activityService = activityService;
            this.formatter = formatter;
        }

        @Override
        public void run() {
            if (nameWords == null || nameWords.isEmpty()) {
                formatter.error("Specify a project name. Usage: cf project show MyProject");
                return;
            }

            String nameQuery = String.join(" ", nameWords);
            Project project = projectService.resolve(nameQuery);

            // Header
            formatter.printSectionHeader("🚀 " + project.getName());
            if (project.getDescription() != null) {
                System.out.println("  " + project.getDescription());
            }
            if (project.getPath() != null) {
                System.out.println("  Path: " + project.getPath());
            }

            // Tasks
            List<Task> tasks = taskService.listAll(project.getId(), null, null);
            long openCount = tasks.stream().filter(Task::isOpen).count();
            long doneCount = tasks.stream()
                    .filter(t -> t.getStatus() == com.akshit.comefort.core.enums.TaskStatus.DONE)
                    .count();

            formatter.printSectionHeader("Tasks (" + openCount + " open, " + doneCount + " done)");
            if (tasks.isEmpty()) {
                formatter.empty("No tasks");
            } else {
                for (Task task : tasks) {
                    formatter.printTaskLine(task, null);
                }
            }

            // Notes
            List<Note> notes = noteService.listByProject(project.getId());
            formatter.printSectionHeader("Notes (" + notes.size() + ")");
            if (notes.isEmpty()) {
                formatter.empty("No notes");
            } else {
                for (Note note : notes) {
                    formatter.printNoteLine(note, null);
                }
            }

            // Recent Activity
            List<ActivityEntry> activity = activityService.getProjectActivity(
                    project.getId(), 10);
            formatter.printSectionHeader("Recent Activity");
            if (activity.isEmpty()) {
                formatter.empty("No activity recorded yet");
            } else {
                for (ActivityEntry entry : activity) {
                    formatter.printActivityLine(entry);
                }
            }

            formatter.newLine();
        }
    }
}
