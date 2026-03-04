package io.quarkiverse.github.index;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    Map<String, EmbeddingStoreHolder> storeMap = new HashMap<>();

    public InMemoryEmbeddingStore<TextSegment> load(String repoName) {
        EmbeddingStoreHolder holder = storeMap.get(repoName);
        if (holder != null) {
            return holder.store;
        }
        InMemoryEmbeddingStore<TextSegment> store;
        Path path = Path.of(baseDirectory, repoName, "rag.json");
        if (Files.exists(path)) {
            store = InMemoryEmbeddingStore.fromFile(path);
        } else {
            store = new InMemoryEmbeddingStore<>();
        }
        storeMap.put(repoName, new EmbeddingStoreHolder(store));
        return store;
    }

    @PreDestroy
    public void preDestroy() {
        save();
    }

    public void save() {
        for (String repoName : storeMap.keySet()) {
            Path path = Path.of(baseDirectory, repoName, "rag.json");
            EmbeddingStoreHolder holder = storeMap.get(repoName);
            if (holder.dirty) {
                holder.store.serializeToFile(path);
                holder.dirty = false;
            }
        }
    }

    @Inject
    PullCacheService pullCacheService;

    @Inject
    EmbeddingModel embeddingModel;

    @Inject
    RenderService renderService;

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
                Map<String, Object> map = new HashMap<>();
                map.put("number", discussionNumber);
                map.put("type", "discussion");
                for (String label : discussion.labels()) {
                    map.put("label." + label, true);
                }
                Metadata metadata = Metadata.from(map);
                Document doc = Document.from(renderService.discussion(discussion), metadata);
                docs.add(doc);
            }
            ingester.ingest(docs);
            docs.clear();
            log.thinking("Indexing " + changeSet.issues().size() + " issues");
            for (Integer issueNumber : changeSet.issues()) {
                IssueModel issue = pullCache.issues.get(issueNumber);
                Map<String, Object> map = new HashMap<>();
                map.put("number", issueNumber);
                map.put("type", "issue");
                for (String label : issue.labels()) {
                    map.put("label." + label, true);
                }
                Metadata metadata = Metadata.from(map);
                Document doc = Document.from(renderService.issue(issue), metadata);
                docs.add(doc);
            }
            ingester.ingest(docs);
            storeMap.get(repoName).dirty = true;
        } catch (RuntimeException e) {
            storeMap.remove(repoName);
            throw e;
        }
    }

    public void prune(String repoName, ChangeSet changeSet) {
        try {
            InMemoryEmbeddingStore<TextSegment> store = load(repoName);
            for (Integer discussionNumber : changeSet.discussions()) {
                Filter type = new MetadataFilterBuilder("type").isEqualTo("discussion");
                Filter number = new MetadataFilterBuilder("number").isEqualTo(discussionNumber);
                store.removeAll(Filter.and(type, number));
            }
            for (Integer issueNumber : changeSet.issues()) {
                Filter type = new MetadataFilterBuilder("type").isEqualTo("issue");
                Filter number = new MetadataFilterBuilder("number").isEqualTo(issueNumber);
                store.removeAll(Filter.and(type, number));
            }
            storeMap.get(repoName).dirty = true;
        } catch (RuntimeException e) {
            storeMap.remove(repoName);
            throw e;
        }
    }

    public void clear(String repoName) {
        storeMap.remove(repoName);
        Path path = Path.of(baseDirectory, repoName, "rag.json");
        try {
            if (Files.exists(path)) {
                Files.delete(path);
            }
        } catch (IOException e) {
        }
    }
}
