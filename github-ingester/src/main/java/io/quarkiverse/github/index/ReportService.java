package io.quarkiverse.github.index;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkiverse.github.api.Discussions.Discussion;
import io.quarkiverse.github.api.Github;
import io.quarkiverse.github.api.GithubAPI.Repository;
import io.quarkiverse.github.api.Issues;
import io.quarkiverse.github.api.Labels.Label;
import io.quarkiverse.github.api.Labels.LabelNameOnly;
import io.quarkiverse.github.pm.util.AppLogger;

@ApplicationScoped
public class ReportService {
    static AppLogger log = AppLogger.getLogger(ReportService.class);

    @Inject
    Github github;

    @Inject
    CalculateLabelsPrompt labelBuilder;

    public static record LabelReport(String name, int count) {

    }

    public static record Tally(int total, int unlabeled, List<LabelReport> labelCounts) {
    }

    public static record BasicReport(String startDate, String endDate, List<LabelReport> labelCounts, Tally discussions,
            Tally issues) {
    }

    public enum DateRange {

        MONTH(30),
        QUARTER(90),
        YEAR(365)

        ;

        int days;

        DateRange(int days) {
            this.days = days;
        }

        public long fromToday() {
            Instant today = Instant.now();
            return today.minus(days, ChronoUnit.DAYS).toEpochMilli();
        }

        public String fromString() {
            Instant today = Instant.now();
            return today.minus(days, ChronoUnit.DAYS).toString();
        }

    }

    private Map<String, Label> getLabels(Repository repository, RepositoryIndex repoIndex) {
        return repository.labels().entrySet().stream()
                .filter(entry -> !repoIndex.ignoreLabel(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Set<String> processLLMLabels(Set<String> labels) {
        Set<String> newLabels = new HashSet<>();
        for (String label : labels) {
            if (label.startsWith("- ")) {
                newLabels.add(label.substring(2));
            } else {
                newLabels.add(label);
            }
        }
        return newLabels;
    }

    @Inject
    ObjectMapper objectMapper;

    public Set<String> processLLMLabels(String labels) {
        try {
            return objectMapper.readValue(labels, new TypeReference<Set<String>>() {
            });
        } catch (Exception e) {
            throw new RuntimeException("Error processing LLM labels: " + labels, e);
        }
    }

    public BasicReport basicReport(RepositoryIndex repoIndex, DateRange dateRange) {
        Repository repository = github.repository(repoIndex.repo);
        String start = Instant.now().toString();
        String end = dateRange.fromString();
        Map<String, Label> labels = getLabels(repository, repoIndex);
        Tally discussions = tallyDiscussions(repository, labels, repoIndex, dateRange);
        Tally issues = tallyIssues(repository, labels, repoIndex, dateRange);
        Map<String, AtomicInteger> labelCounts = new HashMap<>();
        for (LabelReport labelReport : discussions.labelCounts()) {
            labelCounts.computeIfAbsent(labelReport.name(), k -> new AtomicInteger()).addAndGet(labelReport.count());
        }
        for (LabelReport labelReport : issues.labelCounts()) {
            labelCounts.computeIfAbsent(labelReport.name(), k -> new AtomicInteger()).addAndGet(labelReport.count());
        }
        List<LabelReport> labelReports = labelCounts.entrySet().stream()
                .map(entry -> new LabelReport(entry.getKey(), entry.getValue().get()))
                .collect(Collectors.toList());
        labelReports.sort(Comparator.comparingInt(LabelReport::count).reversed());
        return new BasicReport(start, end, labelReports, discussions, issues);

    }

    public Tally tallyDiscussions(Repository repository, Map<String, Label> labels, RepositoryIndex repoIndex,
            DateRange dateRange) {
        log.thinking("Tallying discussions...");
        Iterable<Discussion> discussions = repository.discussions().full(20);
        Map<String, AtomicInteger> labelCounts = new HashMap<>();
        long afterTime = dateRange.fromToday();
        int numDiscussions = 0;
        List<Discussion> unlabled = new ArrayList<>();
        for (Discussion discussion : discussions) {
            if (repoIndex.ignoredCategories.contains(discussion.category().name())) {
                continue;
            }
            numDiscussions++;
            String updatedAt = discussion.updatedAt();
            Instant instant = Instant.parse(updatedAt);
            // log.thinking(instant.toString());
            if (instant.toEpochMilli() < afterTime) {
                break;
            }
            if (discussion.labels().nodes().isEmpty()) {
                unlabled.add(discussion);

                continue;
            }
            for (LabelNameOnly label : discussion.labels().nodes()) {
                if (labels.containsKey(label.name())) {
                    labelCounts.computeIfAbsent(label.name(), k -> new AtomicInteger()).incrementAndGet();
                }
            }
        }
        log.thinking("Labelling unlabelled discussions...");
        for (Discussion discussion : unlabled) {
            Set<String> newLabels = processLLMLabels(labelBuilder.labelDiscussion(labels.values(), discussion));
            newLabels = processLLMLabels(newLabels);
            for (String label : newLabels) {
                labelCounts.computeIfAbsent(label, k -> new AtomicInteger()).incrementAndGet();
            }
        }
        List<LabelReport> labelReports = labelCounts.entrySet().stream()
                .map(entry -> new LabelReport(entry.getKey(), entry.getValue().get()))
                .collect(Collectors.toList());
        labelReports.sort(Comparator.comparingInt(LabelReport::count).reversed());
        return new Tally(numDiscussions, unlabled.size(), labelReports);
    }

    public Tally tallyIssues(Repository repository, Map<String, Label> labels, RepositoryIndex repoIndex, DateRange dateRange) {
        log.thinking("Tallying issues...");
        Iterable<Issues.Issue> issues = repository.issues().full(20,
                dateRange.fromString());
        Map<String, AtomicInteger> labelCounts = new HashMap<>();
        int numIssues = 0;
        List<Issues.Issue> unlabled = new ArrayList<>();
        for (Issues.Issue issue : issues) {
            numIssues++;
            if (issue.labels().nodes().isEmpty()) {
                unlabled.add(issue);
                continue;
            }
            for (LabelNameOnly label : issue.labels().nodes()) {
                if (labels.containsKey(label.name())) {
                    labelCounts.computeIfAbsent(label.name(), k -> new AtomicInteger()).incrementAndGet();
                }
            }
        }
        log.thinking("Labelling unlabelled issues...");
        for (Issues.Issue issue : unlabled) {
            Set<String> newLabels = processLLMLabels(labelBuilder.labelIssue(labels.values(), issue));
            newLabels = processLLMLabels(newLabels);
            for (String label : newLabels) {
                labelCounts.computeIfAbsent(label, k -> new AtomicInteger()).incrementAndGet();
            }
        }
        List<LabelReport> labelReports = labelCounts.entrySet().stream()
                .map(entry -> new LabelReport(entry.getKey(), entry.getValue().get()))
                .collect(Collectors.toList());
        labelReports.sort(Comparator.comparingInt(LabelReport::count).reversed());
        return new Tally(numIssues, unlabled.size(), labelReports);
    }

}
