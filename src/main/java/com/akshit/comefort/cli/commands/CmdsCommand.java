package com.akshit.comefort.cli.commands;

import com.akshit.comefort.cli.CliFormatter;
import com.akshit.comefort.util.ConsoleColors;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.Wincon;
import picocli.CommandLine.Command;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Lists all ComeFort commands in a clean, compact one-line list
 * (identical in interface to {@code git log --oneline}) with interactive
 * pagination (Up/Down navigation, ':' prompt, and 'q' to exit).
 */
@Command(
        name = "cmds",
        aliases = {"commands"},
        description = "Display all ComeFort commands in an interactive oneline list (like git log --oneline)",
        mixinStandardHelpOptions = true
)
public class CmdsCommand implements Runnable {

    private final CliFormatter formatter;

    public interface WindowsConsoleNative extends Library {
        WindowsConsoleNative INSTANCE = Platform.isWindows()
                ? Native.load("msvcrt", WindowsConsoleNative.class)
                : null;

        int _getch();
        int _kbhit();
    }

    public record CommandDoc(String syntax, String description) {}

    public CmdsCommand(CliFormatter formatter) {
        this.formatter = formatter;
    }

    private static final List<CommandDoc> COMMANDS = List.of(
            new CommandDoc("cmf today", "Your daily dashboard — overdue, due today, high priority"),
            new CommandDoc("cmf c <thought>", "Quick capture — dump a thought into your inbox"),
            new CommandDoc("cmf inbox", "View and manage unprocessed captures in your inbox"),
            new CommandDoc("cmf inbox list", "List all unprocessed inbox items"),
            new CommandDoc("cmf inbox convert <id>", "Convert capture into a task or project note"),
            new CommandDoc("cmf inbox done <id>", "Mark an inbox capture as processed"),
            new CommandDoc("cmf inbox delete <id>", "Permanently delete an inbox capture"),
            new CommandDoc("cmf task list", "List active tasks with priority and due dates"),
            new CommandDoc("cmf task add <title>", "Create a new task (-p priority, -d due, --project)"),
            new CommandDoc("cmf task done <id>", "Mark a task as completed"),
            new CommandDoc("cmf task edit <id>", "Edit task title, priority, or due date"),
            new CommandDoc("cmf task delete <id>", "Permanently delete a task"),
            new CommandDoc("cmf project list", "List all active projects with task and note metrics"),
            new CommandDoc("cmf project add <name>", "Create a new project (-d description)"),
            new CommandDoc("cmf project show <name>", "Show project details, active tasks, and notes"),
            new CommandDoc("cmf note list", "List all notes with preview snippets"),
            new CommandDoc("cmf note add <title>", "Create a note (-c content, --project name)"),
            new CommandDoc("cmf note show <id>", "Display full note body and metadata"),
            new CommandDoc("cmf note edit <id>", "Edit note title or body content"),
            new CommandDoc("cmf search <query>", "Global search across tasks, notes, captures, and projects"),
            new CommandDoc("cmf status", "Global statistics, completion rates, streaks, and activity"),
            new CommandDoc("cmf gui", "Launch ComeFort desktop GUI application"),
            new CommandDoc("cmf cmds", "Display this interactive command reference (pager)"),
            new CommandDoc("cmf init", "Initialize local ComeFort database"),
            new CommandDoc("cmf help [command]", "Display detailed help and flags for any command")
    );

    private enum PagerAction {
        SCROLL_DOWN,
        SCROLL_UP,
        PAGE_DOWN,
        PAGE_UP,
        HOME,
        END,
        EXIT,
        IGNORE
    }

    @Override
    public void run() {
        List<String> formattedLines = new ArrayList<>();
        for (CommandDoc cmd : COMMANDS) {
            // Format syntax in bold yellow (matching git log commit hash aesthetic)
            String syntaxCol = String.format("%-28s", cmd.syntax());
            formattedLines.add(ConsoleColors.colorize(syntaxCol, ConsoleColors.BOLD_YELLOW) + " " + cmd.description());
        }

        // If non-interactive stream (piped e.g. cmf cmds | grep ... or redirect), print all lines and exit
        if (System.console() == null) {
            for (String line : formattedLines) {
                System.out.println(line);
            }
            return;
        }

        int totalLines = formattedLines.size();
        int termHeight = getTerminalHeight();
        // Reserve the very bottom row for the ':' prompt
        int visibleRows = Math.max(4, termHeight - 1);
        int maxTop = Math.max(0, totalLines - visibleRows);
        int topIndex = 0;

        // Enter alternate screen buffer & hide cursor during initial setup
        System.out.print("\u001B[?1049h\u001B[?25l");
        System.out.flush();

        try {
            renderScreen(formattedLines, topIndex, visibleRows, totalLines);

            while (true) {
                PagerAction action = readNextKey();

                // Re-query height in case window was resized
                int currentTermHeight = getTerminalHeight();
                visibleRows = Math.max(4, currentTermHeight - 1);
                maxTop = Math.max(0, totalLines - visibleRows);

                if (action == PagerAction.EXIT) {
                    break;
                } else if (action == PagerAction.SCROLL_DOWN) {
                    if (topIndex < maxTop) {
                        topIndex++;
                        renderScreen(formattedLines, topIndex, visibleRows, totalLines);
                    }
                } else if (action == PagerAction.SCROLL_UP) {
                    if (topIndex > 0) {
                        topIndex--;
                        renderScreen(formattedLines, topIndex, visibleRows, totalLines);
                    }
                } else if (action == PagerAction.PAGE_DOWN) {
                    if (topIndex < maxTop) {
                        topIndex = Math.min(topIndex + visibleRows, maxTop);
                        renderScreen(formattedLines, topIndex, visibleRows, totalLines);
                    }
                } else if (action == PagerAction.PAGE_UP) {
                    if (topIndex > 0) {
                        topIndex = Math.max(0, topIndex - visibleRows);
                        renderScreen(formattedLines, topIndex, visibleRows, totalLines);
                    }
                } else if (action == PagerAction.HOME) {
                    if (topIndex > 0) {
                        topIndex = 0;
                        renderScreen(formattedLines, topIndex, visibleRows, totalLines);
                    }
                } else if (action == PagerAction.END) {
                    if (topIndex < maxTop) {
                        topIndex = maxTop;
                        renderScreen(formattedLines, topIndex, visibleRows, totalLines);
                    }
                }
            }
        } finally {
            // Restore main screen buffer and restore cursor
            System.out.print("\u001B[?25h\u001B[?1049l");
            System.out.flush();
        }
    }

    private void renderScreen(List<String> lines, int topIndex, int visibleRows, int totalLines) {
        StringBuilder sb = new StringBuilder();
        sb.append("\u001B[H"); // Cursor to (1, 1)

        for (int r = 0; r < visibleRows; r++) {
            int idx = topIndex + r;
            if (idx < totalLines) {
                sb.append(lines.get(idx));
            }
            sb.append("\u001B[K\r\n"); // Clear line to right margin and newline
        }

        // Bottom row: single ':' prompt exactly like git log / less
        sb.append(":\u001B[K");
        sb.append("\u001B[?25h"); // Show cursor at the ':' prompt position

        System.out.print(sb);
        System.out.flush();
    }

    private static int getTerminalHeight() {
        if (Platform.isWindows()) {
            try {
                WinNT.HANDLE hOut = Kernel32.INSTANCE.GetStdHandle(Wincon.STD_OUTPUT_HANDLE);
                Wincon.CONSOLE_SCREEN_BUFFER_INFO csbi = new Wincon.CONSOLE_SCREEN_BUFFER_INFO();
                if (Kernel32.INSTANCE.GetConsoleScreenBufferInfo(hOut, csbi)) {
                    int height = csbi.srWindow.Bottom - csbi.srWindow.Top + 1;
                    if (height >= 5) {
                        return height;
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        String lines = System.getenv("LINES");
        if (lines != null) {
            try {
                return Integer.parseInt(lines);
            } catch (NumberFormatException ignored) {
            }
        }
        return 24;
    }

    private PagerAction readNextKey() {
        while (true) {
            // 1. Check native Windows console keyboard buffer (instant, unbuffered)
            if (Platform.isWindows() && WindowsConsoleNative.INSTANCE != null) {
                try {
                    if (WindowsConsoleNative.INSTANCE._kbhit() != 0) {
                        int ch = WindowsConsoleNative.INSTANCE._getch();
                        if (ch == 'q' || ch == 'Q' || ch == 27 || ch == 3) {
                            return PagerAction.EXIT;
                        } else if (ch == 13 || ch == 10 || ch == 'j') {
                            return PagerAction.SCROLL_DOWN;
                        } else if (ch == 'k') {
                            return PagerAction.SCROLL_UP;
                        } else if (ch == ' ' || ch == 'f') {
                            return PagerAction.PAGE_DOWN;
                        } else if (ch == 'b') {
                            return PagerAction.PAGE_UP;
                        } else if (ch == 'g') {
                            return PagerAction.HOME;
                        } else if (ch == 'G') {
                            return PagerAction.END;
                        } else if (ch == 0 || ch == 224) {
                            // Extended key code
                            int ext = WindowsConsoleNative.INSTANCE._getch();
                            if (ext == 80) { // Down Arrow
                                return PagerAction.SCROLL_DOWN;
                            } else if (ext == 72) { // Up Arrow
                                return PagerAction.SCROLL_UP;
                            } else if (ext == 81) { // Page Down
                                return PagerAction.PAGE_DOWN;
                            } else if (ext == 73) { // Page Up
                                return PagerAction.PAGE_UP;
                            } else if (ext == 71) { // Home
                                return PagerAction.HOME;
                            } else if (ext == 79) { // End
                                return PagerAction.END;
                            }
                        }
                        return PagerAction.IGNORE;
                    }
                } catch (Throwable ignored) {
                }
            }

            // 2. Check standard input stream (for piped inputs or standard terminals)
            try {
                if (System.in.available() > 0) {
                    int ch = System.in.read();
                    if (ch == 'q' || ch == 'Q' || ch == 3 || ch == -1) {
                        return PagerAction.EXIT;
                    } else if (ch == '\n' || ch == '\r' || ch == 'j') {
                        return PagerAction.SCROLL_DOWN;
                    } else if (ch == 'k') {
                        return PagerAction.SCROLL_UP;
                    } else if (ch == ' ' || ch == 'f') {
                        return PagerAction.PAGE_DOWN;
                    } else if (ch == 'b') {
                        return PagerAction.PAGE_UP;
                    } else if (ch == 27) { // ANSI escape sequence
                        if (System.in.available() >= 2) {
                            int next1 = System.in.read();
                            int next2 = System.in.read();
                            if (next1 == '[') {
                                if (next2 == 'B') return PagerAction.SCROLL_DOWN;
                                if (next2 == 'A') return PagerAction.SCROLL_UP;
                                if (next2 == '5') return PagerAction.PAGE_UP;
                                if (next2 == '6') return PagerAction.PAGE_DOWN;
                            }
                        }
                        return PagerAction.EXIT;
                    }
                    return PagerAction.IGNORE;
                }
            } catch (Exception ignored) {
                return PagerAction.EXIT;
            }

            try {
                Thread.sleep(15);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return PagerAction.EXIT;
            }
        }
    }
}
