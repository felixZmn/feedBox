package de._0x2b.job;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BackgroundJobServiceTest {

    BackgroundJobService sut;

    @BeforeEach
    void setUp() {
        sut = new BackgroundJobService();
    }

    @Test
    void startRefresh_isSingleFlight_returnsSameJobWhileRunning() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger runs = new AtomicInteger();

        JobSnapshot first = sut.startRefresh(progress -> {
            runs.incrementAndGet();
            started.countDown();
            try {
                assertTrue(release.await(5, TimeUnit.SECONDS));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        });

        assertTrue(started.await(5, TimeUnit.SECONDS));
        JobSnapshot second = sut.startRefresh(progress -> runs.incrementAndGet());

        assertEquals(first.id(), second.id());
        assertEquals(JobStatus.RUNNING, second.status());
        assertEquals(1, runs.get());

        release.countDown();
        awaitTerminal(first.id());
        assertEquals(1, runs.get());
    }

    @Test
    void startRefresh_afterCompletion_startsNewJob() throws Exception {
        JobSnapshot first = sut.startRefresh(progress -> progress.setTotal(0));
        awaitTerminal(first.id());

        JobSnapshot second = sut.startRefresh(progress -> progress.setTotal(1));
        assertNotEquals(first.id(), second.id());
        awaitTerminal(second.id());
    }

    @Test
    void startImport_reportsFailureStatus() throws Exception {
        JobSnapshot started = sut.startImport(progress -> {
            throw new IllegalStateException("boom");
        });
        JobSnapshot done = awaitTerminal(started.id());
        assertEquals(JobStatus.FAILED, done.status());
        assertEquals("Job failed (IllegalStateException)", done.error());
    }

    private JobSnapshot awaitTerminal(java.util.UUID id) throws InterruptedException {
        for (int i = 0; i < 100; i++) {
            JobSnapshot snap = sut.find(id).orElseThrow();
            if (snap.status().isTerminal()) {
                return snap;
            }
            Thread.sleep(20);
        }
        throw new AssertionError("job did not finish: " + id);
    }
}
