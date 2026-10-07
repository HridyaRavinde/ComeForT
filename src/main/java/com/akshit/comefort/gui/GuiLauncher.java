package com.akshit.comefort.gui;

/**
 * Dedicated standalone entry point for JavaFX GUI.
 * Kept separate from MainWindow (which extends Application) to ensure compatibility
 * with jpackage, fat JARs, and non-modular classpaths.
 */
public final class GuiLauncher {

    private GuiLauncher() {
    }

    public static void main(String[] args) {
        MainWindow.launchGui(args);
    }
}
