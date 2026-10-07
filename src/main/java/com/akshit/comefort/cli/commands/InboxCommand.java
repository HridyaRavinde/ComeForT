package com.akshit.comefort.cli.commands;

import com.akshit.comefort.cli.CliFormatter;
import com.akshit.comefort.core.Capture;
import com.akshit.comefort.core.Note;
import com.akshit.comefort.core.Task;
import com.akshit.comefort.core.enums.TaskPriority;
import com.akshit.comefort.service.CaptureService;
import com.akshit.comefort.service.ProjectService;
import com.akshit.comefort.util.DateTimeUtil;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.time.LocalDate;
import java.util.List;

/**
 * Inbox management commands:
 *   - cmf inbox              (list inbox)
 *   - cmf inbox list
 *   - cmf inbox convert <id> --to <task|note|idea> [-t title] [-p project] [--priority] [-d due]
 *   - cmf inbox done <id>    (mark processed)
 *   - cmf inbox delete <id>  (delete)
 */
@Command(
        name = "inbox",
        description = "Manage unprocessed captures in your inbox",
        mixinStandardHelpOptions = true
)
public class InboxCommand implements Runnable {

    private final CaptureService captureService;
    private final CliFormatter formatter;

    public InboxCommand(CaptureService captureService, CliFormatter formatter) {
        this.captureService = captureService;
        this.formatter = formatter;
    }

    @Override
    public void run() {
        List<Capture> inbox = captureService.getInbox();

        if (inbox.isEmpty()) {
            formatter.printSectionHeader("📥 Inbox");
            formatter.empty("Your inbox is empty. Capture something with: cmf c \"your thought\"");
            formatter.newLine();
            return;
        }

        formatter.printSectionHeader("📥 Inbox (" + inbox.size() + " items)");
        for (Capture capture : inbox) {
            formatter.printCaptureLine(capture);
        }
        formatter.newLine();
    }

    @Command(name = "list", description = "List unprocessed inbox captures", mixinStandardHelpOptions = true)
    public static class ListInbox implements Runnable {
        private final CaptureService captureService;
        private final CliFormatter formatter;

        public ListInbox(CaptureService captureService, CliFormatter formatter) {
            this.captureService = captureService;
            this.formatter = formatter;
        }

        @Override
        public void run() {
            List<Capture> inbox = captureService.getInbox();
            if (inbox.isEmpty()) {
                formatter.printSectionHeader("📥 Inbox");
                formatter.empty("Your inbox is empty. Capture something with: cmf c \"your thought\"");
                formatter.newLine();
                return;
            }

            formatter.printSectionHeader("📥 Inbox (" + inbox.size() + " items)");
            for (Capture capture : inbox) {
                formatter.printCaptureLine(capture);
            }
            formatter.newLine();
        }
    }

    @Command(name = "convert", description = "Convert an inbox capture into a task, note, or idea", mixinStandardHelpOptions = true)
    public static class Convert implements Runnable {
        private final CaptureService captureService;
        private final ProjectService projectService;
        private final CliFormatter formatter;

        @Parameters(index = "0", description = "Capture ID or text fragment")
        private String captureId;

        @Option(names = {"--to"}, required = true, description = "Target type: task, note, or idea")
        private String targetType;

        @Option(names = {"-t", "--title"}, description = "Custom title")
        private String title;

        @Option(names = {"-p", "--project"}, description = "Assign to project name or ID")
        private String projectName;

        @Option(names = {"--priority"}, description = "Priority for task: low, medium, high, critical")
        private String priority;

        @Option(names = {"-d", "--due"}, description = "Due date for task: today, tomorrow, yyyy-MM-dd")
        private String dueDate;

        public Convert(CaptureService captureService, ProjectService projectService, CliFormatter formatter) {
            this.captureService = captureService;
            this.projectService = projectService;
            this.formatter = formatter;
        }

        @Override
        public void run() {
            String projectId = null;
            if (projectName != null && !projectName.isBlank()) {
                try {
                    projectId = projectService.resolve(projectName).getId();
                } catch (Exception e) {
                    formatter.error("Project not found: " + projectName);
                    return;
                }
            }

            String type = targetType.toLowerCase().trim();
            switch (type) {
                case "task" -> {
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

                    Task task = captureService.convertToTask(captureId, title, projectId, taskPriority, parsedDue);
                    formatter.success("Converted capture to task: " + task.getTitle() + " [" + task.getShortId() + "]");
                }
                case "note" -> {
                    Note note = captureService.convertToNote(captureId, title, null, projectId);
                    formatter.success("Converted capture to note: " + note.getTitle() + " [" + note.getShortId() + "]");
                }
                case "idea" -> {
                    Note note = captureService.convertToIdea(captureId, projectId);
                    formatter.success("Converted capture to idea note: " + note.getTitle() + " [" + note.getShortId() + "]");
                }
                default -> formatter.error("Invalid target type '" + targetType + "'. Supported types: task, note, idea");
            }
        }
    }

    @Command(name = "done", description = "Mark an inbox capture as processed", mixinStandardHelpOptions = true)
    public static class Process implements Runnable {
        private final CaptureService captureService;
        private final CliFormatter formatter;

        @Parameters(index = "0", description = "Capture ID or content fragment")
        private String captureId;

        public Process(CaptureService captureService, CliFormatter formatter) {
            this.captureService = captureService;
            this.formatter = formatter;
        }

        @Override
        public void run() {
            captureService.markProcessed(captureId);
            formatter.success("Marked capture as processed.");
        }
    }

    @Command(name = "delete", description = "Delete an inbox capture", mixinStandardHelpOptions = true)
    public static class Delete implements Runnable {
        private final CaptureService captureService;
        private final CliFormatter formatter;

        @Parameters(index = "0", description = "Capture ID or content fragment")
        private String captureId;

        public Delete(CaptureService captureService, CliFormatter formatter) {
            this.captureService = captureService;
            this.formatter = formatter;
        }

        @Override
        public void run() {
            captureService.delete(captureId);
            formatter.success("Deleted capture.");
        }
    }
}
