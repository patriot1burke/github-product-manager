package io.quarkiverse.github.pm;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class IssueIndex {

    public static record Issue(String number) {

        @Override
        public int hashCode() {
            return number.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            return number.equals(((Issue) obj).number);
        }
    }

    public Map<String, Issue> issues = new HashMap<>();
    public Map<String, Set<String>> labelMap = new HashMap<>();
    public Map<String, Set<String>> typeMap = new HashMap<>();

    public int numIssues() {
        return issues.size();
    }

    public Map<String, Integer> numIssuesByLabel() {
        Map<String, Integer> result = new HashMap<>();
        labelMap.forEach((label, issues) -> result.put(label, issues.size()));
        return result;
    }

    public Map<String, Integer> numIssuesByType() {
        Map<String, Integer> result = new HashMap<>();
        typeMap.forEach((type, issues) -> result.put(type, issues.size()));
        return result;
    }

    public void closeIssue(String number) {
        Issue issue = issues.remove(number);
        if (issue != null) {
            labelMap.values().forEach(issues -> issues.remove(number));
            typeMap.values().forEach(issues -> issues.remove(number));
        }
    }

    public void mergeIssue(String issue, Set<String> labels, String type) {
        closeIssue(issue);
        issues.put(issue, new Issue(issue));
        labels.forEach(label -> labelMap.computeIfAbsent(label, k -> new HashSet<>()).add(issue));
        if (type != null)
            typeMap.computeIfAbsent(type, k -> new HashSet<>()).add(issue);
    }
}
