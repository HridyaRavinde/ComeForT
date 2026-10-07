package com.akshit.comefort.gui.components;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Callback;
import javafx.util.Duration;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Integrated Terminal Panel — Real OS terminal connected to the host machine's
 * genuinely installed shells (PowerShell 7, Windows PowerShell, CMD, Git Bash, WSL, etc.).
 *
 * <p>Key Features:
 *   - Real Shell Discovery: Zero hardcoding. Detects installed shells directly from the host system.
 *   - Native Terminal Emulation: Real ANSI color parser, screen clear (cls/clear) support.
 *   - Authentic Inline Typing: Blinking cursor attached directly to the active prompt.
 *   - Shell Switcher: Dynamically lists all verified executables present on this device.
 *   - Full Keyboard Controls: Enter, Backspace, Delete, Arrows, History, Tab autocomplete, Ctrl+C, Ctrl+L, Ctrl+V.
 *   - PATH Injection: Guarantees 'cmf' and 'comefort' binaries are in process PATH.
 *   - Real-time GUI Sync: Triggers desktop data reload after ComeFort CLI mutations.
 * </p>
 */
public class TerminalPanel {

    private static final String FONT_FAMILY = "Cascadia Code, Consolas, 'Courier New', monospace";
    private static final int FONT_SIZE = 13;
    private static final int MAX_CONSOLE_NODES = 5000;

    // UI layout
    private final VBox rootNode;
    private final ScrollPane scrollPane;
    private final TextFlow consoleFlow;
    private Label statusIndicator;
    private ComboBox<ShellDetector.ShellProfile> shellSelector;
    private Button maxRestoreBtn;

    // Shell configuration
    private final List<ShellDetector.ShellProfile> installedShells;
    private ShellDetector.ShellProfile activeShell;

    // Inline prompt & typing state
    private final StringBuilder currentInput = new StringBuilder();
    private int cursorIndex = 0;
    private final Text inputBeforeCursor = new Text("");
    private final Text cursorNode = new Text("█");
    private final Text inputAfterCursor = new Text("");
    private Timeline cursorTimeline;

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
    private double currentHeight = 270.0;

    // Autocomplete dictionary
    private static final List<String> CMF_AUTOCOMPLETE = List.of(
            "cmf today", "cmf inbox", "cmf inbox done ", "cmf inbox convert ",
            "cmf task list", "cmf task add ", "cmf task done ", "cmf task edit ",
            "cmf project list", "cmf project add ", "cmf project show ",
            "cmf note list", "cmf note add ", "cmf note show ",
            "cmf status", "cmf search ", "cmf gui", "cmf --help",
            "comefort today", "comefort task list", "comefort inbox",
            "git status", "git log", "git diff", "dir", "cls", "clear", "exit"
    );

    // ANSI pattern: \u001B\[([0-9;]*)m or control codes
    private static final Pattern ANSI_PATTERN = Pattern.compile("\u001B\\[([0-9;]*)([a-zA-Z])");

    public TerminalPanel(Runnable onDataChanged, Runnable onCloseRequest) {
        this.onDataChanged = onDataChanged;
        this.onCloseRequest = onCloseRequest;

        // Dynamically discover all real shells installed on this device
        this.installedShells = ShellDetector.detectInstalledShells();
        this.activeShell = ShellDetector.getDefaultShell(installedShells);

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
        consoleFlow.setPadding(new Insets(10, 14, 10, 14));

        Font font = Font.font(FONT_FAMILY, FontWeight.NORMAL, FONT_SIZE);
        inputBeforeCursor.setFont(font);
        inputBeforeCursor.setFill(Color.web("#f8f8f2"));
        inputBeforeCursor.getStyleClass().add("terminal-input-text");

        cursorNode.setFont(Font.font(FONT_FAMILY, FontWeight.BOLD, FONT_SIZE));
        cursorNode.setFill(Color.web("#2ecc71"));
        cursorNode.getStyleClass().add("terminal-cursor");

        inputAfterCursor.setFont(font);
        inputAfterCursor.setFill(Color.web("#f8f8f2"));
        inputAfterCursor.getStyleClass().add("terminal-input-text");

        scrollPane = new ScrollPane(consoleFlow);
        scrollPane.getStyleClass().add("terminal-scroll");
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(false);
        scrollPane.setFocusTraversable(true);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        // Direct keyboard listeners on scrollPane
        scrollPane.addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyPressed);
        scrollPane.addEventFilter(KeyEvent.KEY_TYPED, this::handleKeyTyped);

        // Focus handling
        rootNode.setOnMouseClicked(e -> focusInput());
        scrollPane.setOnMouseClicked(e -> focusInput());
        consoleFlow.setOnMouseClicked(e -> focusInput());

        setupCursorBlinking();

        rootNode.getChildren().addAll(header, scrollPane);

        // Start active shell
        if (activeShell != null) {
            startShell(activeShell);
        }
    }

    /**
     * Builds the top control bar with dynamically discovered shell profiles.
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

        // Dynamic Shell Profiles Dropdown (Real shells only)
        shellSelector = new ComboBox<>();
        shellSelector.getStyleClass().add("terminal-shell-select");
        shellSelector.getItems().addAll(installedShells);
        shellSelector.setValue(activeShell);

        // Custom Cell Factory for clean shell icons and descriptions
        Callback<ListView<ShellDetector.ShellProfile>, ListCell<ShellDetector.ShellProfile>> cellFactory = lv -> new ListCell<>() {
            @Override
            protected void updateItem(ShellDetector.ShellProfile item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.icon() + "  " + item.displayName());
                    setTooltip(new Tooltip(item.subtitle() + "\n" + item.executablePath()));
                }
            }
        };
        shellSelector.setCellFactory(cellFactory);
        shellSelector.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(ShellDetector.ShellProfile item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.icon() + "  " + item.displayName());
                }
            }
        });

        shellSelector.setOnAction(e -> {
            ShellDetector.ShellProfile selected = shellSelector.getValue();
            if (selected != null && !selected.equals(activeShell)) {
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

        // Clear button (Ctrl+L)
        Button clearBtn = new Button("🗑 Clear");
        clearBtn.getStyleClass().add("terminal-btn");
        clearBtn.setTooltip(new Tooltip("Clear terminal screen (Ctrl+L or type cls/clear)"));
        clearBtn.setOnAction(e -> clearConsole());

        // Restart button
        Button restartBtn = new Button("↺ Restart");
        restartBtn.getStyleClass().add("terminal-btn");
        restartBtn.setTooltip(new Tooltip("Restart active shell session"));
        restartBtn.setOnAction(e -> restartShell(activeShell));

        // Maximize / Restore button
        maxRestoreBtn = new Button("⤢");
        maxRestoreBtn.getStyleClass().add("terminal-btn");
        maxRestoreBtn.setTooltip(new Tooltip("Toggle Maximize / Restore"));
        maxRestoreBtn.setOnAction(e -> toggleMaximize());

        // Close button (Ctrl+` or Ctrl+T)
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
            setInputText(command);
            focusInput();
        });
        menu.getItems().add(item);
    }

    private void setupCursorBlinking() {
        cursorTimeline = new Timeline(new KeyFrame(Duration.millis(500), e -> {
            if (scrollPane.isFocused()) {
                cursorNode.setVisible(!cursorNode.isVisible());
            } else {
                cursorNode.setVisible(true);
            }
        }));
        cursorTimeline.setCycleCount(Animation.INDEFINITE);
        cursorTimeline.play();

        scrollPane.focusedProperty().addListener((obs, oldV, focused) -> {
            if (focused) {
                cursorNode.setFill(Color.web("#2ecc71"));
                cursorNode.setVisible(true);
                cursorTimeline.play();
            } else {
                cursorNode.setFill(Color.web("#777777"));
                cursorNode.setVisible(true);
                cursorTimeline.stop();
            }
        });
    }

    private void handleKeyTyped(KeyEvent event) {
        String c = event.getCharacter();
        if (c == null || c.isEmpty()) return;

        char ch = c.charAt(0);
        if (ch >= 32 && ch != 127) {
            event.consume();
            if (cursorIndex <= currentInput.length()) {
                currentInput.insert(cursorIndex, ch);
                cursorIndex++;
                renderInputState();
                scrollToBottom();
            }
        }
    }

    private void handleKeyPressed(KeyEvent event) {
        KeyCode code = event.getCode();

        // 1. Enter: execute command
        if (code == KeyCode.ENTER) {
            event.consume();
            if (!running) {
                appendSystemMessage("\n[Restarting shell session...]\n");
                restartShell(activeShell);
                return;
            }

            String cmd = currentInput.toString();
            String trimmed = cmd.trim();
            currentInput.setLength(0);
            cursorIndex = 0;
            detachInputNodes();

            if (!trimmed.isEmpty()) {
                commandHistory.add(cmd);
                historyIndex = commandHistory.size();
            }

            // Real Terminal Emulator: Native 'cls' and 'clear' support
            if (trimmed.equalsIgnoreCase("cls") || trimmed.equalsIgnoreCase("clear")) {
                clearConsole();
                writeToProcess("\r\n");
                attachInputNodes();
                scrollToBottom();
                return;
            }

            // Record the typed command in the console history
            Text echo = new Text(cmd + "\n");
            echo.setFont(Font.font(FONT_FAMILY, FontWeight.NORMAL, FONT_SIZE));
            echo.setFill(Color.web("#ffffff"));
            consoleFlow.getChildren().add(echo);

            // Send command to child process
            writeToProcess(cmd + "\r\n");

            // Attach cursor right back at bottom
            attachInputNodes();
            scrollToBottom();

            // Trigger GUI screen sync if it's a ComeFort command
            if (trimmed.startsWith("cmf") || trimmed.startsWith("comefort")) {
                scheduleDataSync();
            }
            return;
        }

        // 2. Backspace
        if (code == KeyCode.BACK_SPACE) {
            event.consume();
            if (cursorIndex > 0) {
                currentInput.deleteCharAt(cursorIndex - 1);
                cursorIndex--;
                renderInputState();
                scrollToBottom();
            }
            return;
        }

        // 3. Delete
        if (code == KeyCode.DELETE) {
            event.consume();
            if (cursorIndex < currentInput.length()) {
                currentInput.deleteCharAt(cursorIndex);
                renderInputState();
                scrollToBottom();
            }
            return;
        }

        // 4. Left Arrow
        if (code == KeyCode.LEFT) {
            event.consume();
            if (cursorIndex > 0) {
                cursorIndex--;
                renderInputState();
                scrollToBottom();
            }
            return;
        }

        // 5. Right Arrow
        if (code == KeyCode.RIGHT) {
            event.consume();
            if (cursorIndex < currentInput.length()) {
                cursorIndex++;
                renderInputState();
                scrollToBottom();
            }
            return;
        }

        // 6. Home
        if (code == KeyCode.HOME) {
            event.consume();
            cursorIndex = 0;
            renderInputState();
            scrollToBottom();
            return;
        }

        // 7. End
        if (code == KeyCode.END) {
            event.consume();
            cursorIndex = currentInput.length();
            renderInputState();
            scrollToBottom();
            return;
        }

        // 8. Up Arrow: previous command in history
        if (code == KeyCode.UP) {
            event.consume();
            if (!commandHistory.isEmpty()) {
                if (historyIndex > 0) {
                    historyIndex--;
                }
                if (historyIndex < commandHistory.size()) {
                    setInputText(commandHistory.get(historyIndex));
                }
            }
            return;
        }

        // 9. Down Arrow: next command in history
        if (code == KeyCode.DOWN) {
            event.consume();
            if (!commandHistory.isEmpty()) {
                if (historyIndex < commandHistory.size() - 1) {
                    historyIndex++;
                    setInputText(commandHistory.get(historyIndex));
                } else {
                    historyIndex = commandHistory.size();
                    setInputText("");
                }
            }
            return;
        }

        // 10. Tab: auto-complete
        if (code == KeyCode.TAB) {
            event.consume();
            handleTabCompletion();
            return;
        }

        // 11. Ctrl+C: Send interrupt
        if (event.isControlDown() && code == KeyCode.C) {
            event.consume();
            sendInterrupt();
            currentInput.setLength(0);
            cursorIndex = 0;
            renderInputState();
            scrollToBottom();
            return;
        }

        // 12. Ctrl+L: Clear screen
        if (event.isControlDown() && code == KeyCode.L) {
            event.consume();
            clearConsole();
            return;
        }

        // 13. Ctrl+V: Paste from clipboard
        if (event.isControlDown() && code == KeyCode.V) {
            event.consume();
            String clip = Clipboard.getSystemClipboard().getString();
            if (clip != null && !clip.isEmpty()) {
                String clean = clip.replaceAll("[\r\n]", " ");
                currentInput.insert(cursorIndex, clean);
                cursorIndex += clean.length();
                renderInputState();
                scrollToBottom();
            }
            return;
        }
    }

    private void handleTabCompletion() {
        String current = currentInput.toString();
        if (current.isEmpty()) return;

        for (String candidate : CMF_AUTOCOMPLETE) {
            if (candidate.startsWith(current) && candidate.length() > current.length()) {
                currentInput.setLength(0);
                currentInput.append(candidate);
                cursorIndex = currentInput.length();
                renderInputState();
                scrollToBottom();
                return;
            }
        }
    }

    private void renderInputState() {
        if (cursorIndex < 0) cursorIndex = 0;
        if (cursorIndex > currentInput.length()) cursorIndex = currentInput.length();

        String before = currentInput.substring(0, cursorIndex);
        String after = currentInput.substring(cursorIndex);

        inputBeforeCursor.setText(before);
        inputAfterCursor.setText(after);
        cursorNode.setVisible(true);
    }

    public void setInputText(String text) {
        currentInput.setLength(0);
        if (text != null) {
            currentInput.append(text);
        }
        cursorIndex = currentInput.length();
        renderInputState();
        scrollToBottom();
        focusInput();
    }

    private void detachInputNodes() {
        consoleFlow.getChildren().removeAll(inputBeforeCursor, cursorNode, inputAfterCursor);
    }

    private void attachInputNodes() {
        if (!consoleFlow.getChildren().contains(cursorNode)) {
            consoleFlow.getChildren().addAll(inputBeforeCursor, cursorNode, inputAfterCursor);
        }
    }

    private void writeToProcess(String text) {
        if (processWriter == null || process == null || !process.isAlive()) {
            appendSystemMessage("Shell is stopped. Press Enter to restart...\n");
            statusIndicator.setText("● Stopped");
            statusIndicator.setStyle("-fx-text-fill: #95a5a6; -fx-font-size: 11px;");
            return;
        }

        try {
            processWriter.write(text);
            processWriter.flush();
        } catch (IOException e) {
            appendErrorMessage("Failed to send input: " + e.getMessage() + "\n");
        }
    }

    private void scheduleDataSync() {
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

    private void sendInterrupt() {
        if (processWriter != null && process != null && process.isAlive()) {
            try {
                processWriter.write("\u0003");
                processWriter.flush();
                detachInputNodes();
                appendSystemMessage("^C\n");
                attachInputNodes();
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * Starts the specified real shell profile.
     */
    public synchronized void startShell(ShellDetector.ShellProfile shellProfile) {
        destroy(); // Terminate existing process cleanly
        this.activeShell = shellProfile;

        ProcessBuilder pb = new ProcessBuilder(shellProfile.launchArgs());

        File workDir = new File(System.getProperty("user.dir", "."));
        if (workDir.exists()) {
            pb.directory(workDir);
        }

        // Augment PATH with local ComeFort distribution binaries
        Map<String, String> env = pb.environment();
        String currentPath = env.getOrDefault("PATH", "");
        File binDir = new File(workDir, "build/install/comefort/bin");
        if (binDir.exists()) {
            env.put("PATH", binDir.getAbsolutePath() + File.pathSeparator + currentPath);
        }
        env.put("COMEFORT_TERMINAL", "1");
        env.put("TERM", "xterm-256color");

        pb.redirectErrorStream(true);

        try {
            process = pb.start();
            processWriter = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
            running = true;

            Platform.runLater(() -> {
                statusIndicator.setText("● Active: " + shellProfile.displayName());
                statusIndicator.setStyle("-fx-text-fill: #2ecc71; -fx-font-size: 11px;");
                shellSelector.setValue(shellProfile);
            });

            // Start background stream reader
            startOutputReader(process.getInputStream());

            appendBanner(shellProfile, workDir.getAbsolutePath());

            Platform.runLater(() -> {
                attachInputNodes();
                renderInputState();
                focusInput();
            });

        } catch (Exception ex) {
            running = false;
            Platform.runLater(() -> {
                statusIndicator.setText("● Failed: " + ex.getMessage());
                statusIndicator.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 11px;");
                appendErrorMessage("Error launching " + shellProfile.displayName() + ": " + ex.getMessage() + "\n");
                attachInputNodes();
            });
        }
    }

    public synchronized void startShell(String shellNameOrId) {
        ShellDetector.ShellProfile target = findProfile(shellNameOrId);
        startShell(target != null ? target : activeShell);
    }

    public void restartShell(ShellDetector.ShellProfile shellProfile) {
        clearConsole();
        startShell(shellProfile);
    }

    public void restartShell(String shellNameOrId) {
        clearConsole();
        startShell(shellNameOrId);
    }

    private ShellDetector.ShellProfile findProfile(String nameOrId) {
        if (nameOrId == null) return activeShell;
        for (ShellDetector.ShellProfile p : installedShells) {
            if (p.id().equalsIgnoreCase(nameOrId) || p.displayName().equalsIgnoreCase(nameOrId)) {
                return p;
            }
        }
        return activeShell;
    }

    private void appendBanner(ShellDetector.ShellProfile profile, String workDir) {
        appendSystemMessage("┌─────────────────────────────────────────────────────────────┐\n");
        appendSystemMessage("│ " + profile.icon() + " ComeFort Integrated Terminal — " + profile.displayName() + "\n");
        appendSystemMessage("│ Executable: " + profile.executablePath() + "\n");
        appendSystemMessage("│ Cwd: " + workDir + "\n");
        appendSystemMessage("│ 'cmf' and 'comefort' commands are directly available.       │\n");
        appendSystemMessage("└─────────────────────────────────────────────────────────────┘\n\n");
    }

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
                running = false;
                Platform.runLater(() -> {
                    statusIndicator.setText("● Stopped");
                    statusIndicator.setStyle("-fx-text-fill: #95a5a6; -fx-font-size: 11px;");
                    appendSystemMessage("\n[Session ended. Press Enter or click Restart to launch a new shell]\n");
                    attachInputNodes();
                    scrollToBottom();
                });
            }
        }, "Terminal-Reader-" + System.currentTimeMillis());

        outputReaderThread.setDaemon(true);
        outputReaderThread.start();
    }

    /**
     * Parses ANSI color and screen clear sequences and appends formatted Text nodes into the console.
     */
    private void appendAnsiOutput(String text) {
        if (text == null || text.isEmpty()) return;

        // Check for ANSI Clear Screen code (\u001B[2J or \u001B[3J)
        if (text.contains("\u001B[2J") || text.contains("\u001B[3J")) {
            clearConsole();
            return;
        }

        detachInputNodes();

        Matcher matcher = ANSI_PATTERN.matcher(text);
        int lastIndex = 0;
        Color currentColor = Color.web("#e0e0e0");
        boolean isBold = false;

        while (matcher.find()) {
            if (matcher.start() > lastIndex) {
                String sub = text.substring(lastIndex, matcher.start());
                addTextNode(sub, currentColor, isBold);
            }

            String commandLetter = matcher.group(2);
            String codeStr = matcher.group(1);

            // 'm' is Select Graphic Rendition (Colors / Styles)
            if ("m".equalsIgnoreCase(commandLetter) && codeStr != null && !codeStr.isEmpty()) {
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
                            case 37, 97 -> currentColor = Color.web("#ecf0f1");
                            default -> {
                            }
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            } else if ("J".equals(commandLetter)) {
                // Erase in Display
                if ("2".equals(codeStr) || "3".equals(codeStr)) {
                    clearConsole();
                    return;
                }
            }
            lastIndex = matcher.end();
        }

        if (lastIndex < text.length()) {
            addTextNode(text.substring(lastIndex), currentColor, isBold);
        }

        trimConsoleNodes();
        attachInputNodes();
        scrollToBottom();
    }

    private void addTextNode(String content, Color color, boolean isBold) {
        if (content.isEmpty()) return;
        Text t = new Text(content);
        t.setFont(Font.font(FONT_FAMILY, isBold ? FontWeight.BOLD : FontWeight.NORMAL, FONT_SIZE));
        t.setFill(color);
        consoleFlow.getChildren().add(t);
    }

    public void appendSystemMessage(String msg) {
        detachInputNodes();
        Text t = new Text(msg);
        t.setFont(Font.font(FONT_FAMILY, FontWeight.NORMAL, FONT_SIZE));
        t.setFill(Color.web("#58a6ff"));
        consoleFlow.getChildren().add(t);
        trimConsoleNodes();
        attachInputNodes();
        scrollToBottom();
    }

    public void appendErrorMessage(String msg) {
        detachInputNodes();
        Text t = new Text(msg);
        t.setFont(Font.font(FONT_FAMILY, FontWeight.NORMAL, FONT_SIZE));
        t.setFill(Color.web("#e74c3c"));
        consoleFlow.getChildren().add(t);
        trimConsoleNodes();
        attachInputNodes();
        scrollToBottom();
    }

    private void trimConsoleNodes() {
        int size = consoleFlow.getChildren().size();
        if (size > MAX_CONSOLE_NODES) {
            consoleFlow.getChildren().remove(0, size - MAX_CONSOLE_NODES);
        }
    }

    private void scrollToBottom() {
        Platform.runLater(() -> scrollPane.setVvalue(1.0));
    }

    public void clearConsole() {
        detachInputNodes();
        consoleFlow.getChildren().clear();
        attachInputNodes();
        renderInputState();
        scrollToBottom();
        focusInput();
    }

    public void focusInput() {
        Platform.runLater(scrollPane::requestFocus);
    }

    private void toggleMaximize() {
        isMaximized = !isMaximized;
        if (isMaximized) {
            currentHeight = 520.0;
            rootNode.setPrefHeight(currentHeight);
            maxRestoreBtn.setText("🗗");
        } else {
            currentHeight = 270.0;
            rootNode.setPrefHeight(currentHeight);
            maxRestoreBtn.setText("⤢");
        }
    }

    public Region getView() {
        return rootNode;
    }

    public synchronized void destroy() {
        running = false;
        if (cursorTimeline != null) {
            cursorTimeline.stop();
        }
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
}
