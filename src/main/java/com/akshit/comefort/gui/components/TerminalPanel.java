package com.akshit.comefort.gui.components;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Integrated Terminal Panel — Real OS process terminal connected to
 * the system shell (PowerShell / CMD on Windows, Bash / Zsh on Unix).
 *
 * <p>Features:
 *   - Live bidirectional process I/O with standard stream piping
 *   - Real-time ANSI color rendering into styled TextFlow
 *   - Shell switcher (PowerShell / Command Prompt / Git Bash)
 *   - Command history navigation with Up/Down arrows
 *   - Quick ComeFort commands shortcut menu
 *   - Auto-sync: triggers GUI screen refresh when ComeFort CLI commands finish
 *   - Pre-configured PATH containing ComeFort binaries (cmf and comefort)
 * </p>
 */
public class TerminalPanel {

    private static final String FONT_FAMILY = "Cascadia Code, Consolas, 'Courier New', monospace";
    private static final int FONT_SIZE = 13;
    private static final int MAX_CONSOLE_NODES = 4000;

    // UI layout
    private final VBox rootNode;
    private final ScrollPane scrollPane;
    private final TextFlow consoleFlow;
    private TextField inputField;
    private Label statusIndicator;
    private ComboBox<String> shellSelector;
    private Button maxRestoreBtn;

    // State & process management
    private final Runnable onDataChanged;
    private final Runnable onCloseRequest;
    private Process process;
    private BufferedWriter processWriter;
    private Thread outputReaderThread;
    private volatile boolean running = false;

    // Command history
    private final List<String> commandHistory = new ArrayList<>();
    private int historyIndex = 0;
    private boolean isMaximized = false;
    private double currentHeight = 260.0;

    // ANSI pattern: \u001B\[([0-9;]*)m
    private static final Pattern ANSI_PATTERN = Pattern.compile("\u001B\\[([0-9;]*)m");

    public TerminalPanel(Runnable onDataChanged, Runnable onCloseRequest) {
        this.onDataChanged = onDataChanged;
        this.onCloseRequest = onCloseRequest;

        this.rootNode = new VBox();
        this.rootNode.getStyleClass().add("terminal-panel");
        this.rootNode.setPrefHeight(currentHeight);
        this.rootNode.setMinHeight(160);

        // 1. Header bar
        HBox header = buildHeader();

        // 2. Output console inside ScrollPane
        consoleFlow = new TextFlow();
        consoleFlow.getStyleClass().add("terminal-flow");
        consoleFlow.setLineSpacing(2);
        consoleFlow.setPadding(new Insets(8, 12, 8, 12));

        scrollPane = new ScrollPane(consoleFlow);
        scrollPane.getStyleClass().add("terminal-scroll");
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(false);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        // Click on console focuses the input
        scrollPane.setOnMouseClicked(e -> inputField.requestFocus());

        // 3. Interactive prompt input bar
        HBox inputBar = buildInputBar();

        rootNode.getChildren().addAll(header, scrollPane, inputBar);

        // Start default shell
        String defaultShell = detectDefaultShell();
        startShell(defaultShell);
    }

    /**
     * Builds the top control bar with title, shell picker, quick actions, clear, maximize, and close.
     */
    private HBox buildHeader() {
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("terminal-header");
        header.setPadding(new Insets(6, 12, 6, 12));

        Label title = new Label("⌨ TERMINAL");
        title.getStyleClass().add("terminal-title");

        statusIndicator = new Label("● Connecting...");
        statusIndicator.getStyleClass().add("terminal-status");
        statusIndicator.setStyle("-fx-text-fill: #2ecc71; -fx-font-size: 11px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Available shells
        shellSelector = new ComboBox<>();
        shellSelector.getStyleClass().add("terminal-shell-select");
        List<String> availableShells = getAvailableShells();
        shellSelector.getItems().addAll(availableShells);
        if (!availableShells.isEmpty()) {
            shellSelector.setValue(availableShells.get(0));
        }
        shellSelector.setOnAction(e -> {
            String selected = shellSelector.getValue();
            if (selected != null) {
                restartShell(selected);
            }
        });

        // Quick ComeFort Commands button
        MenuButton quickCmds = new MenuButton("⚡ Quick Cmds");
        quickCmds.getStyleClass().add("terminal-btn");
        addQuickMenuItem(quickCmds, "cmf today", "cmf today");
        addQuickMenuItem(quickCmds, "cmf inbox", "cmf inbox");
        addQuickMenuItem(quickCmds, "cmf task list", "cmf task list");
        addQuickMenuItem(quickCmds, "cmf project list", "cmf project list");
        addQuickMenuItem(quickCmds, "cmf note list", "cmf note list");
        addQuickMenuItem(quickCmds, "cmf status", "cmf status");
        addQuickMenuItem(quickCmds, "cmf --help", "cmf --help");

        // Clear button
        Button clearBtn = new Button("🗑 Clear");
        clearBtn.getStyleClass().add("terminal-btn");
        clearBtn.setTooltip(new Tooltip("Clear terminal screen (Ctrl+L)"));
        clearBtn.setOnAction(e -> clearConsole());

        // Restart button
        Button restartBtn = new Button("↻ Restart");
        restartBtn.getStyleClass().add("terminal-btn");
        restartBtn.setTooltip(new Tooltip("Restart active shell session"));
        restartBtn.setOnAction(e -> restartShell(shellSelector.getValue()));

        // Maximize / Restore button
        maxRestoreBtn = new Button("⤢");
        maxRestoreBtn.getStyleClass().add("terminal-btn");
        maxRestoreBtn.setTooltip(new Tooltip("Toggle Maximize / Restore"));
        maxRestoreBtn.setOnAction(e -> toggleMaximize());

        // Close button
        Button closeBtn = new Button("✕");
        closeBtn.getStyleClass().add("terminal-btn-close");
        closeBtn.setTooltip(new Tooltip("Hide Terminal (Ctrl+`)"));
        closeBtn.setOnAction(e -> {
            if (onCloseRequest != null) {
                onCloseRequest.run();
            }
        });

        header.getChildren().addAll(
                title, statusIndicator, spacer,
                shellSelector, quickCmds, clearBtn, restartBtn, maxRestoreBtn, closeBtn
        );
        return header;
    }

    private void addQuickMenuItem(MenuButton menu, String label, String command) {
        MenuItem item = new MenuItem(label);
        item.setOnAction(e -> {
            inputField.setText(command);
            inputField.requestFocus();
            inputField.positionCaret(command.length());
        });
        menu.getItems().add(item);
    }

    /**
     * Builds the bottom input prompt bar.
     */
    private HBox buildInputBar() {
        HBox inputBar = new HBox(8);
        inputBar.setAlignment(Pos.CENTER_LEFT);
        inputBar.getStyleClass().add("terminal-input-bar");
        inputBar.setPadding(new Insets(6, 12, 6, 12));

        Label promptSymbol = new Label("❯");
        promptSymbol.setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold; -fx-font-family: monospace; -fx-font-size: 14px;");

        inputField = new TextField();
        inputField.getStyleClass().add("terminal-input");
        inputField.setPromptText("Type command (e.g. cmf today, dir, git status)...");
        inputField.setStyle("-fx-font-family: " + FONT_FAMILY + "; -fx-font-size: " + FONT_SIZE + "px;");
        HBox.setHgrow(inputField, Priority.ALWAYS);

        // Handle Enter key and Up/Down history
        inputField.addEventFilter(KeyEvent.KEY_PRESSED, this::handleInputKeyEvent);

        Button runBtn = new Button("Run ⏎");
        runBtn.getStyleClass().add("terminal-btn-run");
        runBtn.setOnAction(e -> executeCurrentInput());

        inputBar.getChildren().addAll(promptSymbol, inputField, runBtn);
        return inputBar;
    }

    /**
     * Handles keyboard shortcuts inside the input field:
     * Enter = execute, Up/Down = navigate history, Ctrl+L = clear, Ctrl+C = interrupt.
     */
    private void handleInputKeyEvent(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            event.consume();
            executeCurrentInput();
        } else if (event.getCode() == KeyCode.UP) {
            event.consume();
            if (!commandHistory.isEmpty()) {
                if (historyIndex > 0) {
                    historyIndex--;
                }
                if (historyIndex < commandHistory.size()) {
                    inputField.setText(commandHistory.get(historyIndex));
                    inputField.positionCaret(inputField.getText().length());
                }
            }
        } else if (event.getCode() == KeyCode.DOWN) {
            event.consume();
            if (!commandHistory.isEmpty()) {
                if (historyIndex < commandHistory.size() - 1) {
                    historyIndex++;
                    inputField.setText(commandHistory.get(historyIndex));
                    inputField.positionCaret(inputField.getText().length());
                } else {
                    historyIndex = commandHistory.size();
                    inputField.clear();
                }
            }
        } else if (event.isControlDown() && event.getCode() == KeyCode.L) {
            event.consume();
            clearConsole();
        } else if (event.isControlDown() && event.getCode() == KeyCode.C) {
            event.consume();
            sendInterrupt();
        }
    }

    /**
     * Sends the current input text to the underlying shell process.
     */
    public void executeCurrentInput() {
        String command = inputField.getText();
        inputField.clear();

        if (command == null || command.trim().isEmpty()) {
            writeToProcess("\r\n");
            return;
        }

        commandHistory.add(command);
        historyIndex = commandHistory.size();

        // Echo command to console if shell doesn't echo it
        writeToProcess(command + "\r\n");

        // If the command is a ComeFort CLI mutation, refresh the GUI after brief delay
        String trimmed = command.trim();
        if (trimmed.startsWith("cmf ") || trimmed.startsWith("comefort ") || trimmed.equals("cmf") || trimmed.equals("comefort")) {
            if (onDataChanged != null) {
                new Thread(() -> {
                    try {
                        Thread.sleep(450);
                    } catch (InterruptedException ignored) {
                    }
                    Platform.runLater(onDataChanged);
                }, "GUI-Sync-Timer").start();
            }
        }
    }

    private void writeToProcess(String text) {
        if (processWriter == null || process == null || !process.isAlive()) {
            appendSystemMessage("Process is not running. Restarting shell...\n");
            restartShell(shellSelector.getValue());
            return;
        }

        try {
            processWriter.write(text);
            processWriter.flush();
        } catch (IOException e) {
            appendErrorMessage("Failed to send input: " + e.getMessage() + "\n");
        }
    }

    /**
     * Sends ASCII ETX (Ctrl+C) to interrupt running command.
     */
    private void sendInterrupt() {
        if (processWriter != null && process != null && process.isAlive()) {
            try {
                processWriter.write("\u0003");
                processWriter.flush();
                appendSystemMessage("^C\n");
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * Starts the specified shell process.
     */
    public synchronized void startShell(String shellName) {
        destroy(); // Terminate any existing process cleanly

        List<String> commandList = resolveShellCommand(shellName);
        ProcessBuilder pb = new ProcessBuilder(commandList);

        // Set working directory to project root
        File workDir = new File(System.getProperty("user.dir", "."));
        if (workDir.exists()) {
            pb.directory(workDir);
        }

        // Augment PATH with local ComeFort distribution binaries and project directory
        Map<String, String> env = pb.environment();
        String currentPath = env.getOrDefault("PATH", "");
        File binDir = new File(workDir, "build/install/comefort/bin");
        if (binDir.exists()) {
            env.put("PATH", binDir.getAbsolutePath() + File.pathSeparator + currentPath);
        }
        env.put("COMEFORT_TERMINAL", "1");
        env.put("TERM", "xterm-256color");

        // Pipe both stdout and stderr together
        pb.redirectErrorStream(true);

        try {
            process = pb.start();
            processWriter = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
            running = true;

            Platform.runLater(() -> {
                statusIndicator.setText("● Active: " + shellName);
                statusIndicator.setStyle("-fx-text-fill: #2ecc71; -fx-font-size: 11px;");
            });

            // Start background reader
            startOutputReader(process.getInputStream());

            appendBanner(shellName, workDir.getAbsolutePath());

        } catch (Exception ex) {
            running = false;
            Platform.runLater(() -> {
                statusIndicator.setText("● Failed: " + ex.getMessage());
                statusIndicator.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 11px;");
            });
            appendErrorMessage("Error launching " + shellName + ": " + ex.getMessage() + "\n");
        }
    }

    private void appendBanner(String shellName, String workDir) {
        appendSystemMessage("╔════════════════════════════════════════════════════════════════╗\n");
        appendSystemMessage("║  ComeFort Integrated Terminal (" + shellName + ")                        \n");
        appendSystemMessage("║  Cwd: " + workDir + "\n");
        appendSystemMessage("║  Both 'cmf' and 'comefort' commands are directly available.   \n");
        appendSystemMessage("╚════════════════════════════════════════════════════════════════╝\n\n");
    }

    /**
     * Reads output from shell process in a daemon thread and flushes to UI.
     */
    private void startOutputReader(InputStream in) {
        outputReaderThread = new Thread(() -> {
            byte[] buffer = new byte[4096];
            Charset charset = Charset.defaultCharset();
            try {
                int read;
                while (running && (read = in.read(buffer)) != -1) {
                    String chunk = new String(buffer, 0, read, charset);
                    Platform.runLater(() -> appendAnsiOutput(chunk));
                }
            } catch (IOException e) {
                if (running) {
                    Platform.runLater(() -> appendSystemMessage("\n[Process closed]\n"));
                }
            } finally {
                Platform.runLater(() -> {
                    statusIndicator.setText("● Stopped");
                    statusIndicator.setStyle("-fx-text-fill: #95a5a6; -fx-font-size: 11px;");
                });
            }
        }, "Terminal-Reader-" + System.currentTimeMillis());

        outputReaderThread.setDaemon(true);
        outputReaderThread.start();
    }

    /**
     * Parses ANSI color escapes and appends formatted Text nodes into the console.
     */
    private void appendAnsiOutput(String text) {
        if (text == null || text.isEmpty()) return;

        Matcher matcher = ANSI_PATTERN.matcher(text);
        int lastIndex = 0;
        Color currentColor = Color.web("#e0e0e0");
        boolean isBold = false;

        while (matcher.find()) {
            if (matcher.start() > lastIndex) {
                String sub = text.substring(lastIndex, matcher.start());
                addTextNode(sub, currentColor, isBold);
            }

            // Parse escape code
            String codeStr = matcher.group(1);
            if (codeStr != null && !codeStr.isEmpty()) {
                String[] codes = codeStr.split(";");
                for (String code : codes) {
                    try {
                        int c = Integer.parseInt(code);
                        switch (c) {
                            case 0 -> {
                                currentColor = Color.web("#e0e0e0");
                                isBold = false;
                            }
                            case 1 -> isBold = true;
                            case 30 -> currentColor = Color.web("#2b2b2b");
                            case 31, 91 -> currentColor = Color.web("#e74c3c");
                            case 32, 92 -> currentColor = Color.web("#2ecc71");
                            case 33, 93 -> currentColor = Color.web("#f1c40f");
                            case 34, 94 -> currentColor = Color.web("#3498db");
                            case 35, 95 -> currentColor = Color.web("#9b59b6");
                            case 36, 96 -> currentColor = Color.web("#1abc9c");
                            case 37, 97 -> currentColor = Color.web("#ffffff");
                            case 90 -> currentColor = Color.web("#888888");
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            } else {
                // Reset
                currentColor = Color.web("#e0e0e0");
                isBold = false;
            }

            lastIndex = matcher.end();
        }

        if (lastIndex < text.length()) {
            String remaining = text.substring(lastIndex);
            addTextNode(remaining, currentColor, isBold);
        }

        // Bound nodes to prevent memory unbounded growth
        if (consoleFlow.getChildren().size() > MAX_CONSOLE_NODES) {
            consoleFlow.getChildren().remove(0, 600);
        }

        // Auto-scroll to bottom
        scrollPane.layout();
        scrollPane.setVvalue(1.0);
    }

    private void addTextNode(String text, Color color, boolean bold) {
        Text node = new Text(text);
        node.setFont(Font.font(FONT_FAMILY, bold ? FontWeight.BOLD : FontWeight.NORMAL, FONT_SIZE));
        node.setFill(color);
        consoleFlow.getChildren().add(node);
    }

    public void appendSystemMessage(String msg) {
        addTextNode(msg, Color.web("#3498db"), false);
        scrollPane.layout();
        scrollPane.setVvalue(1.0);
    }

    public void appendErrorMessage(String msg) {
        addTextNode(msg, Color.web("#e74c3c"), true);
        scrollPane.layout();
        scrollPane.setVvalue(1.0);
    }

    public void clearConsole() {
        consoleFlow.getChildren().clear();
    }

    public void restartShell(String shellName) {
        clearConsole();
        startShell(shellName);
    }

    public void focusInput() {
        Platform.runLater(inputField::requestFocus);
    }

    private void toggleMaximize() {
        isMaximized = !isMaximized;
        if (isMaximized) {
            currentHeight = 520.0;
            rootNode.setPrefHeight(currentHeight);
            maxRestoreBtn.setText("🗗");
        } else {
            currentHeight = 260.0;
            rootNode.setPrefHeight(currentHeight);
            maxRestoreBtn.setText("⤢");
        }
    }

    public Region getView() {
        return rootNode;
    }

    /**
     * Cleanly terminates the background shell process.
     */
    public synchronized void destroy() {
        running = false;
        if (process != null) {
            try {
                if (processWriter != null) {
                    processWriter.close();
                }
            } catch (IOException ignored) {
            }
            process.destroyForcibly();
            process = null;
        }
        if (outputReaderThread != null) {
            outputReaderThread.interrupt();
            outputReaderThread = null;
        }
    }

    // --- Shell Detection & Resolution ---

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    private static String detectDefaultShell() {
        if (isWindows()) {
            return "PowerShell";
        }
        return "Bash";
    }

    private static List<String> getAvailableShells() {
        List<String> shells = new ArrayList<>();
        if (isWindows()) {
            shells.add("PowerShell");
            shells.add("Command Prompt");
            // Check Git Bash
            if (new File("C:\\Program Files\\Git\\bin\\bash.exe").exists()
                    || new File("C:\\Program Files (x86)\\Git\\bin\\bash.exe").exists()) {
                shells.add("Git Bash");
            }
        } else {
            shells.add("Bash");
            shells.add("Zsh");
            shells.add("Sh");
        }
        return shells;
    }

    private static List<String> resolveShellCommand(String shellName) {
        if (isWindows()) {
            if ("Command Prompt".equalsIgnoreCase(shellName)) {
                return List.of("cmd.exe", "/K");
            }
            if ("Git Bash".equalsIgnoreCase(shellName)) {
                if (new File("C:\\Program Files\\Git\\bin\\bash.exe").exists()) {
                    return List.of("C:\\Program Files\\Git\\bin\\bash.exe", "-i");
                }
                if (new File("C:\\Program Files (x86)\\Git\\bin\\bash.exe").exists()) {
                    return List.of("C:\\Program Files (x86)\\Git\\bin\\bash.exe", "-i");
                }
            }
            // Default PowerShell
            return List.of("powershell.exe", "-NoLogo", "-NoExit", "-ExecutionPolicy", "Bypass");
        } else {
            if ("Zsh".equalsIgnoreCase(shellName)) {
                return List.of("/bin/zsh", "-i");
            }
            if ("Sh".equalsIgnoreCase(shellName)) {
                return List.of("/bin/sh", "-i");
            }
            return List.of("/bin/bash", "-i");
        }
    }
}
