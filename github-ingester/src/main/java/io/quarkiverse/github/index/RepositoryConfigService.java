package io.quarkiverse.github.index;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkiverse.github.util.AppLogger;

@ApplicationScoped
public class RepositoryConfigService {
    static AppLogger log = AppLogger.getLogger(RepositoryConfigService.class);

    @ConfigProperty(name = "product.manager.cache.dir")
    String baseDirectory;

    @Inject
    ObjectMapper objectMapper;

    public boolean exists(String repoName) {
        Path path = Path.of(baseDirectory, repoName);
        Path indexPath = path.resolve("config.json");
        return Files.exists(indexPath);
    }

    public RepositoryConfig load(String repoName) {
        Path path = Path.of(baseDirectory, repoName);
        Path indexPath = path.resolve("config.json");
        try {

            RepositoryConfig repo = objectMapper.readValue(indexPath.toFile(), RepositoryConfig.class);
            return repo;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load repo", e);
        }
    }

    public RepositoryConfig createIfNotExists(String repoName) {
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
        RepositoryConfig repo = new RepositoryConfig();
        repo.repo = repoName;
        return repo;
    }

    public void save(RepositoryConfig repo) {
        try {
            Path indexFile = Path.of(baseDirectory, repo.repo, "config.json");
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(indexFile.toFile(), repo);
        } catch (Exception e) {
            throw new RuntimeException("Failed to save repo", e);
        }
    }

}
