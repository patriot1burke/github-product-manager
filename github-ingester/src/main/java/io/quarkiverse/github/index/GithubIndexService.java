package io.quarkiverse.github.index;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkiverse.github.api.Discussions.Discussion;
import io.quarkiverse.github.api.Discussions.DiscussionCategory;
import io.quarkiverse.github.api.Discussions.DiscussionCategoryConnection;
import io.quarkiverse.github.api.Discussions.DiscussionConnection;
import io.quarkiverse.github.api.Github;
import io.quarkiverse.github.api.GithubAPI;
import io.quarkiverse.github.api.Labels.Label;
import io.quarkiverse.github.api.Labels.LabelConnection;
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

    public Map<String, DiscussionCategory> discussionCategories(String repoName) {
        Map<String, DiscussionCategory> categories = new HashMap<>();
        GithubAPI.Repository repository = github.repository(repoName);
        DiscussionCategoryConnection discussionCategories = null;
        String after = null;
        do {
            if (after == null) {
                discussionCategories = repository.discussionCategories(100);
            } else {
                discussionCategories = repository.discussionCategories(100, after);
                after = discussionCategories.pageInfo().endCursor();
            }
            for (DiscussionCategory discussionCategory : discussionCategories.nodes()) {
                categories.put(discussionCategory.name(), discussionCategory);
            }
        } while (discussionCategories.pageInfo().hasNextPage());
        return categories;
    }

    public Map<String, Label> labels(String repoName) {
        GithubAPI.Repository repository = github.repository(repoName);
        LabelConnection connection = null;
        Map<String, Label> result = new HashMap<>();
        String after = null;
        do {
            if (after == null) {
                connection = repository.labels(100);
            } else {
                connection = repository.labels(100, after);
            }
            for (Label label : connection.nodes()) {
                result.put(label.name(), label);
            }
            after = connection.pageInfo().endCursor();
        } while (connection.pageInfo().hasNextPage());
        return result;
    }

    public void pullDiscussions4(String repoName) throws Exception {
        GithubAPI.Repository repository = github.repository(repoName);
        DiscussionConnection discussions = repository.discussions(10);
        log.info("************************************");
        int num = 1;
        for (Discussion discussion : discussions.nodes()) {
            log.info("Discussion[" + num++ + "]: " + discussion.title() + " - " + discussion.createdAt());
        }
        if (discussions.pageInfo().hasNextPage()) {
            String after = discussions.pageInfo().endCursor();
            discussions = repository.discussions(10, after);
            for (Discussion discussion : discussions.nodes()) {
                log.info("Discussion[" + num++ + "]: " + discussion.title() + " - " + discussion.createdAt());
            }
        }
    }
}
