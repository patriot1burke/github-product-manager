package io.quarkiverse.github.index;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import dev.langchain4j.model.TokenCountEstimator;
import io.quarkiverse.github.index.model.ChangeSet;
import io.quarkiverse.github.index.model.DiscussionModel;
import io.quarkiverse.github.index.model.IssueModel;
import io.quarkiverse.github.index.model.RenderResult;
import io.quarkiverse.github.util.AppLogger;

@ApplicationScoped
public class SummaryService {
    static AppLogger log = AppLogger.getLogger(SummaryService.class);

    @Inject
    PromptWrapper promptWrapper;

    @Inject
    RenderService renderService;

    @Inject
    PullCacheService pullCacheService;

    @ConfigProperty(name = "product.manager.cache.dir")
    String baseDirectory;

    @Inject
    ObjectMapper objectMapper;

    static class SummaryCache {

        @JsonDeserialize(as = ConcurrentHashMap.class)
        public Map<Integer, RenderResult> discussions = new ConcurrentHashMap<>();

        @JsonDeserialize(as = ConcurrentHashMap.class)
        public Map<Integer, RenderResult> issues = new ConcurrentHashMap<>();

        @JsonIgnore
        boolean dirty = false;
    }

    private Map<String, SummaryCache> summaryCacheMap = new ConcurrentHashMap<>();

    @PreDestroy
    public void preDestroy() {
        for (String repoName : summaryCacheMap.keySet()) {
            save(repoName);
        }
    }

    public void clear(String repoName) {
        summaryCacheMap.remove(repoName);
        Path path = Path.of(baseDirectory, repoName, "summary.json");
        try {
            if (Files.exists(path)) {
                Files.delete(path);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to clear summary cache", e);
        }
    }

    private SummaryCache load(String repoName) {
        SummaryCache summaryCache = summaryCacheMap.get(repoName);
        if (summaryCache != null) {
            return summaryCache;
        }
        Path path = Path.of(baseDirectory, repoName, "summary.json");
        if (Files.exists(path)) {
            try {
                summaryCache = objectMapper.readValue(path.toFile(), SummaryCache.class);
            } catch (IOException e) {
                log.errorv("Failed to load summary cache for repo {0}: {1}", repoName, e.getMessage());
                try {
                    Files.delete(path);
                } catch (IOException e2) {
                }

            }
        }
        if (summaryCache == null) {
            summaryCache = new SummaryCache();
        }
        summaryCacheMap.put(repoName, summaryCache);
        return summaryCache;
    }

    private void save(String repoName) {
        SummaryCache summaryCache = summaryCacheMap.get(repoName);
        if (summaryCache == null) {
            return;
        }
        if (!summaryCache.dirty) {
            return;
        }
        summaryCache.dirty = false;
        Path path = Path.of(baseDirectory, repoName, "summary.json");
        try {
            objectMapper.writeValue(path.toFile(), summaryCache);
        } catch (IOException e) {
            log.error("Failed to save summary cache", e);
            try {
                Files.delete(path);
            } catch (IOException e2) {
            }
        }
    }

    @Inject
    TokenCountEstimator estimator;

    public RenderResult summarize(String repoName, DiscussionModel discussion) {
        SummaryCache summaryCache = load(repoName);
        RenderResult summaryEntry = summaryCache.discussions.get(discussion.number());
        if (summaryEntry != null) {
            return summaryEntry;
        }
        log.thinking("Summarizing discussion: " + discussion.title());

        String summary = promptWrapper.summarize(renderService.discussion(discussion).text());
        RenderResult renderResult = new RenderResult(summary, estimator.estimateTokenCountInText(summary));
        summaryCache.discussions.put(discussion.number(), renderResult);
        summaryCache.dirty = true;
        return renderResult;
    }

    public RenderResult summarize(String repoName, IssueModel issue) {
        SummaryCache summaryCache = load(repoName);
        RenderResult summaryEntry = summaryCache.issues.get(issue.number());
        if (summaryEntry != null) {
            return summaryEntry;
        }
        log.thinking("Summarizing issue: " + issue.title());
        String summary = promptWrapper.summarize(renderService.issue(issue).text());
        RenderResult renderResult = new RenderResult(summary, estimator.estimateTokenCountInText(summary));
        summaryCache.issues.put(issue.number(), renderResult);
        summaryCache.dirty = true;
        return renderResult;
    }

    public void prune(String repoName, ChangeSet changeSet) {
        SummaryCache summaryCache = load(repoName);
        for (Integer discussion : changeSet.discussions()) {
            var summary = summaryCache.discussions.remove(discussion);
            if (summary != null) {
                summaryCache.dirty = true;
            }
        }
        for (Integer issue : changeSet.issues()) {
            var summary = summaryCache.issues.remove(issue);
            if (summary != null) {
                summaryCache.dirty = true;
            }
        }
    }
}
