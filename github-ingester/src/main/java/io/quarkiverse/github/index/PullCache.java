package io.quarkiverse.github.index;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.quarkiverse.github.index.model.DiscussionModel;
import io.quarkiverse.github.index.model.IssueModel;

public class PullCache {
    public long lastPulled = 0;

    public Map<Integer, DiscussionModel> discussions = new HashMap<>();
    public Map<Integer, IssueModel> issues = new HashMap<>();

    @JsonIgnore
    public boolean dirty = false;
}
