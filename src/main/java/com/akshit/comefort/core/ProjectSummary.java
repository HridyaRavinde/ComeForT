package com.akshit.comefort.core;

/**
 * Aggregated summary of a project with task and note counts
 * retrieved via a single optimized SQL query.
 */
public record ProjectSummary(
        Project project,
        int totalTasks,
        int openTasks,
        int totalNotes
) {
}
