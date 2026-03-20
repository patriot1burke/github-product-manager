package io.quarkiverse.github.index;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkiverse.github.api.Discussions.Discussion;
import io.quarkiverse.github.api.Github;
import io.quarkiverse.github.api.GithubAPI.Repository;
import io.quarkiverse.github.api.Issues.Issue;
import io.quarkiverse.github.api.Labels.Label;
import io.quarkiverse.github.api.Labels.LabelNameOnly;
import io.quarkiverse.github.index.model.ChangeSet;
import io.quarkiverse.github.index.model.CommentModel;
import io.quarkiverse.github.index.model.DiscussionCommentModel;
import io.quarkiverse.github.index.model.DiscussionModel;
import io.quarkiverse.github.index.model.Earlier;
import io.quarkiverse.github.index.model.IssueModel;
import io.quarkiverse.github.index.model.RecordSet;
import io.quarkiverse.github.util.AppLogger;

@ApplicationScoped
public class PullCacheService {
    static AppLogger log = AppLogger.getLogger(PullCacheService.class);

    @ConfigProperty(name = "product.manager.cache.dir")
    String baseDirectory;

    @Inject
    Github github;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    RepositoryConfigService configService;

    @Inject
    PromptWrapper promptWrapper;

    @Inject
    PruneService pruneService;

    Map<String, PullCache> pullCacheMap = new ConcurrentHashMap<>();

    @PreDestroy
    public void preDestroy() {
        for (String repoName : pullCacheMap.keySet()) {
            save(repoName);
        }
    }

    public Set<String> repos() {
        Set<String> repos = new HashSet<>(pullCacheMap.keySet());
        Path cacheDir = Path.of(baseDirectory);
        if (!Files.exists(cacheDir) || !Files.isDirectory(cacheDir)) {
            return repos;
        }
        try {
            Files.walk(cacheDir)
                    .filter(p -> Files.isRegularFile(p) && "pull.json".equals(p.getFileName().toString()))
                    .forEach(pullJsonPath -> {
                        Path relative = cacheDir.relativize(pullJsonPath);
                        Path parent = relative.getParent();
                        if (parent != null && parent.getNameCount() > 0) {
                            String repoName = parent.toString().replace(java.io.File.separatorChar, '/');
                            repos.add(repoName);
                        }
                    });
        } catch (IOException e) {
            log.warnv("Failed to walk cache dir {0}: {1}", cacheDir, e.getMessage());
        }
        return repos;
    }

    public PullCache load(String repoName) {
        PullCache pullCache = pullCacheMap.get(repoName);
        if (pullCache != null) {
            return pullCache;
        }
        Path indexPath = pullIndexPath(repoName);
        if (pullIndexExists(repoName)) {
            try {
                pullCache = objectMapper.readValue(indexPath.toFile(), PullCache.class);
            } catch (IOException e) {
                log.warnv("Failed to load pull index for repo {0}: {1}", repoName, e.getMessage());
                try {
                    Files.delete(indexPath);
                } catch (IOException e2) {
                }
            }
        }
        if (pullCache == null) {
            pullCache = new PullCache(repoName);
        }
        pullCacheMap.put(repoName, pullCache);
        return pullCache;
    }

    private void save(String repoName) {
        PullCache pullCache = pullCacheMap.get(repoName);
        if (pullCache == null) {
            return;
        }
        if (!pullCache.dirty) {
            return;
        }
        pullCache.dirty = false;
        try {
            objectMapper.writeValue(pullIndexPath(repoName).toFile(), pullCache);
        } catch (IOException e) {
            log.errorv("Failed to save pull index for repo {0}: {1}", repoName, e.getMessage());
            try {
                Files.delete(pullIndexPath(repoName));
            } catch (IOException e2) {
            }
        }
    }

    public void clear(String repoName) {
        pullCacheMap.remove(repoName);
        Path path = Path.of(baseDirectory, repoName);
        Path indexPath = path.resolve("pull.json");
        try {
            if (Files.exists(indexPath)) {
                Files.delete(indexPath);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to clear pull index", e);
        }
    }

    public Map<String, Label> getLabels(Repository repository, RepositoryConfig repoIndex) {
        return repository.labels().entrySet().stream()
                .filter(entry -> !repoIndex.ignoreLabel(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public Map<String, Label> getLabels(String repoName) {
        Repository repository = github.repository(repoName);
        RepositoryConfig repoIndex = configService.load(repoName);
        return getLabels(repository, repoIndex);
    }

    private Path pullIndexPath(String repoName) {
        Path path = Path.of(baseDirectory, repoName);
        return path.resolve("pull.json");
    }

    public ChangeSet pull(String repoName, Earlier range) {
        try {
            Repository repository = github.repository(repoName);
            RepositoryConfig config = configService.load(repoName);
            Map<String, Label> labels = getLabels(repository, config);
            PullCache pullCache = load(repoName);

            long since;

            if (pullCache.lastPulled == 0) {
                since = range == null ? Earlier.month.fromMillis() : range.fromMillis();
            } else if (range == null) {
                // get all from last pull
                since = pullCache.lastPulled;
            } else {
                since = pullCache.lastPulled > range.fromMillis() ? pullCache.lastPulled : range.fromMillis();
            }
            pullCache.lastPulled = System.currentTimeMillis();
            log.thinking("Pulling since: " + Instant.ofEpochMilli(since).toString());
            log.thinking("Pulling discussions...");
            Set<Integer> discussionChanges = new HashSet<>();
            Iterable<Discussion> discussions = github.repository(repoName).discussions().full(20);
            for (Discussion discussion : discussions) {
                if (config.ignoredCategories.contains(discussion.category().name())) {
                    continue;
                }
                String updatedAt = discussion.updatedAt();
                Instant instant = Instant.parse(updatedAt);
                // log.thinking(instant.toString());
                if (instant.toEpochMilli() < since) {
                    break;
                }
                Set<String> discussionLabels = discussion.labels().nodes().stream().map(LabelNameOnly::name)
                        .collect(Collectors.toSet());
                if (discussionLabels.isEmpty()) {
                    log.thinking("Labeling unlabelled discussion: " + discussion.title());
                    discussionLabels = promptWrapper.labelDiscussion(labels.values(), discussion);
                }
                List<DiscussionCommentModel> comments = discussion.comments().nodes().stream()
                        .map(comment -> new DiscussionCommentModel(
                                comment.author() == null ? "unknown" : comment.author().login(), comment.body(),
                                comment.replies().nodes().stream()
                                        .map(reply -> new CommentModel(
                                                reply.author() == null ? "unknown" : reply.author().login(), reply.body()))
                                        .collect(Collectors.toList())))
                        .collect(Collectors.toList());
                DiscussionModel discussionModel = new DiscussionModel(repoName, discussion.number(), discussion.title(),
                        discussion.author() == null ? "unknown" : discussion.author().login(), discussion.body(),
                        Instant.parse(discussion.createdAt()).toEpochMilli(),
                        Instant.parse(discussion.updatedAt()).toEpochMilli(), discussion.category().name(),
                        discussionLabels,
                        comments);
                discussionChanges.add(discussion.number());
                pullCache.discussions.put(discussion.number(), discussionModel);
            }
            if (discussionChanges.size() > 0) {
                pullCache.dirty = true;
            }

            log.thinking("Pulling issues...");
            Iterable<Issue> issues = github.repository(repoName).issues().full(20,
                    Instant.ofEpochMilli(since).toString());
            Set<Integer> issueChanges = new HashSet<>();
            for (Issue issue : issues) {
                Set<String> issueLabels = issue.labels().nodes().stream().map(LabelNameOnly::name)
                        .collect(Collectors.toSet());
                if (issueLabels.isEmpty()) {
                    log.thinking("Labeling unlabelled issue: " + issue.title());
                    issueLabels = promptWrapper.labelIssue(labels.values(), issue);
                }
                List<CommentModel> comments = issue.comments().nodes().stream()
                        .map(comment -> new CommentModel(comment.author() == null ? "unknown" : comment.author().login(),
                                comment.body()))
                        .collect(Collectors.toList());
                IssueModel issueModel = new IssueModel(repoName, issue.number(), issue.title(),
                        issue.author() == null ? "unknown" : issue.author().login(),
                        issue.body(),
                        Instant.parse(issue.createdAt()).toEpochMilli(),
                        Instant.parse(issue.updatedAt()).toEpochMilli(),
                        issue.issueType() == null ? null : issue.issueType().name(), issueLabels, comments);
                issueChanges.add(issue.number());
                pullCache.issues.put(issue.number(), issueModel);
            }
            if (issueChanges.size() > 0) {
                pullCache.dirty = true;
            }
            ChangeSet changeSet = new ChangeSet(discussionChanges, issueChanges);
            pruneService.newPull(repoName, changeSet);
            return changeSet;
        } catch (RuntimeException e) {
            pullCacheMap.remove(repoName);
            throw e;
        }
    }

    public boolean pullIndexExists(String repoName) {
        Path indexPath = pullIndexPath(repoName);
        return Files.exists(indexPath);
    }

    public ChangeSet prune(String repoName, Earlier since) {
        PullCache pullCache = load(repoName);
        Set<Integer> discussions = new HashSet<>();
        Set<Integer> issues = new HashSet<>();
        long sinceMillis = since.fromMillis();
        for (DiscussionModel discussion : pullCache.discussions.values()) {
            if (discussion.updatedAt() < sinceMillis) {
                discussions.add(discussion.number());
            }
        }
        for (IssueModel issue : pullCache.issues.values()) {
            if (issue.updatedAt() < sinceMillis) {
                issues.add(issue.number());
            }
        }
        if (discussions.size() > 0 || issues.size() > 0) {
            pullCache.dirty = true;
        }
        for (Integer discussion : discussions) {
            pullCache.discussions.remove(discussion);
        }
        for (Integer issue : issues) {
            pullCache.issues.remove(issue);
        }
        return new ChangeSet(discussions, issues);
    }

    public RecordSet findByLabel(String repoName, Set<String> labels) {
        PullCache pullCache = load(repoName);
        List<DiscussionModel> discussions = new ArrayList<>();
        List<IssueModel> issues = new ArrayList<>();
        for (DiscussionModel discussion : pullCache.discussions.values()) {
            if (discussion.labels().containsAll(labels)) {
                discussions.add(discussion);
            }
        }
        for (IssueModel issue : pullCache.issues.values()) {
            if (issue.labels().containsAll(labels)) {
                issues.add(issue);
            }
        }
        return new RecordSet(discussions, issues);
    }
}
