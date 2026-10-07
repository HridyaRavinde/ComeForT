package com.akshit.comefort.exception;

import java.util.List;

/**
 * Thrown when an identifier or title fragment matches multiple entities
 * instead of silently picking the first one.
 */
public class AmbiguousEntityException extends ComeFortException {

    private final String entityType;
    private final String query;
    private final List<String> candidates;

    public AmbiguousEntityException(String entityType, String query, List<String> candidates) {
        super(buildMessage(entityType, query, candidates));
        this.entityType = entityType;
        this.query = query;
        this.candidates = candidates;
    }

    private static String buildMessage(String entityType, String query, List<String> candidates) {
        StringBuilder sb = new StringBuilder();
        sb.append("Multiple ").append(entityType).append("s match '").append(query).append("':\n");
        for (int i = 0; i < candidates.size(); i++) {
            sb.append("   ").append(i + 1).append(". ").append(candidates.get(i)).append("\n");
        }
        sb.append("Please specify the exact ID or a more specific name.");
        return sb.toString();
    }

    public String getEntityType() {
        return entityType;
    }

    public String getQuery() {
        return query;
    }

    public List<String> getCandidates() {
        return candidates;
    }
}
