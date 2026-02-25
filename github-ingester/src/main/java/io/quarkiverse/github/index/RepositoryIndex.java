package io.quarkiverse.github.index;

import java.util.HashSet;
import java.util.Set;

public class RepositoryIndex {
    public String repo;
    public Set<String> ignoredCategories = new HashSet<>();
    public Set<String> ignoredLabels = new HashSet<>();
    public long lastPulled = 0;

}
