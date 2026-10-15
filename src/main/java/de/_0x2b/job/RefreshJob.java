package de._0x2b.job;

import java.util.concurrent.TimeUnit;

import de._0x2b.service.FeedService;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class RefreshJob {

    @Inject
    BackgroundJobService backgroundJobService;
    @Inject
    FeedService feedService;

    @Scheduled(delay = 30, delayUnit = TimeUnit.SECONDS, every = "${refresh.job.interval:1h}", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void refreshFeeds() {
        // Shares the single-flight lock with POST /api/feed/refresh.
        backgroundJobService.startRefresh(feedService::refresh);
    }
}
