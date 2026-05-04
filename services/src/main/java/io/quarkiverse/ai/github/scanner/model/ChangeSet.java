package io.quarkiverse.ai.github.scanner.model;

import java.util.Set;

public record ChangeSet(Set<Integer> discussions, Set<Integer> issues) {
}