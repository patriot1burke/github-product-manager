package io.quarkiverse.ai.github.db;

import java.util.Map;

import jakarta.persistence.*;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import io.quarkiverse.ai.github.scanner.model.GitType;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;

@Entity
@Table(name = "github_entry")
@IdClass(GithubEntryKey.class)
public class GithubEntry extends PanacheEntityBase {
    @Id
    public String repo;
    @Id
    public int number;
    @Id
    public GitType type;

    public GithubEntry(String repo, int number, GitType type, String content, Map<String, Object> metadata) {
        this.repo = repo;
        this.number = number;
        this.type = type;
        this.content = content;
        this.metadata = metadata;
    }

    public GithubEntry() {
    }

    @Column(columnDefinition = "TEXT", nullable = false)
    public String content;

    public int contentTokens;

    @Column(columnDefinition = "TEXT")
    public String summary;

    public int summaryTokens;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSON")
    public Map<String, Object> metadata;

    public boolean embeddingCreated;

}
