package io.quarkiverse.ai.github.db;

import io.quarkiverse.ai.github.api.Github;
import io.quarkiverse.ai.github.api.Labels;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@ApplicationScoped
public class GithubLabelRepository implements PanacheRepositoryBase<GithubLabel, GithubLabelKey> {

    @Inject
    Github github;

    @Transactional
    public void syncLabels(String repo) {
        Map<String, Labels.Label> labels = github.repository(repo).labels();
        for (Labels.Label label : labels.values()) {
            GithubLabel existing = findById(new GithubLabelKey(repo, label.name()));
            if (existing == null) {
                persist(new GithubLabel(repo, label.name(), label.description()));
            } else if (!Objects.equals(existing.description, label.description())) {
                existing.description = label.description();
            }
        }
    }

    public List<GithubLabel> findByRepository(String repo) {
        return list("repository", repo);
    }
}
