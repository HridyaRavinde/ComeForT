package com.akshit.comefort.cli.commands;

import com.akshit.comefort.cli.CliFormatter;
import com.akshit.comefort.core.Capture;
import com.akshit.comefort.core.Note;
import com.akshit.comefort.core.Project;
import com.akshit.comefort.core.Task;
import com.akshit.comefort.service.SearchService;
import com.akshit.comefort.service.SearchService.SearchResults;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.util.List;

/**
 * Cross-entity search command.
 * Searches across tasks, projects, notes, and captures.
 */
@Command(
        name = "search",
        description = "Search across everything",
        mixinStandardHelpOptions = true
)
public class SearchCommand implements Runnable {

    private final SearchService searchService;
    private final CliFormatter formatter;

    @Parameters(index = "0..*", description = "Search query")
    private List<String> queryWords;

    public SearchCommand(SearchService searchService, CliFormatter formatter) {
        this.searchService = searchService;
        this.formatter = formatter;
    }

    @Override
    public void run() {
        if (queryWords == null || queryWords.isEmpty()) {
            formatter.error("Specify a search query. Usage: cmf search <query> (or comefort search <query>)");
            return;
        }

        String query = String.join(" ", queryWords);
        SearchResults results = searchService.search(query);

        if (results.isEmpty()) {
            formatter.printSectionHeader("🔎 Search: \"" + query + "\"");
            formatter.empty("No results found.");
            formatter.newLine();
            return;
        }

        formatter.printSectionHeader("🔎 Search: \"" + query
                + "\" (" + results.totalCount() + " results)");

        // Projects
        if (!results.projects().isEmpty()) {
            System.out.println();
            System.out.println("  PROJECTS");
            for (Project p : results.projects()) {
                System.out.println("    " + p.getName()
                        + (p.getDescription() != null ? " — " + p.getDescription() : ""));
            }
        }

        // Tasks
        if (!results.tasks().isEmpty()) {
            System.out.println();
            System.out.println("  TASKS");
            for (Task t : results.tasks()) {
                formatter.printTaskLine(t, null);
            }
        }

        // Notes
        if (!results.notes().isEmpty()) {
            System.out.println();
            System.out.println("  NOTES");
            for (Note n : results.notes()) {
                formatter.printNoteLine(n, null);
            }
        }

        // Captures
        if (!results.captures().isEmpty()) {
            System.out.println();
            System.out.println("  CAPTURES");
            for (Capture c : results.captures()) {
                formatter.printCaptureLine(c);
            }
        }

        formatter.newLine();
    }
}
