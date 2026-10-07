package com.akshit.comefort.service;

import com.akshit.comefort.core.ActivityEntry;
import com.akshit.comefort.core.enums.ActionType;
import com.akshit.comefort.core.enums.EntityType;
import com.akshit.comefort.repository.ActivityRepository;
import com.akshit.comefort.util.IdGenerator;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for recording and querying activity history.
 * Every significant action (create, update, complete, delete) gets logged
 * so the user can trace "what was I doing?" later.
 */
public class ActivityService {

    private final ActivityRepository activityRepository;

    public ActivityService(ActivityRepository activityRepository) {
        this.activityRepository = activityRepository;
    }

    /**
     * Logs an activity entry.
     *
     * @param entityType what kind of entity was affected
     * @param entityId   the entity's ID
     * @param action     what happened
     * @param summary    human-readable description
     */
    public void log(EntityType entityType, String entityId,
                    ActionType action, String summary) {
        ActivityEntry entry = new ActivityEntry(
                IdGenerator.generate(),
                entityType,
                entityId,
                action,
                summary,
                LocalDateTime.now()
        );
        activityRepository.save(entry);
    }

    /**
     * Returns the N most recent activity entries globally.
     */
    public List<ActivityEntry> getRecentActivity(int limit) {
        return activityRepository.findRecent(limit);
    }

    /**
     * Returns activity entries for a specific entity.
     */
    public List<ActivityEntry> getEntityActivity(EntityType entityType, String entityId) {
        return activityRepository.findByEntity(entityType, entityId);
    }

    /**
     * Returns all activity related to a project (including its tasks, notes, captures).
     */
    public List<ActivityEntry> getProjectActivity(String projectId, int limit) {
        return activityRepository.findByProjectActivity(projectId, limit);
    }
}
