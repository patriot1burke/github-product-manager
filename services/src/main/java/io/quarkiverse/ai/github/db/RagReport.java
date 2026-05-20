package io.quarkiverse.ai.github.db;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;

@Entity
@Table(name = "rag_report")
public class RagReport extends PanacheEntityBase {
    @Id
    public String name;

    public String description;
    public String filter;

    @Column(columnDefinition = "TEXT")
    public String prompt;
}
