package io.quarkiverse.ai.github.api;

public class Repo {
    public String owner;
    public String name;

    public Repo(String repo) {
        String[] parts = repo.split("/");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid repository format: " + repo);
        }
        owner = parts[0];
        name = parts[1];
    }

    @Override
    public String toString() {
        return owner + "/" + name;
    }

}
