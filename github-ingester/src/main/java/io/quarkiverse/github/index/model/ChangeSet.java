package io.quarkiverse.github.index.model;

import java.util.Set;

public record ChangeSet(Set<Integer> discussions, Set<Integer> issues) {
}