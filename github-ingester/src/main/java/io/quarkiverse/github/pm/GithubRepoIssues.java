package io.quarkiverse.github.pm;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

public class GithubRepoIssues {
    public String repo;
    public long lastUpdated;
    public IssueIndex issues;

    @JsonIgnore
    public InMemoryEmbeddingStore<TextSegment> embeddings;

    @JsonIgnore
    Path indexFile;

    @JsonIgnore
    Path embeddingsFile;

    @JsonIgnore
    ObjectMapper objectMapper;

    public static class MustReindexException extends RuntimeException {
        public MustReindexException(String message) {
            super(message);
        }

        public MustReindexException() {

        }
    }

    public static boolean exists(String baseDirectory, String repoName) {
        Path path = Path.of(baseDirectory, repoName);
        Path indexPath = path.resolve("repo.json");
        Path embeddingsPath = path.resolve("embeddings.json");
        return Files.exists(indexPath) && Files.exists(embeddingsPath);
    }

    public static GithubRepoIssues load(String baseDirectory, String repoName, ObjectMapper objectMapper) {
        Path path = Path.of(baseDirectory, repoName);
        Path indexPath = path.resolve("repo.json");
        Path embeddingsPath = path.resolve("embeddings.json");
        if (!exists(baseDirectory, repoName)) {
            throw new MustReindexException("Repo does not exist");
        }
        try {

            GithubRepoIssues repo = objectMapper.readValue(indexPath.toFile(), GithubRepoIssues.class);
            repo.indexFile = indexPath;
            repo.embeddingsFile = embeddingsPath;
            repo.embeddings = InMemoryEmbeddingStore.fromFile(embeddingsPath);
            repo.objectMapper = objectMapper;
            return repo;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load repo", e);
        }
    }

    public static GithubRepoIssues createIfNotExists(String baseDirectory, String repoName, ObjectMapper objectMapper) {
        if (exists(baseDirectory, repoName)) {
            return load(baseDirectory, repoName, objectMapper);
        }
        Path path = Path.of(baseDirectory, repoName);
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create directory", e);
            }
        }
        GithubRepoIssues repo = new GithubRepoIssues();
        repo.repo = repoName;
        repo.issues = new IssueIndex();
        repo.embeddings = new InMemoryEmbeddingStore<>();
        repo.indexFile = Path.of(baseDirectory, repoName, "repo.json");
        repo.embeddingsFile = Path.of(baseDirectory, repoName, "embeddings.json");
        repo.objectMapper = objectMapper;
        return repo;
    }

    public void save() {
        try {
            objectMapper.writeValue(indexFile.toFile(), this);
        } catch (Exception e) {
            throw new RuntimeException("Failed to save repo", e);
        }
        embeddings.serializeToFile(embeddingsFile);
    }
}
