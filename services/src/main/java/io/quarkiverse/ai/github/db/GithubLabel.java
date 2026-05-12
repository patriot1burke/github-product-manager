package io.quarkiverse.ai.github.db;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

@Entity
@Table(name = "github_label")
@IdClass(GithubLabelKey.class)
public class GithubLabel extends PanacheEntityBase {

    @Id
    public String repository;

    @Id
    public String name;

    public String description;

    public GithubLabel() {
    }

    public GithubLabel(String repository, String name, String description) {
        this.repository = repository;
        this.name = name;
        this.description = description;
    }
}
