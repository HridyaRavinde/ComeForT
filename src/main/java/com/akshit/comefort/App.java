package com.akshit.comefort;

import com.akshit.comefort.cli.CliFormatter;
import com.akshit.comefort.cli.commands.*;
import com.akshit.comefort.db.DatabaseManager;
import com.akshit.comefort.exception.ComeFortException;
import com.akshit.comefort.repository.*;
import com.akshit.comefort.service.*;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/**
 * ComeFort — Your Local-First Developer Life OS.
 *
 * <p>This is the main entry point. It wires together all layers
 * (database → repositories → services → commands) and delegates
 * to picocli for command routing.</p>
 */
@Command(
        name = "comefort",
        aliases = {"cmf"},
        description = "ComeFort — Your Local-First Developer Life OS",
        version = "0.1.0",
        mixinStandardHelpOptions = true,
        subcommands = {CommandLine.HelpCommand.class}
)
public class App implements Runnable {

    @CommandLine.Option(names = {"--gui"}, description = "Launch ComeFort desktop GUI")
    private boolean gui;

    // --- Infrastructure ---
    private final DatabaseManager dbManager;
    private final CliFormatter formatter;

    // --- Repositories ---
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final NoteRepository noteRepository;
    private final CaptureRepository captureRepository;
    private final ActivityRepository activityRepository;
    private final ConfigRepository configRepository;

    // --- Services ---
    private final ActivityService activityService;
    private final ProjectService projectService;
    private final TaskService taskService;
    private final NoteService noteService;
    private final CaptureService captureService;
    private final SearchService searchService;
    private final TodayService todayService;

    public App() {
        this(AppContext.getInstance());
    }

    public App(DatabaseManager dbManager) {
        this(new AppContext(dbManager));
    }

    public App(AppContext context) {
        this.formatter = new CliFormatter();
        this.dbManager = context.getDbManager();

        this.projectRepository = context.getProjectRepository();
        this.taskRepository = context.getTaskRepository();
        this.noteRepository = context.getNoteRepository();
        this.captureRepository = context.getCaptureRepository();
        this.activityRepository = context.getActivityRepository();
        this.configRepository = context.getConfigRepository();

        this.activityService = context.getActivityService();
        this.projectService = context.getProjectService();
        this.taskService = context.getTaskService();
        this.noteService = context.getNoteService();
        this.captureService = context.getCaptureService();
        this.searchService = context.getSearchService();
        this.todayService = context.getTodayService();
    }

    @Override
    public void run() {
        if (gui) {
            com.akshit.comefort.gui.MainWindow.launchGui(new String[]{});
            return;
        }
        // No subcommand given — show today dashboard as default
        ensureInitialized();
        new TodayCommand(todayService, projectService, formatter).run();
    }

    /**
     * Ensures the database is initialized before running any command.
     * Auto-initializes on first use.
     */
    public void ensureInitialized() {
        if (!dbManager.isDatabaseInitialized()) {
            dbManager.initialize();
        } else {
            // Open connection to existing database
            dbManager.initialize();
        }
    }

    /**
     * Creates and configures the CommandLine parser with all subcommands and exception handling.
     */
    public static CommandLine createCommandLine(App app) {
        CommandLine cmd = new CommandLine(app);

        // Register subcommands with their dependencies
        cmd.addSubcommand("init",
                new InitCommand(app.dbManager, app.formatter));
        cmd.addSubcommand("c",
                new CaptureCommand(app.captureService, app.formatter));
        // Inbox command with subcommands
        CommandLine inboxCmd = new CommandLine(new InboxCommand(app.captureService, app.formatter));
        inboxCmd.addSubcommand("list", new InboxCommand.ListInbox(app.captureService, app.formatter));
        inboxCmd.addSubcommand("convert", new InboxCommand.Convert(app.captureService, app.projectService, app.formatter));
        inboxCmd.addSubcommand("done", new InboxCommand.Process(app.captureService, app.formatter));
        inboxCmd.addSubcommand("delete", new InboxCommand.Delete(app.captureService, app.formatter));
        cmd.addSubcommand("inbox", inboxCmd);
        cmd.addSubcommand("gui",
                new GuiCommand());

        // Task command with subcommands
        CommandLine taskCmd = new CommandLine(new TaskCommand(app.formatter));
        taskCmd.addSubcommand("add",
                new TaskCommand.Add(app.taskService, app.projectService, app.formatter));
        taskCmd.addSubcommand("list",
                new TaskCommand.ListTasks(app.taskService, app.projectService, app.formatter));
        taskCmd.addSubcommand("done",
                new TaskCommand.Done(app.taskService, app.formatter));
        taskCmd.addSubcommand("edit",
                new TaskCommand.Edit(app.taskService, app.projectService, app.formatter));
        taskCmd.addSubcommand("delete",
                new TaskCommand.Delete(app.taskService, app.formatter));
        cmd.addSubcommand("task", taskCmd);

        // Project command with subcommands
        CommandLine projectCmd = new CommandLine(new ProjectCommand(app.formatter));
        projectCmd.addSubcommand("add",
                new ProjectCommand.Add(app.projectService, app.formatter));
        projectCmd.addSubcommand("list",
                new ProjectCommand.ListProjects(app.projectService, app.taskService,
                        app.noteService, app.formatter));
        projectCmd.addSubcommand("show",
                new ProjectCommand.Show(app.projectService, app.taskService,
                        app.noteService, app.activityService, app.formatter));
        cmd.addSubcommand("project", projectCmd);

        // Note command with subcommands
        CommandLine noteCmd = new CommandLine(new NoteCommand(app.formatter));
        noteCmd.addSubcommand("add",
                new NoteCommand.Add(app.noteService, app.projectService, app.formatter));
        noteCmd.addSubcommand("list",
                new NoteCommand.ListNotes(app.noteService, app.projectService, app.formatter));
        noteCmd.addSubcommand("show",
                new NoteCommand.Show(app.noteService, app.projectService, app.formatter));
        noteCmd.addSubcommand("edit",
                new NoteCommand.Edit(app.noteService, app.projectService, app.formatter));
        cmd.addSubcommand("note", noteCmd);

        // Search, Today, Status
        cmd.addSubcommand("search",
                new SearchCommand(app.searchService, app.formatter));
        cmd.addSubcommand("today",
                new TodayCommand(app.todayService, app.projectService, app.formatter));
        cmd.addSubcommand("status",
                new StatusCommand(app.projectService, app.taskService,
                        app.noteService, app.captureService,
                        app.activityService, app.formatter));

        // Set up execution strategy that initializes DB before every command
        cmd.setExecutionStrategy(parseResult -> {
            // Check if help or version was requested anywhere in command hierarchy
            boolean isHelpRequested = parseResult.isUsageHelpRequested() || parseResult.isVersionHelpRequested();
            CommandLine.ParseResult pr = parseResult;
            while (pr.hasSubcommand()) {
                pr = pr.subcommand();
                if (pr.isUsageHelpRequested() || pr.isVersionHelpRequested() || "help".equals(pr.commandSpec().name())) {
                    isHelpRequested = true;
                    break;
                }
            }

            boolean isGuiCommand = parseResult.hasSubcommand() && "gui".equals(parseResult.subcommand().commandSpec().name());

            if (!isHelpRequested && !isGuiCommand) {
                app.ensureInitialized();
            }

            // Execute the command
            return new CommandLine.RunLast().execute(parseResult);
        });

        // Handle exceptions gracefully
        cmd.setExecutionExceptionHandler((ex, commandLine, parseResult) -> {
            if (ex instanceof ComeFortException) {
                app.formatter.error(ex.getMessage());
            } else {
                app.formatter.error("Unexpected error: " + ex.getMessage());
                if (System.getenv("COMEFORT_DEBUG") != null || System.getenv("CMF_DEBUG") != null) {
                    ex.printStackTrace();
                }
            }
            return 1;
        });

        return cmd;
    }

    public static void main(String[] args) {
        App app = new App();
        CommandLine cmd = createCommandLine(app);

        // Execute
        int exitCode = cmd.execute(args);

        // Clean up
        app.dbManager.close();

        System.exit(exitCode);
    }
}
