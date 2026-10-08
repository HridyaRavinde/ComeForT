package com.akshit.comefort.gui;

import javafx.application.Platform;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Panel Toggle (Ctrl+B & Ctrl+J) Tests")
class PanelToggleTest {

    @BeforeAll
    static void initJavaFX() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Already started
        }
        Platform.setImplicitExit(false);
    }

    @Test
    @DisplayName("Primary panel (Ctrl+B) and bottom panel (Ctrl+J) toggle states correctly")
    void testPanelToggles() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean success = new AtomicBoolean(false);

        Platform.runLater(() -> {
            try {
                MainWindow window = new MainWindow();
                Stage stage = new Stage();
                window.start(stage);

                // Initial state
                assertTrue(window.isSidebarVisible(), "Primary panel should be visible by default");
                assertFalse(window.isTerminalVisible(), "Bottom panel should be hidden by default");

                // Toggle primary panel (Ctrl+B)
                window.togglePrimaryPanel();
                assertFalse(window.isSidebarVisible(), "Primary panel should be hidden after Ctrl+B toggle");

                window.togglePrimaryPanel();
                assertTrue(window.isSidebarVisible(), "Primary panel should be restored after second Ctrl+B toggle");

                // Toggle bottom panel (Ctrl+J)
                window.toggleTerminal();
                assertTrue(window.isTerminalVisible(), "Bottom panel should be visible after Ctrl+J toggle");

                window.toggleTerminal();
                assertFalse(window.isTerminalVisible(), "Bottom panel should be hidden after second Ctrl+J toggle");

                window.stop();
                stage.close();
                success.set(true);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        });

        boolean completed = latch.await(8, TimeUnit.SECONDS);
        assertTrue(completed, "Panel toggle test timed out");
        assertTrue(success.get(), "Panel toggle test failed");
    }
}
