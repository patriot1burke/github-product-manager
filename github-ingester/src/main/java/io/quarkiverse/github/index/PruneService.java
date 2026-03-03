package io.quarkiverse.github.index;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkiverse.github.index.PullCacheService.ChangeSet;
import io.quarkiverse.github.index.ReportService.DateRange;

@ApplicationScoped
public class PruneService {
    @Inject
    PullCacheService pullCacheService;

    @Inject
    SummaryService summaryService;

    public void prune(String repoName, DateRange dateRange) {
        ChangeSet changeSet = pullCacheService.prune(repoName, dateRange);
        summaryService.prune(repoName, changeSet);
    }

    public void clear(String repoName) {
        pullCacheService.clear(repoName);
        summaryService.clear(repoName);
    }

}
