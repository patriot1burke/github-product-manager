package io.quarkiverse.ai.github.db;

import java.io.Serializable;
import java.util.Objects;

public class GithubLabelKey implements Serializable {
    public String repository;
    public String name;

    public GithubLabelKey() {
    }

    public GithubLabelKey(String repository, String name) {
        this.repository = repository;
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof GithubLabelKey that))
            return false;
        return Objects.equals(repository, that.repository) && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(repository, name);
    }
}
