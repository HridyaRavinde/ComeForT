package com.akshit.comefort.service;

import com.akshit.comefort.core.Capture;
import com.akshit.comefort.core.Note;
import com.akshit.comefort.core.Task;
import com.akshit.comefort.core.enums.CaptureType;
import com.akshit.comefort.core.enums.TaskPriority;
import com.akshit.comefort.db.DatabaseManager;
import com.akshit.comefort.exception.AmbiguousEntityException;
import com.akshit.comefort.repository.ActivityRepository;
import com.akshit.comefort.repository.CaptureRepository;
import com.akshit.comefort.repository.NoteRepository;
import com.akshit.comefort.repository.TaskRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CaptureService Integration Tests")
class CaptureServiceTest {

    private DatabaseManager dbManager;
    private CaptureService captureService;
    private TaskService taskService;
    private NoteService noteService;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        dbManager = new DatabaseManager(tempDir);
        dbManager.initialize();

        CaptureRepository captureRepository = new CaptureRepository(dbManager);
        TaskRepository taskRepository = new TaskRepository(dbManager);
        NoteRepository noteRepository = new NoteRepository(dbManager);
        ActivityRepository activityRepository = new ActivityRepository(dbManager);

        ActivityService activityService = new ActivityService(activityRepository);
        taskService = new TaskService(taskRepository, activityService);
        noteService = new NoteService(noteRepository, activityService);

        captureService = new CaptureService(captureRepository, activityService, taskService, noteService);
    }

    @AfterEach
    void tearDown() {
        if (dbManager != null) {
            dbManager.close();
        }
    }

    @Test
    @DisplayName("Quick capture creates unprocessed inbox item")
    void testCapture() {
        Capture capture = captureService.capture("idea: offline first life OS");
        assertNotNull(capture.getId());
        assertEquals(CaptureType.IDEA, capture.getType());
        assertEquals("offline first life OS", capture.getContent());
        assertFalse(capture.isProcessed());

        List<Capture> inbox = captureService.getInbox();
        assertEquals(1, inbox.size());
        assertEquals(1, captureService.getInboxCount());
    }

    @Test
    @DisplayName("Marking capture as processed removes from inbox")
    void testMarkProcessed() {
        Capture capture = captureService.capture("task: review PR #42");
        assertEquals(1, captureService.getInboxCount());

        captureService.markProcessed(capture.getId());
        assertEquals(0, captureService.getInboxCount());
        assertTrue(captureService.getInbox().isEmpty());
    }

    @Test
    @DisplayName("Delete capture removes from database")
    void testDeleteCapture() {
        Capture capture = captureService.capture("random scratch note");
        assertEquals(1, captureService.getInboxCount());

        captureService.delete(capture.getId());
        assertEquals(0, captureService.getInboxCount());
    }

    @Test
    @DisplayName("Convert capture to Task creates task and marks capture processed")
    void testConvertToTask() {
        Capture capture = captureService.capture("fix HoneyChain QR scanner");
        assertEquals(1, captureService.getInboxCount());

        Task task = captureService.convertToTask(capture.getId(), "Fix QR Scanner", null,
                TaskPriority.HIGH, LocalDate.now().plusDays(1));

        assertNotNull(task.getId());
        assertEquals("Fix QR Scanner", task.getTitle());
        assertEquals(TaskPriority.HIGH, task.getPriority());

        // Capture is now marked processed
        assertEquals(0, captureService.getInboxCount());
        assertTrue(captureService.getById(capture.getId()).isProcessed());
    }

    @Test
    @DisplayName("Convert capture to Note creates note and marks capture processed")
    void testConvertToNote() {
        Capture capture = captureService.capture("API design guidelines for auth module");
        assertEquals(1, captureService.getInboxCount());

        Note note = captureService.convertToNote(capture.getId(), "Auth Guidelines", null, null);
        assertNotNull(note.getId());
        assertEquals("Auth Guidelines", note.getTitle());
        assertEquals("API design guidelines for auth module", note.getContent());

        assertEquals(0, captureService.getInboxCount());
        assertTrue(captureService.getById(capture.getId()).isProcessed());
    }

    @Test
    @DisplayName("Convert capture to Idea creates idea note")
    void testConvertToIdea() {
        Capture capture = captureService.capture("idea: decentralized identity on local keychain");
        assertEquals(1, captureService.getInboxCount());

        Note note = captureService.convertToIdea(capture.getId(), null);
        assertTrue(note.getTitle().startsWith("[Idea]"));
        assertEquals(0, captureService.getInboxCount());
    }

    @Test
    @DisplayName("Ambiguous capture fragment match throws AmbiguousEntityException")
    void testAmbiguousCapture() {
        captureService.capture("honey chain audit");
        captureService.capture("honey chain deploy");

        assertThrows(AmbiguousEntityException.class, () -> captureService.resolve("honey"));
    }
}
