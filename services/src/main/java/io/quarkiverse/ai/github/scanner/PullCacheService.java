package io.quarkiverse.ai.github.scanner;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.*;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import io.quarkiverse.ai.github.api.Discussions.Discussion;
import io.quarkiverse.ai.github.api.Github;
import io.quarkiverse.ai.github.api.GithubAPI.Repository;
import io.quarkiverse.ai.github.api.Issues.Issue;
import io.quarkiverse.ai.github.api.Labels.Label;
import io.quarkiverse.ai.github.api.Labels.LabelNameOnly;
import io.quarkiverse.ai.github.db.EmbeddingsRepository;
import io.quarkiverse.ai.github.db.GithubEntry;
import io.quarkiverse.ai.github.db.GithubEntryRepository;
import io.quarkiverse.ai.github.scanner.model.*;
import io.quarkiverse.ai.github.util.AppLogger;

@ApplicationScoped
public class PullCacheService {
    static AppLogger log = AppLogger.getLogger(PullCacheService.class);

    @Inject
    Github github;

    @Inject
    DetermineLabelsPrompt determineLabels;

    @Inject
    RenderService renderService;

    @Inject
    EmbeddingModel embeddingModel;

    @Inject
    EmbeddingStore<TextSegment> embeddingsStore;

    @Inject
    GithubEntryRepository githubdb;

    @Inject
    EmbeddingsRepository embeddingsDb;

    @Inject
    UserTransaction transaction;

    private static final int MAX_PRUNE_BATCH_SIZE = 20;

    long lastPulled(String repoName) {
        return embeddingsDb.lastPulled(repoName);
    }

    public void pull(String repoName, TimePeriod range) {
        try {
            Repository repository = github.repository(repoName);
            Map<String, Label> labels = repository.labels();

            long since;
            long lastPulled = lastPulled(repoName);
            if (lastPulled == 0) {
                since = range == null ? TimePeriod.month.fromMillis() : range.fromMillis();
            } else if (range == null) {
                // get all from last pull
                since = lastPulled;
            } else {
                since = lastPulled > range.fromMillis() ? lastPulled : range.fromMillis();
            }
            EmbeddingStoreIngestor ingester = EmbeddingStoreIngestor.builder()
                    .embeddingModel(embeddingModel)
                    .embeddingStore(embeddingsStore)
                    .build();
            log.thinking("Pulling since: " + Instant.ofEpochMilli(since).toString());
            log.thinking("Pulling discussions from github...");
            Iterable<Discussion> discussions = repository.discussions().full(20);
            List<Document> docs = new ArrayList<>();
            List<GithubEntry> entries = new ArrayList<>();
            for (Discussion discussion : discussions) {
                DiscussionModel discussionModel = discussionToDiscussionModel(repoName, discussion, since, labels);
                if (discussionModel == null) {
                    break;
                }
                embeddingsDb.prune(discussionModel);
                GithubEntry entry = persist(discussionModel);
                entries.add(entry);
                log.thinking("\tcreating document");
                createDoc(docs, discussionModel, entry.metadata);
            }
            int numDiscussions = entries.size();

            log.thinking("\n\nPulling issues...");
            Iterable<Issue> issues = github.repository(repoName).issues().full(20,
                    Instant.ofEpochMilli(since).toString());
            for (Issue issue : issues) {
                IssueModel issueModel = issueToIssueModel(repoName, issue, labels);
                if (issueModel == null) {
                    continue;
                }
                embeddingsDb.prune(issueModel);
                GithubEntry entry = persist(issueModel);
                entries.add(entry);
                log.thinking("\tcreating document");
                createDoc(docs, issueModel, entry.metadata);
            }
            int numIssues = entries.size() - numDiscussions;
            log.thinking("\nPulled " + numDiscussions + " discussions and " + numIssues + " issues");
            if (docs.size() > 0) {
                log.thinking("\n\nIngesting " + docs.size() + " items");

                for (List<Document> batch : partition(docs, 20)) {
                    log.thinking("\tingesting batch of " + batch.size() + " items");
                    ingester.ingest(batch);
                }
            }
        } catch (RuntimeException e) {
            throw e;
        }
    }

    private List<List<Document>> partition(List<Document> inputList, int size) {
        List<List<Document>> result = new ArrayList<>();
        for (int i = 0; i < inputList.size(); i += size) {
            int fromIndex = i;
            int toIndex = Math.min(i + size, inputList.size());
            result.add(inputList.subList(fromIndex, toIndex));
        }
        return result;
    }

    public DiscussionModel discussionToDiscussionModel(String repoName, Discussion discussion, long since,
            Map<String, Label> labels) {
        String updatedAt = discussion.updatedAt();
        Instant instant = Instant.parse(updatedAt);
        // log.thinking(instant.toString());
        if (instant.toEpochMilli() < since) {
            return null;
        }
        log.thinking("Discussion: " + discussion.title());
        Set<String> discussionLabels = discussion.labels().nodes().stream().map(LabelNameOnly::name)
                .collect(Collectors.toSet());
        if (discussionLabels.isEmpty()) {
            log.thinking("\tlabeling unlabelled");
            discussionLabels = determineLabels.labelDiscussion(labels.values(), discussion);
            log.thinking("\t" + discussionLabels.size() + " new labels");
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
                discussion.closed(), discussion.closedAt() == null ? 0 : Instant.parse(discussion.closedAt()).toEpochMilli(),
                Instant.parse(discussion.createdAt()).toEpochMilli(),
                Instant.parse(discussion.updatedAt()).toEpochMilli(), discussion.category().name(),
                discussionLabels,
                comments);
        return discussionModel;
    }

    public IssueModel issueToIssueModel(String repoName, Issue issue, Map<String, Label> labels) {
        Set<String> issueLabels = issue.labels().nodes().stream().map(LabelNameOnly::name)
                .collect(Collectors.toSet());
        log.thinking("Issue: " + issue.title());
        if (issueLabels.isEmpty()) {
            log.thinking("\tlabeling unlabelled");
            issueLabels = determineLabels.labelIssue(labels.values(), issue);
            log.thinking("\t" + issueLabels.size() + " new labels");
        }
        List<CommentModel> comments = issue.comments().nodes().stream()
                .map(comment -> new CommentModel(comment.author() == null ? "unknown" : comment.author().login(),
                        comment.body()))
                .collect(Collectors.toList());
        IssueModel issueModel = new IssueModel(repoName, issue.number(), issue.title(),
                issue.author() == null ? "unknown" : issue.author().login(),
                issue.body(),
                issue.closed(), issue.closedAt() == null ? 0 : Instant.parse(issue.closedAt()).toEpochMilli(),
                Instant.parse(issue.createdAt()).toEpochMilli(),
                Instant.parse(issue.updatedAt()).toEpochMilli(),
                issue.issueType() == null ? null : issue.issueType().name(), issueLabels, comments);
        return issueModel;
    }

    private void addLabels(Map<String, Object> map, Collection<String> labels) {
        for (String label : labels) {
            map.put("label_" + label, "true");
        }
    }

    public GithubEntry persist(IssueModel issue) {
        Map<String, Object> map = new HashMap<>();
        map.put("repo", issue.repo());
        map.put("number", issue.number());
        map.put("type", GitType.ISSUE.name());
        map.put("updatedAt", issue.updatedAt());
        map.put("createdAt", issue.createdAt());
        map.put("author", issue.author());
        map.put("closed", Boolean.toString(issue.closed()));
        map.put("closedAt", issue.closedAt());
        addLabels(map, issue.labels());
        RenderResult rendering = renderService.issue(issue);
        GithubEntry entry = new GithubEntry(issue.repo(), issue.number(), GitType.ISSUE, rendering.text(), map);
        persist(entry);
        return entry;

    }

    public GithubEntry persist(DiscussionModel discussion) {
        Map<String, Object> map = new HashMap<>();
        map.put("repo", discussion.repo());
        map.put("number", discussion.number());
        map.put("type", GitType.DISCUSSION.name());
        map.put("updatedAt", discussion.updatedAt());
        map.put("createdAt", discussion.createdAt());
        map.put("author", discussion.author());
        map.put("closed", Boolean.toString(discussion.closed()));
        map.put("closedAt", discussion.closedAt());
        addLabels(map, discussion.labels());
        RenderResult rendering = renderService.discussion(discussion);
        GithubEntry entry = new GithubEntry(discussion.repo(), discussion.number(), GitType.DISCUSSION, rendering.text(), map);
        persist(entry);
        return entry;
    }

    private void persist(GithubEntry entry) {
        try {
            transaction.begin();
            githubdb.persist(entry);
            transaction.commit();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void createDoc(List<Document> docs, DiscussionModel model, Map<String, Object> metaMap) {
        List<RenderResult> renderings = renderService.splitDocument(model);
        for (RenderResult rendering : renderings) {
            Metadata metadata = Metadata.from(metaMap);
            Document doc = Document.from(rendering.text(), metadata);
            docs.add(doc);
        }
    }

    public void createDoc(List<Document> docs, IssueModel model, Map<String, Object> metaMap) {
        List<RenderResult> renderings = renderService.splitDocument(model);
        for (RenderResult rendering : renderings) {
            Metadata metadata = Metadata.from(metaMap);
            Document doc = Document.from(rendering.text(), metadata);
            docs.add(doc);
        }
    }

}
