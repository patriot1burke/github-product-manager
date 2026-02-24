package io.quarkiverse.github.index;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.quarkiverse.github.api.Discussions.DiscussionCategory;
import io.quarkiverse.github.api.Labels.Label;

public class RepositoryIndex {
    public String repo;
    public Set<String> ignoredCategories = new HashSet<>();
    public Set<String> ignoredLabels = new HashSet<>();
    public long lastPulled = 0;

}
