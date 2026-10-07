package com.akshit.comefort.service;

import com.akshit.comefort.core.Capture;
import com.akshit.comefort.core.enums.CaptureType;
import com.akshit.comefort.db.DatabaseManager;
import com.akshit.comefort.repository.ActivityRepository;
import com.akshit.comefort.repository.CaptureRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CaptureService Integration Tests")
class CaptureServiceTest {

    private DatabaseManager dbManager;
    private CaptureService captureService;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        dbManager = new DatabaseManager(tempDir);
        dbManager.initialize();

        CaptureRepository captureRepository = new CaptureRepository(dbManager);
        ActivityRepository activityRepository = new ActivityRepository(dbManager);
        ActivityService activityService = new ActivityService(activityRepository);
        captureService = new CaptureService(captureRepository, activityService);
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
}
