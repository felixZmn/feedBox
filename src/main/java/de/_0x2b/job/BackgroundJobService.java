package de._0x2b.job;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * In-memory registry for long-running refresh/import jobs.
 * Refresh is single-flight (shared by HTTP and {@link RefreshJob}).
 */
@ApplicationScoped
public class BackgroundJobService {

    private static final Logger logger = LoggerFactory.getLogger(BackgroundJobService.class);
    private static final Duration RETENTION = Duration.ofHours(1);
    private static final int MAX_JOBS = 32;

    private final Map<UUID, BackgroundJob> jobs = new ConcurrentHashMap<>();
    private final AtomicReference<UUID> activeRefreshId = new AtomicReference<>();
    private final AtomicReference<UUID> activeImportId = new AtomicReference<>();

    public Optional<JobSnapshot> find(UUID id) {
        prune();
        BackgroundJob job = jobs.get(id);
        return job == null ? Optional.empty() : Optional.of(job.snapshot());
    }

    /**
     * Start a refresh, or return the already-running refresh job (idempotent).
     */
    public JobSnapshot startRefresh(Consumer<ProgressCallback> work) {
        prune();
        UUID existingId = activeRefreshId.get();
        if (existingId != null) {
            BackgroundJob existing = jobs.get(existingId);
            if (existing != null && !existing.isTerminal()) {
                return existing.snapshot();
            }
            activeRefreshId.compareAndSet(existingId, null);
        }

        BackgroundJob job = new BackgroundJob(JobType.REFRESH);
        if (!activeRefreshId.compareAndSet(null, job.id())) {
            BackgroundJob raced = jobs.get(activeRefreshId.get());
            if (raced != null) {
                return raced.snapshot();
            }
        }

        jobs.put(job.id(), job);
        Thread.startVirtualThread(() -> run(job, activeRefreshId, work));
        return job.snapshot();
    }

    /**
     * Start an OPML import, or return the already-running import job.
     */
    public JobSnapshot startImport(Consumer<ProgressCallback> work) {
        prune();
        UUID existingId = activeImportId.get();
        if (existingId != null) {
            BackgroundJob existing = jobs.get(existingId);
            if (existing != null && !existing.isTerminal()) {
                return existing.snapshot();
            }
            activeImportId.compareAndSet(existingId, null);
        }

        BackgroundJob job = new BackgroundJob(JobType.IMPORT);
        if (!activeImportId.compareAndSet(null, job.id())) {
            BackgroundJob raced = jobs.get(activeImportId.get());
            if (raced != null) {
                return raced.snapshot();
            }
        }

        jobs.put(job.id(), job);
        Thread.startVirtualThread(() -> run(job, activeImportId, work));
        return job.snapshot();
    }

    private void run(BackgroundJob job, AtomicReference<UUID> activeSlot, Consumer<ProgressCallback> work) {
        try {
            work.accept(job);
            job.complete();
        } catch (Throwable t) {
            // Catch Error too so the job cannot stick in RUNNING forever.
            logger.error("{} job {} failed", job.type(), job.id(), t);
            job.fail(sanitizeJobError(t));
        } finally {
            activeSlot.compareAndSet(job.id(), null);
        }
    }

    /**
     * Client-visible job errors must not include SQL, stack frames, or other
     * internals — those stay in the log.
     */
    static String sanitizeJobError(Throwable t) {
        if (t == null) {
            return "Job failed";
        }
        Throwable root = t;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String name = root.getClass().getSimpleName();
        if (name == null || name.isBlank()) {
            return "Job failed";
        }
        return "Job failed (" + name + ")";
    }

    private void prune() {
        Instant cutoff = Instant.now().minus(RETENTION);
        jobs.entrySet().removeIf(entry -> {
            BackgroundJob job = entry.getValue();
            return job.isTerminal()
                    && job.snapshot().finishedAt() != null
                    && job.snapshot().finishedAt().isBefore(cutoff);
        });
        while (jobs.size() > MAX_JOBS) {
            UUID oldestTerminal = jobs.entrySet().stream()
                    .filter(e -> e.getValue().isTerminal())
                    .min((a, b) -> {
                        Instant fa = a.getValue().snapshot().finishedAt();
                        Instant fb = b.getValue().snapshot().finishedAt();
                        if (fa == null) return -1;
                        if (fb == null) return 1;
                        return fa.compareTo(fb);
                    })
                    .map(Map.Entry::getKey)
                    .orElse(null);
            if (oldestTerminal == null) {
                break;
            }
            jobs.remove(oldestTerminal);
        }
    }
}
