package com.akshit.comefort.service;

import com.akshit.comefort.core.Capture;
import com.akshit.comefort.core.Note;
import com.akshit.comefort.core.Task;
import com.akshit.comefort.core.enums.ActionType;
import com.akshit.comefort.core.enums.EntityType;
import com.akshit.comefort.core.enums.TaskPriority;
import com.akshit.comefort.exception.AmbiguousEntityException;
import com.akshit.comefort.exception.EntityNotFoundException;
import com.akshit.comefort.repository.CaptureRepository;
import com.akshit.comefort.util.IdGenerator;
import com.akshit.comefort.util.InputParser;

import java.time.LocalDate;
import java.util.List;

/**
 * Business logic for quick captures — the heart of ComeFort.
 * Captures are raw thoughts dumped into the inbox for later processing.
 */
public class CaptureService {

    private final CaptureRepository captureRepository;
    private final ActivityService activityService;
    private TaskService taskService;
    private NoteService noteService;

    public CaptureService(CaptureRepository captureRepository, ActivityService activityService) {
        this(captureRepository, activityService, null, null);
    }

    public CaptureService(CaptureRepository captureRepository, ActivityService activityService,
                          TaskService taskService, NoteService noteService) {
        this.captureRepository = captureRepository;
        this.activityService = activityService;
        this.taskService = taskService;
        this.noteService = noteService;
    }

    public void setTaskService(TaskService taskService) {
        this.taskService = taskService;
    }

    public void setNoteService(NoteService noteService) {
        this.noteService = noteService;
    }

    /**
     * Captures raw user input into the inbox.
     * Automatically detects type prefixes (task:, note:, idea:).
     */
    public Capture capture(String rawInput) {
        InputParser.ParsedInput parsed = InputParser.parse(rawInput);

        Capture capture = Capture.builder(IdGenerator.generate(), parsed.content())
                .type(parsed.type())
                .build();

        captureRepository.save(capture);

        activityService.log(EntityType.CAPTURE, capture.getId(),
                ActionType.CREATED, "Captured: " + capture.getContentPreview());

        return capture;
    }

    /**
     * Returns all unprocessed inbox items (most recent first).
     */
    public List<Capture> getInbox() {
        return captureRepository.findUnprocessed();
    }

    /**
     * Returns the count of unprocessed inbox items.
     */
    public int getInboxCount() {
        return captureRepository.countUnprocessed();
    }

    /**
     * Resolves a capture by ID, ID prefix, or content fragment.
     */
    public Capture resolve(String idOrFragment) {
        var byId = captureRepository.findById(idOrFragment);
        if (byId.isPresent()) return byId.get();

        var byPrefix = captureRepository.findByIdPrefix(idOrFragment);
        if (byPrefix.size() == 1) return byPrefix.getFirst();
        if (byPrefix.size() > 1) {
            List<String> candidates = byPrefix.stream()
                    .map(c -> String.format("%s - %s", c.getId().substring(0, 8), c.getContentPreview()))
                    .toList();
            throw new AmbiguousEntityException("Capture", idOrFragment, candidates);
        }

        var matches = captureRepository.searchByText(idOrFragment);
        if (matches.isEmpty()) {
            throw new EntityNotFoundException("Capture", idOrFragment);
        }
        if (matches.size() > 1) {
            List<String> candidates = matches.stream()
                    .map(c -> String.format("%s - %s", c.getId().substring(0, 8), c.getContentPreview()))
                    .toList();
            throw new AmbiguousEntityException("Capture", idOrFragment, candidates);
        }
        return matches.getFirst();
    }

    public Capture getById(String id) {
        return captureRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Capture", id));
    }

    /**
     * Converts a capture into a Task.
     */
    public Task convertToTask(String idOrFragment, String titleOverride, String projectId,
                              TaskPriority priority, LocalDate dueDate) {
        if (taskService == null) {
            throw new IllegalStateException("TaskService is not configured for CaptureService");
        }
        Capture capture = resolve(idOrFragment);
        String taskTitle = (titleOverride != null && !titleOverride.isBlank())
                ? titleOverride
                : capture.getContent();

        Task task = taskService.create(taskTitle, projectId, priority, dueDate);

        capture.setProcessed(true);
        captureRepository.update(capture);

        activityService.log(EntityType.CAPTURE, capture.getId(), ActionType.UPDATED,
                "Converted capture to task: " + task.getTitle());

        return task;
    }

    /**
     * Converts a capture into a Note.
     */
    public Note convertToNote(String idOrFragment, String titleOverride, String contentOverride, String projectId) {
        if (noteService == null) {
            throw new IllegalStateException("NoteService is not configured for CaptureService");
        }
        Capture capture = resolve(idOrFragment);
        String content = (contentOverride != null && !contentOverride.isBlank())
                ? contentOverride
                : capture.getContent();

        String noteTitle;
        if (titleOverride != null && !titleOverride.isBlank()) {
            noteTitle = titleOverride;
        } else {
            String firstLine = content.split("\n")[0].trim();
            noteTitle = firstLine.length() > 60 ? firstLine.substring(0, 57) + "..." : firstLine;
        }

        Note note = noteService.create(noteTitle, content, projectId);

        capture.setProcessed(true);
        captureRepository.update(capture);

        activityService.log(EntityType.CAPTURE, capture.getId(), ActionType.UPDATED,
                "Converted capture to note: " + note.getTitle());

        return note;
    }

    /**
     * Converts a capture into an Idea note.
     */
    public Note convertToIdea(String idOrFragment, String projectId) {
        Capture capture = resolve(idOrFragment);
        String title = "[Idea] " + capture.getContentPreview();
        return convertToNote(capture.getId(), title, capture.getContent(), projectId);
    }

    /**
     * Marks a capture as processed.
     */
    public void markProcessed(String idOrFragment) {
        Capture capture = resolve(idOrFragment);
        capture.setProcessed(true);
        captureRepository.update(capture);

        activityService.log(EntityType.CAPTURE, capture.getId(), ActionType.UPDATED,
                "Marked capture as processed: " + capture.getContentPreview());
    }

    /**
     * Deletes a capture.
     */
    public void delete(String idOrFragment) {
        Capture capture = resolve(idOrFragment);
        captureRepository.delete(capture.getId());

        activityService.log(EntityType.CAPTURE, capture.getId(), ActionType.DELETED,
                "Deleted capture: " + capture.getContentPreview());
    }
}
