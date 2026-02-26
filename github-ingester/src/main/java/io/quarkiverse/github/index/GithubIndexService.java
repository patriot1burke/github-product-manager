package io.quarkiverse.github.index;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkiverse.github.api.Github;
import io.quarkiverse.github.pm.util.AppLogger;

@ApplicationScoped
public class GithubIndexService {
    static AppLogger log = AppLogger.getLogger(GithubIndexService.class);

    @ConfigProperty(name = "product.manager.cache.dir")
    String baseDirectory;

    @Inject
    Github github;

    @Inject
    ObjectMapper objectMapper;

    public boolean exists(String repoName) {
        Path path = Path.of(baseDirectory, repoName);
        Path indexPath = path.resolve("index.json");
        return Files.exists(indexPath);
    }

    public RepositoryIndex load(String repoName) {
        Path path = Path.of(baseDirectory, repoName);
        Path indexPath = path.resolve("index.json");
        try {

            RepositoryIndex repo = objectMapper.readValue(indexPath.toFile(), RepositoryIndex.class);
            return repo;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load repo", e);
        }
    }

    public RepositoryIndex createIfNotExists(String repoName) {
        String[] spit = repoName.split("/");
        if (spit.length != 2) {
            throw new RuntimeException("Invalid repo name: " + repoName);
        }
        if (exists(repoName)) {
            log.debugv("Index already exists for repo: {0}", repoName);
            return load(repoName);
        }
        log.debugv("Creating index for repo: {0}", repoName);
        Path path = Path.of(baseDirectory, repoName);
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create directory", e);
            }
        }
        RepositoryIndex repo = new RepositoryIndex();
        repo.repo = repoName;
        return repo;
    }

    public void save(RepositoryIndex repo) {
        try {
            Path indexFile = Path.of(baseDirectory, repo.repo, "index.json");
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(indexFile.toFile(), repo);
        } catch (Exception e) {
            throw new RuntimeException("Failed to save repo", e);
        }
    }
}
