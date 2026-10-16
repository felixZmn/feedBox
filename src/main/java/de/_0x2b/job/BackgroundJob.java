package de._0x2b.job;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

final class BackgroundJob implements ProgressCallback {

    /** Cap client-visible per-row results to bound JSON size. */
    static final int MAX_RESULTS = 100;

    private final UUID id = UUID.randomUUID();
    private final JobType type;
    private final Instant startedAt = Instant.now();

    private volatile JobStatus status = JobStatus.RUNNING;
    private volatile int total;
    private final AtomicInteger completed = new AtomicInteger();
    private final AtomicInteger failed = new AtomicInteger();
    private volatile String error;
    private volatile Instant finishedAt;
    private final List<ImportRowOutcome> results = new ArrayList<>();

    BackgroundJob(JobType type) {
        this.type = type;
    }

    UUID id() {
        return id;
    }

    JobType type() {
        return type;
    }

    boolean isTerminal() {
        return status.isTerminal();
    }

    @Override
    public void setTotal(int total) {
        this.total = Math.max(0, total);
    }

    @Override
    public void itemSucceeded() {
        completed.incrementAndGet();
    }

    @Override
    public void itemFailed() {
        failed.incrementAndGet();
    }

    @Override
    public void recordOutcome(ImportRowOutcome outcome) {
        if (outcome == null) {
            return;
        }
        synchronized (results) {
            if (results.size() < MAX_RESULTS) {
                results.add(outcome);
            }
        }
    }

    void complete() {
        status = JobStatus.COMPLETED;
        finishedAt = Instant.now();
    }

    void fail(String message) {
        status = JobStatus.FAILED;
        error = message;
        finishedAt = Instant.now();
    }

    JobSnapshot snapshot() {
        List<ImportRowOutcome> copy;
        synchronized (results) {
            copy = List.copyOf(results);
        }
        return new JobSnapshot(
                id,
                type,
                status,
                total,
                completed.get(),
                failed.get(),
                error,
                startedAt,
                finishedAt,
                copy);
    }
}
