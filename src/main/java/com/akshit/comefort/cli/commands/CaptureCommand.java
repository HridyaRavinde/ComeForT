package com.akshit.comefort.cli.commands;

import com.akshit.comefort.cli.CliFormatter;
import com.akshit.comefort.core.Capture;
import com.akshit.comefort.service.CaptureService;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.util.List;

/**
 * Quick capture command — the heart of ComeFort.
 * Usage: cf c "HoneyChain needs better fraud alerts"
 */
@Command(
        name = "c",
        aliases = {"capture"},
        description = "Quick capture — dump a thought into your inbox",
        mixinStandardHelpOptions = true
)
public class CaptureCommand implements Runnable {

    private final CaptureService captureService;
    private final CliFormatter formatter;

    @Parameters(index = "0..*", description = "The text to capture")
    private List<String> words;

    public CaptureCommand(CaptureService captureService, CliFormatter formatter) {
        this.captureService = captureService;
        this.formatter = formatter;
    }

    @Override
    public void run() {
        if (words == null || words.isEmpty()) {
            formatter.error("Nothing to capture. Usage: cf c \"your thought here\"");
            return;
        }

        String content = String.join(" ", words);
        Capture capture = captureService.capture(content);

        formatter.success("Captured: " + capture.getContent());
    }
}
