package io.quarkiverse.github.index;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import io.quarkiverse.github.index.model.ChangeSet;
import io.quarkiverse.github.index.model.DiscussionModel;
import io.quarkiverse.github.index.model.IssueModel;
import io.quarkiverse.github.util.AppLogger;

@ApplicationScoped
public class RagIndexer {
    static AppLogger log = AppLogger.getLogger(RagIndexer.class);

    @ConfigProperty(name = "product.manager.cache.dir")
    String baseDirectory;

    static class EmbeddingStoreHolder {
        public EmbeddingStoreHolder(InMemoryEmbeddingStore<TextSegment> store) {
            this.store = store;
        }

        InMemoryEmbeddingStore<TextSegment> store;
        boolean dirty = false;
    }

    Path embeddingsPath;
    EmbeddingStoreHolder embeddingsStore;

    @PostConstruct
    public void postConstruct() {
        embeddingsPath = Path.of(baseDirectory, "embeddings.json");
    }

    public InMemoryEmbeddingStore<TextSegment> load(String repoName) {
        if (embeddingsStore != null) {
            return embeddingsStore.store;
        }
        InMemoryEmbeddingStore<TextSegment> store;
        Path path = Path.of(baseDirectory, repoName, "rag.json");
        if (Files.exists(path)) {
            store = InMemoryEmbeddingStore.fromFile(path);
        } else {
            store = new InMemoryEmbeddingStore<>();
        }
        embeddingsStore = new EmbeddingStoreHolder(store);
        return embeddingsStore.store;
    }

    @PreDestroy
    public void preDestroy() {
        save();
    }

    public void save() {
        if (embeddingsStore.dirty) {
            embeddingsStore.store.serializeToFile(embeddingsPath);
            embeddingsStore.dirty = false;
        }
    }

    @Inject
    PullCacheService pullCacheService;

    @Inject
    EmbeddingModel embeddingModel;

    @Inject
    RenderService renderService;

    public void indexAll() {
        Set<String> repos = pullCacheService.repos();
        for (String repoName : repos) {
            index(repoName);
        }
    }

    public void index(String repoName) {
        PullCache pullCache = pullCacheService.load(repoName);
        if (pullCache.discussions.isEmpty() && pullCache.issues.isEmpty()) {
            return;
        }
        ChangeSet changeSet = new ChangeSet(pullCache.discussions.keySet(), pullCache.issues.keySet());
        index(repoName, changeSet);
    }

    public void index(String repoName, ChangeSet changeSet) {
        if (changeSet.discussions().isEmpty() && changeSet.issues().isEmpty()) {
            return;
        }
        try {
            prune(repoName, changeSet);

            InMemoryEmbeddingStore<TextSegment> store = load(repoName);
            PullCache pullCache = pullCacheService.load(repoName);
            EmbeddingStoreIngestor ingester = EmbeddingStoreIngestor.builder()
                    .embeddingModel(embeddingModel)
                    .embeddingStore(store)
                    .build();

            List<Document> docs = new ArrayList<>();
            log.thinking("Indexing " + changeSet.discussions().size() + " discussions");
            for (Integer discussionNumber : changeSet.discussions()) {
                DiscussionModel discussion = pullCache.discussions.get(discussionNumber);
                Document doc = createDoc(discussion);
                docs.add(doc);
            }
            ingester.ingest(docs);
            docs.clear();
            log.thinking("Indexing " + changeSet.issues().size() + " issues");
            for (Integer issueNumber : changeSet.issues()) {
                IssueModel issue = pullCache.issues.get(issueNumber);
                Document doc = createDoc(issue);
                docs.add(doc);
            }
            ingester.ingest(docs);
            embeddingsStore.dirty = true;
        } catch (RuntimeException e) {
            embeddingsStore = null;
            throw e;
        }
    }

    private Document createDoc(IssueModel issue) {
        Map<String, Object> map = new HashMap<>();
        map.put("repo", issue.repo());
        map.put("number", issue.number());
        map.put("type", "issue");
        map.put("updatedAt", issue.updatedAt());
        for (String label : issue.labels()) {
            map.put("label." + label, true);
        }
        Metadata metadata = Metadata.from(map);
        Document doc = Document.from(renderService.issue(issue).text(), metadata);
        return doc;
    }

    private Document createDoc(DiscussionModel discussion) {
        Map<String, Object> map = new HashMap<>();
        map.put("repo", discussion.repo());
        map.put("number", discussion.number());
        map.put("type", "discussion");
        map.put("updatedAt", discussion.updatedAt());
        for (String label : discussion.labels()) {
            map.put("label." + label, true);
        }
        Metadata metadata = Metadata.from(map);
        Document doc = Document.from(renderService.discussion(discussion).text(), metadata);
        return doc;
    }

    public void prune(String repoName, ChangeSet changeSet) {
        try {
            InMemoryEmbeddingStore<TextSegment> store = load(repoName);
            Filter repo = new MetadataFilterBuilder("repo").isEqualTo(repoName);
            for (Integer discussionNumber : changeSet.discussions()) {
                Filter type = new MetadataFilterBuilder("type").isEqualTo("discussion");
                Filter number = new MetadataFilterBuilder("number").isEqualTo(discussionNumber);
                store.removeAll(Filter.and(repo, Filter.and(type, number)));
            }
            for (Integer issueNumber : changeSet.issues()) {
                Filter type = new MetadataFilterBuilder("type").isEqualTo("issue");
                Filter number = new MetadataFilterBuilder("number").isEqualTo(issueNumber);
                store.removeAll(Filter.and(repo, Filter.and(type, number)));
            }
            embeddingsStore.dirty = true;
        } catch (RuntimeException e) {
            embeddingsStore = null;
            throw e;
        }
    }

    public void newPull(String repoName, ChangeSet changeSet) {
        InMemoryEmbeddingStore<TextSegment> store = load(repoName);
        if (store.isEmpty()) {
            return;
        }
        index(repoName, changeSet);
    }

    public void clear(String repoName) {
        if (embeddingsStore != null) {
            Filter repo = new MetadataFilterBuilder("repo").isEqualTo(repoName);
            embeddingsStore.store.removeAll(repo);
            embeddingsStore.dirty = true;
        }
    }

    public void destroy() {
        embeddingsStore = null;
        if (Files.exists(embeddingsPath)) {
            try {
                Files.delete(embeddingsPath);
            } catch (IOException e) {
                throw new RuntimeException("Failed to destroy embeddings", e);
            }
        }
    }
}
