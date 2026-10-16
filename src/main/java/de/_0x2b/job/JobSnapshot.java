package de._0x2b.job;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Immutable JSON view of a background job for REST clients.
 */
public record JobSnapshot(
        UUID id,
        JobType type,
        JobStatus status,
        int total,
        int completed,
        int failed,
        String error,
        Instant startedAt,
        Instant finishedAt,
        List<ImportRowOutcome> results) {

    public JobSnapshot {
        results = results == null ? List.of() : List.copyOf(results);
    }
}
