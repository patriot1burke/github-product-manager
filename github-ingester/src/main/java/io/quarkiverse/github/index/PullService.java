package io.quarkiverse.github.index;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkiverse.github.index.model.ChangeSet;
import io.quarkiverse.github.index.model.Earlier;

@ApplicationScoped
public class PullService {
    @Inject
    PullCacheService pullCacheService;

    @Inject
    SummaryService summaryService;

    @Inject
    RagIndexer ragIndexer;

    @Inject
    RenderService renderService;

    public void pull(String repoName, Earlier dateRange) {
        ChangeSet changeSet = pullCacheService.pull(repoName, dateRange);   
        summaryService.prune(repoName, changeSet);
        ragIndexer.index(repoName, changeSet);
    }

 }
