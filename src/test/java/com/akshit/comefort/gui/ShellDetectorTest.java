package com.akshit.comefort.gui;

import com.akshit.comefort.gui.components.ShellDetector;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ShellDetector Dynamic Shell Discovery Tests")
class ShellDetectorTest {

    @Test
    @DisplayName("Dynamically detects real shells on host machine with valid executable paths")
    void testDynamicShellDetection() {
        List<ShellDetector.ShellProfile> shells = ShellDetector.detectInstalledShells();

        assertNotNull(shells, "Shell list must not be null");
        assertFalse(shells.isEmpty(), "Must detect at least one real shell on the host OS");

        System.out.println("Discovered real shells on host system:");
        for (ShellDetector.ShellProfile s : shells) {
            System.out.println(" -> [" + s.id() + "] " + s.displayName() + " | Path: " + s.executablePath());
            // Verify the executable file actually exists on the filesystem
            File f = new File(s.executablePath());
            assertTrue(f.exists(), "Executable for " + s.displayName() + " must physically exist on disk: " + s.executablePath());
        }

        ShellDetector.ShellProfile defaultShell = ShellDetector.getDefaultShell(shells);
        assertNotNull(defaultShell, "Must identify a default shell");
        System.out.println("Selected default shell: " + defaultShell.displayName());
    }
}
