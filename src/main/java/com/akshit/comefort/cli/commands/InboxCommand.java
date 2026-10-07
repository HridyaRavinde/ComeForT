package com.akshit.comefort.cli.commands;

import com.akshit.comefort.cli.CliFormatter;
import com.akshit.comefort.core.Capture;
import com.akshit.comefort.service.CaptureService;
import picocli.CommandLine.Command;

import java.util.List;

/**
 * Displays unprocessed inbox captures.
 */
@Command(
        name = "inbox",
        description = "Show unprocessed captures in your inbox",
        mixinStandardHelpOptions = true
)
public class InboxCommand implements Runnable {

    private final CaptureService captureService;
    private final CliFormatter formatter;

    public InboxCommand(CaptureService captureService, CliFormatter formatter) {
        this.captureService = captureService;
        this.formatter = formatter;
    }

    @Override
    public void run() {
        List<Capture> inbox = captureService.getInbox();

        if (inbox.isEmpty()) {
            formatter.printSectionHeader("📥 Inbox");
            formatter.empty("Your inbox is empty. Capture something with: cf c \"your thought\"");
            formatter.newLine();
            return;
        }

        formatter.printSectionHeader("📥 Inbox (" + inbox.size() + " items)");
        for (Capture capture : inbox) {
            formatter.printCaptureLine(capture);
        }
        formatter.newLine();
    }
}
