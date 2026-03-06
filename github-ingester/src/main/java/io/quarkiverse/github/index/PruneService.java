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

    @Inject
    RenderService renderService;

    public void prune(String repoName, Earlier dateRange) {
        ChangeSet changeSet = pullCacheService.prune(repoName, dateRange);
        summaryService.prune(repoName, changeSet);
        ragIndexer.prune(repoName, changeSet);
        renderService.prune(repoName, changeSet);
    }

    public void newPull(String repoName, ChangeSet changeSet) {
        summaryService.prune(repoName, changeSet);
        renderService.prune(repoName, changeSet);
        ragIndexer.newPull(repoName, changeSet);
    }

    public void clear(String repoName) {
        pullCacheService.clear(repoName);
        summaryService.clear(repoName);
        ragIndexer.clear(repoName);
        renderService.clear(repoName);
    }

}
