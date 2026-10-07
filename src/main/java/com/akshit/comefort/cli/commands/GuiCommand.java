package com.akshit.comefort.cli.commands;

import com.akshit.comefort.gui.MainWindow;
import picocli.CommandLine.Command;

/**
 * CLI command to launch the ComeFort desktop GUI application.
 * Usage: cf gui
 */
@Command(
        name = "gui",
        description = "Launch the ComeFort desktop GUI application",
        mixinStandardHelpOptions = true
)
public class GuiCommand implements Runnable {

    @Override
    public void run() {
        MainWindow.launchGui(new String[]{});
    }
}
