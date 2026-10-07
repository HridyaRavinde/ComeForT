package com.akshit.comefort.service;

import com.akshit.comefort.core.Note;
import com.akshit.comefort.db.DatabaseManager;
import com.akshit.comefort.exception.EntityNotFoundException;
import com.akshit.comefort.repository.ActivityRepository;
import com.akshit.comefort.repository.NoteRepository;
import com.akshit.comefort.repository.ProjectRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("NoteService Integration Tests")
class NoteServiceTest {

    private DatabaseManager dbManager;
    private NoteService noteService;

    private ProjectService projectService;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        dbManager = new DatabaseManager(tempDir);
        dbManager.initialize();

        NoteRepository noteRepository = new NoteRepository(dbManager);
        ProjectRepository projectRepository = new ProjectRepository(dbManager);
        ActivityRepository activityRepository = new ActivityRepository(dbManager);
        ActivityService activityService = new ActivityService(activityRepository);
        noteService = new NoteService(noteRepository, activityService);
        projectService = new ProjectService(projectRepository, activityService);
    }

    @AfterEach
    void tearDown() {
        if (dbManager != null) {
            dbManager.close();
        }
    }

    @Test
    @DisplayName("Create note and retrieve by ID")
    void testCreateAndGet() {
        Note note = noteService.create("SQLite concurrency", "WAL mode enables concurrent reads", null);
        assertNotNull(note.getId());
        assertEquals("SQLite concurrency", note.getTitle());

        Note fetched = noteService.getById(note.getId());
        assertEquals(note.getContent(), fetched.getContent());
    }

    @Test
    @DisplayName("Update note title and content")
    void testUpdateNote() {
        Note note = noteService.create("Original Title", "Original Content", null);
        Note updated = noteService.update(note.getId(), "Updated Title", "Updated Content", null);

        assertEquals("Updated Title", updated.getTitle());
        assertEquals("Updated Content", updated.getContent());
    }

    @Test
    @DisplayName("Resolve note by title fragment")
    void testResolveNote() {
        noteService.create("Gradle wrapper details", "version 9.6.0", null);

        Note resolved = noteService.resolve("wrapper");
        assertEquals("Gradle wrapper details", resolved.getTitle());
    }

    @Test
    @DisplayName("Filter notes by project ID")
    void testListByProject() {
        var project = projectService.create("DemoProj", "desc", null);
        Note n1 = noteService.create("Project note", "content", project.getId());
        Note n2 = noteService.create("General note", "content", null);

        List<Note> projNotes = noteService.listByProject(project.getId());
        assertEquals(1, projNotes.size());
        assertEquals(n1.getId(), projNotes.get(0).getId());
    }
}
