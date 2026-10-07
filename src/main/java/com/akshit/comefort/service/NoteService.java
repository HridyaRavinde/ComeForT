package com.akshit.comefort.service;

import com.akshit.comefort.core.Note;
import com.akshit.comefort.core.enums.ActionType;
import com.akshit.comefort.core.enums.EntityType;
import com.akshit.comefort.exception.AmbiguousEntityException;
import com.akshit.comefort.exception.EntityNotFoundException;
import com.akshit.comefort.repository.NoteRepository;
import com.akshit.comefort.util.IdGenerator;

import java.util.List;

/**
 * Business logic for managing notes.
 */
public class NoteService {

    private final NoteRepository noteRepository;
    private final ActivityService activityService;

    public NoteService(NoteRepository noteRepository, ActivityService activityService) {
        this.noteRepository = noteRepository;
        this.activityService = activityService;
    }

    /**
     * Creates a new note.
     */
    public Note create(String title, String content, String projectId) {
        Note note = Note.builder(IdGenerator.generate(), title)
                .content(content != null ? content : "")
                .projectId(projectId)
                .build();

        noteRepository.save(note);

        activityService.log(EntityType.NOTE, note.getId(),
                ActionType.CREATED, "Created note: " + title);

        return note;
    }

    /**
     * Returns all notes.
     */
    public List<Note> listAll() {
        return noteRepository.findAll();
    }

    /**
     * Returns notes for a specific project.
     */
    public List<Note> listByProject(String projectId) {
        return noteRepository.findByProjectId(projectId);
    }

    /**
     * Finds a note by ID.
     */
    public Note getById(String id) {
        return noteRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Note", id));
    }

    /**
     * Resolves a note by full ID or title fragment.
     */
    public Note resolve(String idOrFragment) {
        var byId = noteRepository.findById(idOrFragment);
        if (byId.isPresent()) return byId.get();

        var byPrefix = noteRepository.findByIdPrefix(idOrFragment);
        if (byPrefix.size() == 1) return byPrefix.getFirst();
        if (byPrefix.size() > 1) {
            List<String> candidates = byPrefix.stream()
                    .map(n -> String.format("%s - %s", n.getId().substring(0, 8), n.getTitle()))
                    .toList();
            throw new AmbiguousEntityException("Note", idOrFragment, candidates);
        }

        var byTitle = noteRepository.findByTitleFragment(idOrFragment);
        if (byTitle.isEmpty()) {
            throw new EntityNotFoundException("Note", idOrFragment);
        }
        if (byTitle.size() > 1) {
            List<String> candidates = byTitle.stream()
                    .map(n -> String.format("%s - %s", n.getId().substring(0, 8), n.getTitle()))
                    .toList();
            throw new AmbiguousEntityException("Note", idOrFragment, candidates);
        }
        return byTitle.getFirst();
    }

    /**
     * Updates a note.
     */
    public Note update(String id, String title, String content, String projectId) {
        Note note = getById(id);

        if (title != null && !title.isBlank()) note.setTitle(title);
        if (content != null) note.setContent(content);
        if (projectId != null) note.setProjectId(projectId);

        noteRepository.update(note);

        activityService.log(EntityType.NOTE, note.getId(),
                ActionType.UPDATED, "Updated note: " + note.getTitle());

        return note;
    }

    /**
     * Deletes a note.
     */
    public void delete(String idOrFragment) {
        Note note = resolve(idOrFragment);
        noteRepository.delete(note.getId());

        activityService.log(EntityType.NOTE, note.getId(),
                ActionType.DELETED, "Deleted note: " + note.getTitle());
    }

    /**
     * Returns total note count.
     */
    public int count() {
        return noteRepository.count();
    }
}
