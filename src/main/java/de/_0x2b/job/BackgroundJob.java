package de._0x2b.job;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

final class BackgroundJob implements ProgressCallback {

    private final UUID id = UUID.randomUUID();
    private final JobType type;
    private final Instant startedAt = Instant.now();

    private volatile JobStatus status = JobStatus.RUNNING;
    private volatile int total;
    private final AtomicInteger completed = new AtomicInteger();
    private final AtomicInteger failed = new AtomicInteger();
    private volatile String error;
    private volatile Instant finishedAt;

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
        return new JobSnapshot(
                id,
                type,
                status,
                total,
                completed.get(),
                failed.get(),
                error,
                startedAt,
                finishedAt);
    }
}
