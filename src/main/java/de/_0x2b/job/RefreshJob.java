package de._0x2b.job;

import de._0x2b.service.FeedService;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class RefreshJob {
    @Inject
    FeedService feedService;

    @Scheduled(delay = 30, delayUnit = TimeUnit.SECONDS, every = "${refresh.job.interval:1h}", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void refreshFeeds() {
        feedService.refresh();
    }
}
