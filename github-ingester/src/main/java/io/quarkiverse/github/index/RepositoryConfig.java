package io.quarkiverse.github.index;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class RepositoryConfig {
    public String repo;
    public Set<String> ignoredCategories = new HashSet<>();
    public Set<String> ignoredLabels = new HashSet<>();

    @JsonIgnore
    private List<Pattern> ignoredLabelsPatterns = null;

    public void addIgnoredLabel(String pattern) {
        ignoredLabels.add(pattern);
        compile();
    }

    public void removeIgnoredLabel(String pattern) {
        ignoredLabels.remove(pattern);
        compile();
    }

    public boolean ignoreLabel(String label) {
        if (ignoredLabelsPatterns == null) {
            compile();
        }
        for (Pattern pattern : ignoredLabelsPatterns) {
            if (pattern.matcher(label).matches()) {
                return true;
            }
        }
        return false;
    }

    private synchronized void compile() {
        if (ignoredLabelsPatterns != null) {
            return;
        }
        ignoredLabelsPatterns = new ArrayList<>();
        for (String pattern : ignoredLabels) {
            ignoredLabelsPatterns.add(Pattern.compile(pattern));
        }
    }
}
