package io.quarkiverse.github.pm;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import io.quarkiverse.github.pm.util.AppLogger;
import io.quarkiverse.github.pm.util.MessageIcons;

@ApplicationScoped
public class GithubRepoIssuesService {
    @Inject
    ObjectMapper objectMapper;

    @Inject
    @ConfigProperty(name = "product.manager.cache.dir")
    String baseDirectory;

    @Inject
    EmbeddingModel embeddingModel;

    @Inject
    Github github;

    Map<String, GithubRepoIssues> repos = new HashMap<>();

    static AppLogger log = AppLogger.getLogger(GithubRepoIssuesService.class);

    public void pullRepo(String repoName) {
        log.info("Pulling issues from " + repoName);
        GithubRepoIssues repo = repos.get(repoName);
        if (repo == null) {
            repo = GithubRepoIssues.createIfNotExists(baseDirectory, repoName, objectMapper);
            repos.put(repo.repo, repo);
        }
        long lastPulled = repo.lastUpdated;
        String since = lastPulled <= 0 ? null
                : DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
                        .withZone(ZoneOffset.UTC)
                        .format(Instant.ofEpochMilli(lastPulled));

        repo.lastUpdated = System.currentTimeMillis();
        WebTarget path = github.issuesPath(repoName);
        if (since != null) {
            path = path.queryParam("since", since);
        }
        int pageSize = 30;
        path = path.queryParam("per_page", pageSize);
        Invocation.Builder request = null;
        if (since == null) {
            request = github.issues(repoName);
        } else {
            request = github.issues(repoName, since);
        }
        Link next = null;
        int numIssues = 0;
        do {
            log.info("Total pages: " + numIssues++ + ". Pulling next " + pageSize + " issues...");
            next = pull(repoName, repo, request);
            if (next != null) {
                request = github.from(next);
            }
        } while (next != null);
        repo.save();
        log.info(MessageIcons.SUCCESS_ICON + " Finished pull.");
        log.info("Number of issues: " + repo.issues.numIssues());
        log.info("Number of issues by label: " + repo.issues.numIssuesByLabel());
        log.info("Number of issues by type: " + repo.issues.numIssuesByType());
    }

    private Link pull(String repoName, GithubRepoIssues repo, Invocation.Builder request) {
        Response response = request.get();

        if (response.getStatus() != 200) {
            throw new RuntimeException("Failed to pull issues from repo: " + repoName);
        }

        JsonNode issues = response.readEntity(JsonNode.class);
        if (!issues.isArray()) {
            new RuntimeException("Unexpected json format from repo issues");
        }
        ingestIssues(repo, issues);
        Link next = response.getLink("next");
        response.close();
        return next;
    }

    private void ingestIssues(GithubRepoIssues repo, JsonNode issues) {
        List<Document> docs = new ArrayList<>();
        for (JsonNode issue : issues) {
            String number = issue.get("number").asText();
            String title = issue.get("title").asText();
            String body = issue.get("body").asText();
            //log.info("Ingesting issue: " + number + " " + title);
            Set<String> labels = new HashSet<>();
            JsonNode labelsNode = issue.get("labels");
            if (labelsNode != null && labelsNode.isArray() && !labelsNode.isNull()) {
                for (JsonNode label : labelsNode) {
                    String asText = label.get("name").asText();
                    labels.add(asText);
                }
            } else if (labelsNode != null && !labelsNode.isNull()) {
                String asText = labelsNode.get("name").asText();
                labels.add(asText);
            }
            //log.debug("Labels: " + labels);
            JsonNode typeNode = issue.get("type");
            String type = null;
            if (typeNode != null && !typeNode.isNull()) {
                type = typeNode.get("name").asText();
            }
            //log.debug("Type: " + type);
            repo.issues.mergeIssue(number, labels, type);

            Filter filter = new MetadataFilterBuilder("number").isEqualTo(number);
            repo.embeddings.removeAll(filter);

            String content = "Title: " + title + "\n\nDescription:\n" + body;
            Map<String, String> metadata = new HashMap<>();
            metadata.put("number", number);
            if (type != null) {
                metadata.put("type", type);
            }
            for (String label : labels) {
                metadata.put(label, "true");
            }
            Document doc = Document.from(content, Metadata.from(metadata));
            docs.add(doc);
        }
        EmbeddingStoreIngestor ingester = EmbeddingStoreIngestor.builder()
                .embeddingModel(embeddingModel)
                .embeddingStore(repo.embeddings)
                .documentSplitter(DocumentSplitters.recursive(18000, 500))
                .build();

        ingester.ingest(docs);
    }

}
