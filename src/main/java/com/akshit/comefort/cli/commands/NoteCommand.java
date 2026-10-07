package com.akshit.comefort.cli.commands;

import com.akshit.comefort.cli.CliFormatter;
import com.akshit.comefort.core.Note;
import com.akshit.comefort.service.NoteService;
import com.akshit.comefort.service.ProjectService;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.List;

/**
 * Note management commands — add, list, show, edit.
 */
@Command(
        name = "note",
        description = "Manage notes",
        mixinStandardHelpOptions = true
)
public class NoteCommand implements Runnable {

    private final CliFormatter formatter;

    public NoteCommand(CliFormatter formatter) {
        this.formatter = formatter;
    }

    @Override
    public void run() {
        System.out.println("  Usage: cf note <add|list|show|edit>");
        System.out.println();
        System.out.println("  Subcommands:");
        System.out.println("    add      Create a new note");
        System.out.println("    list     List all notes");
        System.out.println("    show     Display note content");
        System.out.println("    edit     Edit a note");
    }

    @Command(name = "add", description = "Create a new note", mixinStandardHelpOptions = true)
    public static class Add implements Runnable {

        private final NoteService noteService;
        private final ProjectService projectService;
        private final CliFormatter formatter;

        @Parameters(index = "0..*", description = "Note title")
        private List<String> titleWords;

        @Option(names = {"-p", "--project"}, description = "Project name")
        private String projectName;

        @Option(names = {"-c", "--content"}, description = "Note content")
        private String content;

        public Add(NoteService noteService, ProjectService projectService,
                   CliFormatter formatter) {
            this.noteService = noteService;
            this.projectService = projectService;
            this.formatter = formatter;
        }

        @Override
        public void run() {
            if (titleWords == null || titleWords.isEmpty()) {
                formatter.error("Note title is required. Usage: cf note add \"My Note\"");
                return;
            }

            String title = String.join(" ", titleWords);

            String projectId = null;
            if (projectName != null && !projectName.isBlank()) {
                try {
                    projectId = projectService.resolve(projectName).getId();
                } catch (Exception e) {
                    formatter.error("Project not found: " + projectName);
                    return;
                }
            }

            Note note = noteService.create(title, content != null ? content : "", projectId);
            formatter.success("Created note: " + note.getTitle());
        }
    }

    @Command(name = "list", description = "List all notes", mixinStandardHelpOptions = true)
    public static class ListNotes implements Runnable {

        private final NoteService noteService;
        private final ProjectService projectService;
        private final CliFormatter formatter;

        @Option(names = {"-p", "--project"}, description = "Filter by project name")
        private String projectName;

        public ListNotes(NoteService noteService, ProjectService projectService,
                         CliFormatter formatter) {
            this.noteService = noteService;
            this.projectService = projectService;
            this.formatter = formatter;
        }

        @Override
        public void run() {
            List<Note> notes;

            if (projectName != null) {
                try {
                    String projectId = projectService.resolve(projectName).getId();
                    notes = noteService.listByProject(projectId);
                } catch (Exception e) {
                    formatter.error("Project not found: " + projectName);
                    return;
                }
            } else {
                notes = noteService.listAll();
            }

            if (notes.isEmpty()) {
                formatter.printSectionHeader("📝 Notes");
                formatter.empty("No notes yet. Create one with: cf note add \"My Note\" -c \"Content\"");
                formatter.newLine();
                return;
            }

            String header = "📝 Notes" + (projectName != null ? " — " + projectName : "");
            formatter.printSectionHeader(header + " (" + notes.size() + ")");
            for (Note note : notes) {
                String projName = null;
                if (note.getProjectId() != null) {
                    try {
                        projName = projectService.getById(note.getProjectId()).getName();
                    } catch (Exception ignored) {
                    }
                }
                formatter.printNoteLine(note, projName);
            }
            formatter.newLine();
        }
    }

    @Command(name = "show", description = "Display note content", mixinStandardHelpOptions = true)
    public static class Show implements Runnable {

        private final NoteService noteService;
        private final ProjectService projectService;
        private final CliFormatter formatter;

        @Parameters(index = "0..*", description = "Note ID or title fragment")
        private List<String> identifierWords;

        public Show(NoteService noteService, ProjectService projectService,
                    CliFormatter formatter) {
            this.noteService = noteService;
            this.projectService = projectService;
            this.formatter = formatter;
        }

        @Override
        public void run() {
            if (identifierWords == null || identifierWords.isEmpty()) {
                formatter.error("Specify a note ID or title. Usage: cf note show \"My Note\"");
                return;
            }

            String identifier = String.join(" ", identifierWords);
            Note note = noteService.resolve(identifier);

            String projectName = null;
            if (note.getProjectId() != null) {
                try {
                    projectName = projectService.getById(note.getProjectId()).getName();
                } catch (Exception ignored) {
                }
            }

            formatter.printNoteDetail(note, projectName);
            formatter.newLine();
        }
    }

    @Command(name = "edit", description = "Edit a note", mixinStandardHelpOptions = true)
    public static class Edit implements Runnable {

        private final NoteService noteService;
        private final ProjectService projectService;
        private final CliFormatter formatter;

        @Parameters(index = "0", description = "Note ID")
        private String noteId;

        @Option(names = {"-t", "--title"}, description = "New title")
        private String title;

        @Option(names = {"-c", "--content"}, description = "New content")
        private String content;

        @Option(names = {"-p", "--project"}, description = "Move to project")
        private String projectName;

        public Edit(NoteService noteService, ProjectService projectService,
                    CliFormatter formatter) {
            this.noteService = noteService;
            this.projectService = projectService;
            this.formatter = formatter;
        }

        @Override
        public void run() {
            String projectId = null;
            if (projectName != null) {
                try {
                    projectId = projectService.resolve(projectName).getId();
                } catch (Exception e) {
                    formatter.error("Project not found: " + projectName);
                    return;
                }
            }

            Note note = noteService.update(noteId, title, content, projectId);
            formatter.success("Updated note: " + note.getTitle());
        }
    }
}
