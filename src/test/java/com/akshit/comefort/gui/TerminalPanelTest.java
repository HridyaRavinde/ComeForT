package com.akshit.comefort.gui;

import com.akshit.comefort.gui.components.TerminalPanel;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TerminalPanel Component Tests")
class TerminalPanelTest {

    @BeforeAll
    static void initJavaFX() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Toolkit already initialized
        }
    }

    @Test
    @DisplayName("TerminalPanel initializes UI, starts background shell, and cleanly destroys")
    void testTerminalPanelLifecycle() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean success = new AtomicBoolean(false);

        Platform.runLater(() -> {
            try {
                AtomicBoolean dataChanged = new AtomicBoolean(false);
                AtomicBoolean closed = new AtomicBoolean(false);
                TerminalPanel panel = new TerminalPanel(() -> dataChanged.set(true), () -> closed.set(true));

                assertNotNull(panel.getView(), "TerminalPanel view must not be null");
                panel.clearConsole();
                panel.appendSystemMessage("Test message");

                panel.destroy();
                success.set(true);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        });

        boolean completed = latch.await(6, TimeUnit.SECONDS);
        assertTrue(completed, "Terminal panel creation timed out");
        assertTrue(success.get(), "Terminal panel creation failed");
    }
}
