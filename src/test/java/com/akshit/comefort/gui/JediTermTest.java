package com.akshit.comefort.gui;

import com.pty4j.PtyProcess;
import com.pty4j.PtyProcessBuilder;
import com.pty4j.WinSize;
import com.techsenger.jeditermfx.core.ProcessTtyConnector;
import com.techsenger.jeditermfx.core.util.TermSize;
import com.techsenger.jeditermfx.ui.JediTermFxWidget;
import com.techsenger.jeditermfx.ui.settings.DefaultSettingsProvider;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JediTermFX Integration Tests")
class JediTermTest {

    @BeforeAll
    static void initFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
        }
    }

    @Test
    @DisplayName("JediTermFxWidget initializes, binds to PtyProcess via ProcessTtyConnector, and runs")
    void testJediTermWidget() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean success = new AtomicBoolean(false);

        Platform.runLater(() -> {
            try {
                boolean isWin = System.getProperty("os.name", "").toLowerCase().contains("win");
                String[] cmd = isWin ? new String[]{"cmd.exe"} : new String[]{"/bin/sh"};

                Map<String, String> env = new HashMap<>(System.getenv());
                env.put("TERM", "xterm-256color");

                PtyProcess ptyProcess = new PtyProcessBuilder()
                        .setCommand(cmd)
                        .setEnvironment(env)
                        .setInitialColumns(80)
                        .setInitialRows(24)
                        .start();

                ProcessTtyConnector connector = new ProcessTtyConnector(ptyProcess, StandardCharsets.UTF_8) {
                    @Override
                    public String getName() {
                        return "PTY";
                    }

                    @Override
                    public void resize(TermSize termSize) {
                        try {
                            ptyProcess.setWinSize(new WinSize(termSize.getColumns(), termSize.getRows()));
                        } catch (Exception ignored) {
                        }
                    }
                };

                com.akshit.comefort.gui.components.TerminalPanel.ComeFortTerminalSettings settings =
                        new com.akshit.comefort.gui.components.TerminalPanel.ComeFortTerminalSettings();
                JediTermFxWidget widget = new JediTermFxWidget(settings);
                widget.setTtyConnector(connector);
                widget.start();

                assertNotNull(widget.getPane(), "Terminal widget Pane must not be null");
                assertTrue(widget.isSessionRunning(), "Terminal session must be running");

                javafx.scene.paint.Color bg = widget.getTerminalPanel().getBackground();
                System.out.println("TerminalPanel Background Color: " + bg);
                assertEquals(javafx.scene.paint.Color.web("#121317"), bg, "Background color must be #121317");

                widget.stop();
                widget.close();
                ptyProcess.destroyForcibly();

                success.set(true);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        });

        boolean completed = latch.await(6, TimeUnit.SECONDS);
        assertTrue(completed, "JediTerm widget creation timed out");
        assertTrue(success.get(), "JediTerm widget creation failed");
    }
}
