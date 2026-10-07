package com.akshit.comefort.gui.components;

import com.pty4j.PtyProcess;
import com.pty4j.PtyProcessBuilder;
import com.pty4j.WinSize;
import com.techsenger.jeditermfx.core.Color;
import com.techsenger.jeditermfx.core.ProcessTtyConnector;
import com.techsenger.jeditermfx.core.TerminalColor;
import com.techsenger.jeditermfx.core.TextStyle;
import com.techsenger.jeditermfx.core.emulator.ColorPalette;
import com.techsenger.jeditermfx.core.emulator.ColorPaletteImpl;
import com.techsenger.jeditermfx.core.util.TermSize;
import com.techsenger.jeditermfx.ui.JediTermFxWidget;
import com.techsenger.jeditermfx.ui.settings.DefaultSettingsProvider;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.util.Callback;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Integrated Terminal Panel — Genuine Pseudo-Terminal (PTY / ConPTY) powered by JediTermFX
 * and Pty4J, directly connected to the host operating system's genuine shells (PowerShell 7,
 * Windows PowerShell, Command Prompt, Git Bash, WSL, etc.).
 *
 * <p>Key Features:
 *   - Genuine Pseudo-Terminal (PTY): Windows ConPTY / Unix PTY with zero fake piping.
 *   - Industrial Emulation: JediTerm engine (the exact same engine powering JetBrains IDE terminals).
 *   - Full Shell Features: PSReadLine, Tab completion, arrow command history, interactive REPLs (python, node, git commit, vim).
 *   - PATH Injection: Guarantees 'cmf' and 'comefort' CLI executables and Git are available in process PATH.
 *   - Dynamic Shell Switching: Lists genuinely detected shells installed on the device.
 *   - Real-time GUI Sync: Refreshes desktop GUI state after ComeFort CLI mutations.
 * </p>
 */
public class TerminalPanel {

    private final VBox rootNode;
    private Label statusIndicator;
    private ComboBox<ShellDetector.ShellProfile> shellSelector;
    private Button maxRestoreBtn;

    // Shell configuration
    private final List<ShellDetector.ShellProfile> installedShells;
    private ShellDetector.ShellProfile activeShell;

    // Terminal widget & process management
    private JediTermFxWidget terminalWidget;
    private PtyProcess ptyProcess;
    private PtyTtyConnector ttyConnector;
    private final Runnable onDataChanged;
    private final Runnable onCloseRequest;

    // Sizing
    private boolean isMaximized = false;
    private double currentHeight = 280.0;

    /**
     * Professional dark color palette inspired by VS Code Dark+ & GitHub Dark.
     */
    public static class ComeFortDarkColorPalette extends ColorPalette {

        private static final Color[] COLORS = new Color[]{
                new Color(18, 19, 23),      // 0: Black / Dark Base (#121317)
                new Color(255, 123, 114),   // 1: Red (#ff7b72)
                new Color(63, 185, 80),     // 2: Green (#3fb950)
                new Color(210, 153, 34),    // 3: Yellow (#d29922)
                new Color(88, 166, 255),    // 4: Blue (#58a6ff)
                new Color(188, 140, 255),   // 5: Magenta (#bc8cff)
                new Color(57, 197, 207),    // 6: Cyan (#39c5cf)
                new Color(209, 215, 222),   // 7: White / Text (#d1d7de)
                new Color(110, 118, 129),   // 8: Bright Black / Muted Gray (#6e7681)
                new Color(255, 161, 152),   // 9: Bright Red (#ffa198)
                new Color(86, 211, 100),    // 10: Bright Green (#56d364)
                new Color(227, 179, 65),    // 11: Bright Yellow (#e3b341)
                new Color(121, 192, 255),   // 12: Bright Blue (#79c0ff)
                new Color(210, 168, 255),   // 13: Bright Magenta (#d2a8ff)
                new Color(86, 212, 221),    // 14: Bright Cyan (#56d4dd)
                new Color(240, 246, 252)    // 15: Bright White (#f0f6fc)
        };

        @Override
        public Color getForegroundByColorIndex(int index) {
            if (index >= 0 && index < COLORS.length) {
                return COLORS[index];
            }
            return COLORS[7];
        }

        @Override
        public Color getBackgroundByColorIndex(int index) {
            if (index >= 0 && index < COLORS.length) {
                return COLORS[index];
            }
            return COLORS[0];
        }
    }

    /**
     * Custom settings provider for a sleek, modern developer terminal aesthetic.
     */
    public static class ComeFortTerminalSettings extends DefaultSettingsProvider {

        private static final TerminalColor DARK_BG = TerminalColor.rgb(18, 19, 23); // #121317
        private static final TerminalColor LIGHT_FG = TerminalColor.rgb(230, 237, 243); // #e6edf3

        @Override
        public Font getTerminalFont() {
            return Font.font("Cascadia Code", 13.0);
        }

        @Override
        public float getTerminalFontSize() {
            return 13.0f;
        }

        @Override
        public ColorPalette getTerminalColorPalette() {
            return new ComeFortDarkColorPalette();
        }

        @Override
        public TerminalColor getDefaultForeground() {
            return LIGHT_FG;
        }

        @Override
        public TerminalColor getDefaultBackground() {
            return DARK_BG;
        }

        @Override
        public TextStyle getDefaultStyle() {
            return new TextStyle(LIGHT_FG, DARK_BG);
        }

        @Override
        public TextStyle getSelectionColor() {
            return new TextStyle(
                    TerminalColor.rgb(255, 255, 255),
                    TerminalColor.rgb(56, 139, 253)
            );
        }

        @Override
        public boolean useAntialiasing() {
            return true;
        }

        @Override
        public int caretBlinkingMs() {
            return 500;
        }

        @Override
        public boolean scrollToBottomOnTyping() {
            return true;
        }

        @Override
        public int getBufferMaxLinesCount() {
            return 5000;
        }
    }

    /**
     * Bridges JediTermFX to Pty4J's PtyProcess and handles terminal resizing and CLI synchronization.
     */
    private class PtyTtyConnector extends ProcessTtyConnector {

        private final PtyProcess process;
        private final StringBuilder commandMonitor = new StringBuilder();

        public PtyTtyConnector(PtyProcess process) {
            super(process, StandardCharsets.UTF_8);
            this.process = process;
        }

        @Override
        public String getName() {
            return "PTY";
        }

        @Override
        public void resize(TermSize termSize) {
            if (process != null && process.isAlive()) {
                try {
                    int cols = Math.max(20, termSize.getColumns());
                    int rows = Math.max(5, termSize.getRows());
                    process.setWinSize(new WinSize(cols, rows));
                } catch (Exception ignored) {
                }
            }
        }

        @Override
        public void write(byte[] bytes) throws IOException {
            // Monitor typed characters to trigger GUI sync on ComeFort mutations
            for (byte b : bytes) {
                if (b == '\r' || b == '\n') {
                    String cmd = commandMonitor.toString().trim();
                    commandMonitor.setLength(0);
                    if (cmd.startsWith("cmf") || cmd.startsWith("comefort")) {
                        scheduleDataSync();
                    }
                } else if (b == 8 || b == 127) { // Backspace
                    if (commandMonitor.length() > 0) {
                        commandMonitor.setLength(commandMonitor.length() - 1);
                    }
                } else if (b >= 32) {
                    commandMonitor.append((char) b);
                }
            }
            super.write(bytes);
        }
    }

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

        // 2. Initialize JediTermFX Widget
        ComeFortTerminalSettings settings = new ComeFortTerminalSettings();
        this.terminalWidget = new JediTermFxWidget(settings);

        // Title listener from shell process
        this.terminalWidget.getTerminal().addApplicationTitleListener(title -> {
            if (title != null && !title.isBlank()) {
                Platform.runLater(() -> {
                    if (statusIndicator != null && activeShell != null) {
                        statusIndicator.setText("● " + activeShell.displayName() + " [" + title.trim() + "]");
                    }
                });
            }
        });

        // Embed terminal pane into VBox
        Pane terminalPane = terminalWidget.getPane();
        terminalPane.setStyle("-fx-background-color: #121317;");
        VBox.setVgrow(terminalPane, Priority.ALWAYS);

        rootNode.getChildren().addAll(header, terminalPane);

        // Focus handling
        rootNode.setOnMouseClicked(e -> focusInput());
        terminalPane.setOnMouseClicked(e -> focusInput());

        // Start active shell session
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

        statusIndicator = new Label("● Initializing...");
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
        addQuickMenuItem(quickCmds, "cmf today", "cmf today\r");
        addQuickMenuItem(quickCmds, "cmf inbox", "cmf inbox\r");
        addQuickMenuItem(quickCmds, "cmf task list", "cmf task list\r");
        addQuickMenuItem(quickCmds, "cmf project list", "cmf project list\r");
        addQuickMenuItem(quickCmds, "cmf note list", "cmf note list\r");
        addQuickMenuItem(quickCmds, "cmf status", "cmf status\r");
        addQuickMenuItem(quickCmds, "cmf --help", "cmf --help\r");

        // Clear button
        Button clearBtn = new Button("🗑 Clear");
        clearBtn.getStyleClass().add("terminal-btn");
        clearBtn.setTooltip(new Tooltip("Clear terminal screen"));
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

    private void addQuickMenuItem(MenuButton menu, String label, String commandWithEnter) {
        MenuItem item = new MenuItem(label);
        item.setOnAction(e -> {
            sendInput(commandWithEnter);
            focusInput();
        });
        menu.getItems().add(item);
    }

    /**
     * Starts the specified real shell profile via Pty4J and connects it to JediTerm.
     */
    public synchronized void startShell(ShellDetector.ShellProfile shellProfile) {
        destroyProcessOnly();
        this.activeShell = shellProfile;

        File workDir = new File(System.getProperty("user.dir", "."));

        // Augment PATH with local ComeFort distribution binaries & Git
        Map<String, String> env = new HashMap<>(System.getenv());
        String currentPath = env.get("Path") != null ? env.get("Path") : env.getOrDefault("PATH", "");

        List<String> pathPrefixes = new ArrayList<>();
        File binDir = new File(workDir, "build/install/comefort/bin");
        if (binDir.exists()) {
            pathPrefixes.add(binDir.getAbsolutePath());
        }

        // Ensure Git is present in PATH
        if (!currentPath.toLowerCase().contains("git\\cmd") && !currentPath.toLowerCase().contains("git/cmd")) {
            File gitCmd = new File("C:\\Program Files\\Git\\cmd");
            if (gitCmd.exists()) {
                pathPrefixes.add(gitCmd.getAbsolutePath());
            }
        }

        if (!pathPrefixes.isEmpty()) {
            String updatedPath = String.join(File.pathSeparator, pathPrefixes) + File.pathSeparator + currentPath;
            env.put("PATH", updatedPath);
            env.put("Path", updatedPath);
        }

        env.put("COMEFORT_TERMINAL", "1");
        env.put("TERM", "xterm-256color");
        env.put("COLORTERM", "truecolor");

        try {
            String[] cmdArray = shellProfile.launchArgs().toArray(new String[0]);
            PtyProcessBuilder pb = new PtyProcessBuilder()
                    .setCommand(cmdArray)
                    .setEnvironment(env)
                    .setDirectory(workDir.getAbsolutePath())
                    .setInitialColumns(100)
                    .setInitialRows(28);

            ptyProcess = pb.start();
            ttyConnector = new PtyTtyConnector(ptyProcess);

            terminalWidget.setTtyConnector(ttyConnector);
            terminalWidget.start();

            Platform.runLater(() -> {
                statusIndicator.setText("● Active: " + shellProfile.displayName());
                statusIndicator.setStyle("-fx-text-fill: #2ecc71; -fx-font-size: 11px;");
                shellSelector.setValue(shellProfile);
                focusInput();
            });

        } catch (Exception ex) {
            Platform.runLater(() -> {
                statusIndicator.setText("● Failed: " + ex.getMessage());
                statusIndicator.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 11px;");
                appendErrorMessage("Error launching " + shellProfile.displayName() + ": " + ex.getMessage() + "\r\n");
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

    public void sendInput(String text) {
        if (ttyConnector != null && ttyConnector.isConnected()) {
            try {
                ttyConnector.write(text);
            } catch (IOException ignored) {
            }
        }
    }

    private void scheduleDataSync() {
        if (onDataChanged != null) {
            new Thread(() -> {
                try {
                    Thread.sleep(600);
                } catch (InterruptedException ignored) {
                }
                Platform.runLater(onDataChanged);
            }, "GUI-Sync-Timer").start();
        }
    }

    public void appendSystemMessage(String msg) {
        if (msg == null || msg.isEmpty()) return;
        Platform.runLater(() -> {
            try {
                terminalWidget.getTerminal().writeUnwrappedString(msg);
            } catch (Exception ignored) {
            }
        });
    }

    public void appendErrorMessage(String msg) {
        if (msg == null || msg.isEmpty()) return;
        Platform.runLater(() -> {
            try {
                terminalWidget.getTerminal().writeUnwrappedString("\r\n[Error] " + msg + "\r\n");
            } catch (Exception ignored) {
            }
        });
    }

    public void clearConsole() {
        Platform.runLater(() -> {
            try {
                terminalWidget.getTerminal().clearScreen();
                sendInput("\r");
            } catch (Exception ignored) {
            }
        });
    }

    public void focusInput() {
        Platform.runLater(() -> {
            Node focusable = terminalWidget.getPreferredFocusableNode();
            if (focusable != null) {
                focusable.requestFocus();
            }
        });
    }

    private void toggleMaximize() {
        isMaximized = !isMaximized;
        if (isMaximized) {
            currentHeight = 540.0;
            rootNode.setPrefHeight(currentHeight);
            maxRestoreBtn.setText("🗗");
        } else {
            currentHeight = 280.0;
            rootNode.setPrefHeight(currentHeight);
            maxRestoreBtn.setText("⤢");
        }
        focusInput();
    }

    public Region getView() {
        return rootNode;
    }

    private synchronized void destroyProcessOnly() {
        if (terminalWidget != null && terminalWidget.isSessionRunning()) {
            terminalWidget.stop();
        }
        if (ptyProcess != null) {
            ptyProcess.destroyForcibly();
            ptyProcess = null;
        }
        if (ttyConnector != null) {
            ttyConnector.close();
            ttyConnector = null;
        }
    }

    public synchronized void destroy() {
        destroyProcessOnly();
        if (terminalWidget != null) {
            terminalWidget.close();
        }
    }
}
