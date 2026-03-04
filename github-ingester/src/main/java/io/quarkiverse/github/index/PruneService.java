package io.quarkiverse.github.index;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkiverse.github.index.model.ChangeSet;
import io.quarkiverse.github.index.model.Earlier;

@ApplicationScoped
public class PruneService {
    @Inject
    PullCacheService pullCacheService;

    @Inject
    SummaryService summaryService;

    @Inject
    RagIndexer ragIndexer;

    public void prune(String repoName, Earlier dateRange) {
        ChangeSet changeSet = pullCacheService.prune(repoName, dateRange);
        summaryService.prune(repoName, changeSet);
        ragIndexer.prune(repoName, changeSet);
    }

    public void clear(String repoName) {
        pullCacheService.clear(repoName);
        summaryService.clear(repoName);
        ragIndexer.clear(repoName);
    }

}
