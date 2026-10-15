package de._0x2b.job;

import java.time.Instant;
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
        Instant finishedAt) {
}
