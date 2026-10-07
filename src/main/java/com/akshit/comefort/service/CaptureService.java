package com.akshit.comefort.service;

import com.akshit.comefort.core.Capture;
import com.akshit.comefort.core.enums.ActionType;
import com.akshit.comefort.core.enums.EntityType;
import com.akshit.comefort.repository.CaptureRepository;
import com.akshit.comefort.util.IdGenerator;
import com.akshit.comefort.util.InputParser;

import java.util.List;

/**
 * Business logic for quick captures — the heart of ComeFort.
 * Captures are raw thoughts dumped into the inbox for later processing.
 */
public class CaptureService {

    private final CaptureRepository captureRepository;
    private final ActivityService activityService;

    public CaptureService(CaptureRepository captureRepository, ActivityService activityService) {
        this.captureRepository = captureRepository;
        this.activityService = activityService;
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
     * Marks a capture as processed.
     */
    public void markProcessed(String id) {
        var capture = captureRepository.findById(id)
                .orElseThrow(() -> new com.akshit.comefort.exception.EntityNotFoundException("Capture", id));
        capture.setProcessed(true);
        captureRepository.update(capture);
    }

    /**
     * Deletes a capture.
     */
    public void delete(String id) {
        captureRepository.delete(id);
    }
}
