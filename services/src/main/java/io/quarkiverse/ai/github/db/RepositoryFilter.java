package io.quarkiverse.ai.github.db;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

@Entity
@Table(name = "repository_filter")
@IdClass(RepositoryFilterKey.class)
public class RepositoryFilter extends PanacheEntityBase {

    @Id
    public String repository;
    @Id
    public String name;

    public String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSON")
    public Filters filters;

    public RepositoryFilter copy() {
        RepositoryFilter copy = new RepositoryFilter();
        copy.repository = this.repository;
        copy.description = this.description;
        copy.name = this.name;
        Filters f = new Filters();
        f.andFilters.addAll(this.filters.andFilters);
        f.orFilters.addAll(this.filters.orFilters);
        f.andLabels.addAll(this.filters.andLabels);
        f.orLabels.addAll(this.filters.orLabels);
        f.type = this.filters.type;
        f.updatedSince = this.filters.updatedSince;
        f.createdSince = this.filters.createdSince;
        copy.filters = f;
        return copy;
    }

}
