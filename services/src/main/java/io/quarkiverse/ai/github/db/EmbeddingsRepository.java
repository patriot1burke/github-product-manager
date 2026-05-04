package io.quarkiverse.ai.github.db;

import java.sql.*;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import io.agroal.api.AgroalDataSource;
import io.quarkiverse.ai.github.scanner.model.DiscussionModel;
import io.quarkiverse.ai.github.scanner.model.GitType;
import io.quarkiverse.ai.github.scanner.model.IssueModel;
import io.quarkus.runtime.Startup;

@ApplicationScoped
public class EmbeddingsRepository {
    @Inject
    AgroalDataSource db;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    EmbeddingStore<TextSegment> embeddingsStore;

    @Startup
    public void startup() {
        embeddingsStore.generateIds(1); // initialize the bean
    }

    public long lastPulled(String repo) {
        try (Connection conn = db.getConnection()) {
            Statement stmt = conn.createStatement();
            ResultSet resultSet = stmt.executeQuery(
                    "SELECT MAX(metadata->>'updatedAt') FROM embeddings WHERE metadata->>'repo' = '" + repo + "'");
            if (resultSet.next()) {
                return resultSet.getLong(1);
            }
            return 0L;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean prune(IssueModel issue) {
        String repo = issue.repo();
        int number = issue.number();
        String type = GitType.ISSUE.name();
        return deleteEmbedding(repo, number, type);
    }

    public boolean prune(DiscussionModel discussion) {
        String repo = discussion.repo();
        int number = discussion.number();
        String type = GitType.DISCUSSION.name();
        return deleteEmbedding(repo, number, type);
    }

    public Map<String, Object> metadata(String repo, int number, String type) {
        try (Connection conn = db.getConnection()) {
            Statement stmt = conn.createStatement();
            ResultSet resultSet = stmt.executeQuery("SELECT metadata FROM embeddings WHERE " +
                    "metadata->>'repo' = '" + repo + "'" +
                    " AND (metadata->>'number')::int = " + number +
                    " AND metadata->>'type' = '" + type + "'");
            if (resultSet.next()) {
                String json = stmt.getResultSet().getString(1);
                return objectMapper.readValue(json, Map.class);

            } else {
                throw new RuntimeException("Unable to query metadata");
            }
        } catch (SQLException | JsonProcessingException e) {
            throw new RuntimeException("Unable to query metadata", e);
        }
    }

    public boolean updateMetadata(String repo, int number, String type, Map<String, Object> metadata) {
        try (Connection conn = db.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement("UPDATE embeddings SET metadata = ? WHERE " +
                    "metadata->>'repo' = ?" +
                    " AND (metadata->>'number')::int = ?" +
                    " AND metadata->>'type' = ?");
            stmt.setString(1, objectMapper.writeValueAsString(metadata));
            stmt.setString(2, repo);
            stmt.setInt(3, number);
            stmt.setString(4, type);
            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean deleteEmbedding(String repo, int number, String type) {
        try (Connection conn = db.getConnection()) {
            Statement stmt = conn.createStatement();
            return stmt.executeUpdate("DELETE FROM embeddings WHERE " +
                    "metadata->>'repo' = '" + repo + "'" +
                    " AND (metadata->>'number')::int = " + number +
                    " AND metadata->>'type' = '" + type + "'") > 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
