package io.quarkiverse.ai.github.db;

import io.quarkiverse.ai.github.scanner.model.GitType;

public record GithubEntryKey(String repo, int number, GitType type) {

}
