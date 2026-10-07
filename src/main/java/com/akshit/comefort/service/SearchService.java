package com.akshit.comefort.service;

import com.akshit.comefort.core.Capture;
import com.akshit.comefort.core.Note;
import com.akshit.comefort.core.Project;
import com.akshit.comefort.core.Task;
import com.akshit.comefort.repository.CaptureRepository;
import com.akshit.comefort.repository.NoteRepository;
import com.akshit.comefort.repository.ProjectRepository;
import com.akshit.comefort.repository.TaskRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Cross-entity search service — searches across tasks, projects, notes, and captures.
 */
public class SearchService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final NoteRepository noteRepository;
    private final CaptureRepository captureRepository;

    public SearchService(TaskRepository taskRepository,
                         ProjectRepository projectRepository,
                         NoteRepository noteRepository,
                         CaptureRepository captureRepository) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.noteRepository = noteRepository;
        this.captureRepository = captureRepository;
    }

    /**
     * Holds categorized search results across all entity types.
     */
    public record SearchResults(
            List<Task> tasks,
            List<Project> projects,
            List<Note> notes,
            List<Capture> captures
    ) {
        public boolean isEmpty() {
            return tasks.isEmpty() && projects.isEmpty()
                    && notes.isEmpty() && captures.isEmpty();
        }

        public int totalCount() {
            return tasks.size() + projects.size() + notes.size() + captures.size();
        }
    }

    /**
     * Performs a text search across all entity types.
     * Returns categorized results.
     */
    public SearchResults search(String query) {
        if (query == null || query.isBlank()) {
            return new SearchResults(
                    List.of(), List.of(), List.of(), List.of());
        }

        List<Task> tasks = taskRepository.searchByText(query);
        List<Project> projects = projectRepository.searchByText(query);
        List<Note> notes = noteRepository.searchByText(query);
        List<Capture> captures = captureRepository.searchByText(query);

        return new SearchResults(tasks, projects, notes, captures);
    }
}
