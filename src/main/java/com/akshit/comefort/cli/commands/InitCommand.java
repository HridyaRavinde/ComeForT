package com.akshit.comefort.cli.commands;

import com.akshit.comefort.cli.CliFormatter;
import com.akshit.comefort.db.DatabaseManager;
import picocli.CommandLine.Command;

/**
 * Initializes the ComeFort database.
 * Creates ~/.comefort/ directory and the SQLite database file.
 */
@Command(
        name = "init",
        description = "Initialize ComeFort — create the local database",
        mixinStandardHelpOptions = true
)
public class InitCommand implements Runnable {

    private final DatabaseManager dbManager;
    private final CliFormatter formatter;

    public InitCommand(DatabaseManager dbManager, CliFormatter formatter) {
        this.dbManager = dbManager;
        this.formatter = formatter;
    }

    @Override
    public void run() {
        if (dbManager.isDatabaseInitialized()) {
            formatter.info("ComeFort is already initialized at: "
                    + dbManager.getDbFilePath());
            return;
        }

        boolean isNew = dbManager.initialize();

        if (isNew) {
            formatter.printBanner();
            formatter.newLine();
            formatter.success("ComeFort initialized successfully!");
            formatter.info("Database: " + dbManager.getDbFilePath());
            formatter.newLine();
            formatter.info("Get started (use either 'cmf' or 'comefort'):");
            System.out.println("    cmf c \"your first thought\"     — Quick capture");
            System.out.println("    cmf project add MyProject      — Create a project");
            System.out.println("    cmf task add \"Do something\"    — Add a task");
            System.out.println("    cmf today                      — See your dashboard");
            formatter.newLine();
        } else {
            formatter.success("ComeFort database verified.");
        }
    }
}
