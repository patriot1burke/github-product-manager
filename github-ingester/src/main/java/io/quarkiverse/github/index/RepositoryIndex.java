package io.quarkiverse.github.pm;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.quarkiverse.github.api.Discussions.DiscussionCategory;
import io.quarkiverse.github.api.Labels.Label;

public class RepositoryIndex {
    public String owner;
    public String name;
    public String repo;
    public Map<String, Label> labels = new HashMap<>();
    public Map<String, DiscussionCategory> discussionCategories = new HashMap<>();
    public Set<String> ignoredCategories = new HashSet<>();
    public Set<String> ignoredLabels = new HashSet<>();
    public long lastPulled = 0;

}
