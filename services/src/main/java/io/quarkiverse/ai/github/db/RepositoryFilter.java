package io.quarkiverse.ai.github.db;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;

@Entity
@Table(name = "repository_filter")
public class RepositoryFilter extends PanacheEntityBase {

    @Id
    public String name;

    public String description;

    public String repository;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSON")
    public Filters filters;

    public String output() {
        List<String> parts = new ArrayList<>();
        if (name != null)
            parts.add("name: '" + name + "'");
        if (description != null)
            parts.add("description: '" + description + "'");
        if (repository != null)
            parts.add("repository: '" + repository + "'");
        if (filters != null) {
            if (filters.type != null)
                parts.add("type = '" + filters.type.toLowerCase() + "'");
            if (!filters.andLabels.isEmpty()) {
                String collect = filters.andLabels.stream()
                        .collect(Collectors.joining(","));
                collect = "required labels: " + collect;
                parts.add(collect);
            }
            if (!filters.orLabels.isEmpty()) {
                String collect = filters.orLabels.stream()
                        .collect(Collectors.joining(","));
                collect = "optional labels: " + collect;
                parts.add(collect);
            }
            if (!filters.andFilters.isEmpty()) {
                String collect = filters.andFilters.stream()
                        .collect(Collectors.joining(","));
                collect = "required filters: " + collect;
                parts.add(collect);
            }
            if (!filters.orFilters.isEmpty()) {
                String collect = filters.orFilters.stream()
                        .collect(Collectors.joining(","));
                collect = "optional filters: " + collect;
                parts.add(collect);
            }
            if (filters.updatedSince != null)
                parts.add("updatedSince: '" + filters.updatedSince + "'");
            if (filters.createdSince != null)
                parts.add("createdSince: '" + filters.createdSince + "'");
        }
        return String.join("\n", parts);
    }

    public RepositoryFilter copy() {
        RepositoryFilter copy = new RepositoryFilter();
        copy.repository = this.repository;
        copy.description = this.description;
        copy.name = this.name;
        if (this.filters != null) {
            Filters f = new Filters();
            f.andFilters.addAll(this.filters.andFilters);
            f.orFilters.addAll(this.filters.orFilters);
            f.andLabels.addAll(this.filters.andLabels);
            f.orLabels.addAll(this.filters.orLabels);
            f.type = this.filters.type;
            f.updatedSince = this.filters.updatedSince;
            f.createdSince = this.filters.createdSince;
            copy.filters = f;
        }
        return copy;
    }

}
