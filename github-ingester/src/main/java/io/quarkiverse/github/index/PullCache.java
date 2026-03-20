package io.quarkiverse.github.index;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.quarkiverse.github.index.model.DiscussionModel;
import io.quarkiverse.github.index.model.IssueModel;

public class PullCache {
    public PullCache() {
    }

    public PullCache(String repo) {
        this.repo = repo;
    }

    public String repo;
    public long lastPulled = 0;

    @JsonDeserialize(as = ConcurrentHashMap.class)
    public Map<Integer, DiscussionModel> discussions = new ConcurrentHashMap<>();
    @JsonDeserialize(as = ConcurrentHashMap.class)
    public Map<Integer, IssueModel> issues = new ConcurrentHashMap<>();

    @JsonIgnore
    public boolean dirty = false;
}
