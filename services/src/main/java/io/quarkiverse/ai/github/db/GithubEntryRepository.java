package io.quarkiverse.ai.github.db;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkiverse.ai.github.scanner.model.GitType;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;

@ApplicationScoped
public class GithubEntryRepository implements PanacheRepositoryBase<GithubEntry, GithubEntryKey> {
    public GithubEntry find(String repo, int number, GitType type) {
        return findById(new GithubEntryKey(repo, number, type));
    }

}
