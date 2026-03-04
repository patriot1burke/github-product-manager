package io.quarkiverse.github.index;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkiverse.github.api.GithubAPI.Repository;
import io.quarkiverse.github.api.Labels.Label;
import io.quarkiverse.github.index.model.DiscussionModel;
import io.quarkiverse.github.index.model.IssueModel;
import io.quarkiverse.github.util.AppLogger;

@ApplicationScoped
public class ReportService {
    static AppLogger log = AppLogger.getLogger(ReportService.class);

    @Inject
    PullCacheService pullCacheService;

    public static record LabelReport(String name, int count) {

    }

    public static record Tally(int total, List<LabelReport> labelCounts) {
    }

    public static record BasicReport(String startDate, String endDate, List<LabelReport> labelCounts, Tally discussions,
            Tally issues) {
    }

    public enum DateRange {

        month(30),
        quarter(90),
        year(365)

        ;

        int days;

        DateRange(int days) {
            this.days = days;
        }

        public Instant fromInstant() {
            return Instant.now().minus(days, ChronoUnit.DAYS);
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

    private Map<String, Label> getLabels(Repository repository, RepositoryConfig repoIndex) {
        return repository.labels().entrySet().stream()
                .filter(entry -> !repoIndex.ignoreLabel(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
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

    public BasicReport basicReport(String repoName) {
        PullCache pullCache = pullCacheService.load(repoName);
        long newest = 0;
        long oldest = 0;

        for (DiscussionModel discussion : pullCache.discussions.values()) {
            if (discussion.updatedAt() > newest) {
                newest = discussion.updatedAt();
            }
            if (oldest == 0 || discussion.updatedAt() < oldest) {
                oldest = discussion.updatedAt();
            }
        }
        for (IssueModel issue : pullCache.issues.values()) {
            if (issue.updatedAt() > newest) {
                newest = issue.updatedAt();
            }
            if (oldest == 0 || issue.updatedAt() < oldest) {
                oldest = issue.updatedAt();
            }
        }
        String start = Instant.ofEpochMilli(oldest).toString();
        String end = Instant.ofEpochMilli(newest).toString();
        Tally discussions = tallyDiscussions(repoName);
        Tally issues = tallyIssues(repoName);
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

    public Tally tallyDiscussions(String repoName) {
        log.thinking("Tallying discussions...");
        PullCache pullCache = pullCacheService.load(repoName);
        Map<String, AtomicInteger> labelCounts = new HashMap<>();
        int numDiscussions = 0;
        for (DiscussionModel discussion : pullCache.discussions.values()) {
            numDiscussions++;
            for (String label : discussion.labels()) {
                labelCounts.computeIfAbsent(label, k -> new AtomicInteger()).incrementAndGet();
            }
        }
        List<LabelReport> labelReports = labelCounts.entrySet().stream()
                .map(entry -> new LabelReport(entry.getKey(), entry.getValue().get()))
                .collect(Collectors.toList());
        labelReports.sort(Comparator.comparingInt(LabelReport::count).reversed());
        return new Tally(numDiscussions, labelReports);
    }

    public Tally tallyIssues(String repoName) {
        log.thinking("Tallying issues...");
        PullCache pullCache = pullCacheService.load(repoName);
        Map<String, AtomicInteger> labelCounts = new HashMap<>();
        int numIssues = 0;
        for (IssueModel issue : pullCache.issues.values()) {
            numIssues++;
            for (String label : issue.labels()) {
                labelCounts.computeIfAbsent(label, k -> new AtomicInteger()).incrementAndGet();
            }
        }
        List<LabelReport> labelReports = labelCounts.entrySet().stream()
                .map(entry -> new LabelReport(entry.getKey(), entry.getValue().get()))
                .collect(Collectors.toList());
        labelReports.sort(Comparator.comparingInt(LabelReport::count).reversed());
        return new Tally(numIssues, labelReports);
    }

    @Inject
    SummaryService summaryService;

    @Inject
    PromptWrapper summaryPrompt;

    public String summarizeLabled(String repoName, List<String> labels) {
        PullCache pullCache = pullCacheService.load(repoName);
        List<String> summaries = new ArrayList<>();
        for (IssueModel issue : pullCache.issues.values()) {
            if (issue.labels().containsAll(labels)) {
                summaries.add(summaryService.summarize(repoName, issue));
            }
        }
        for (DiscussionModel discussion : pullCache.discussions.values()) {
            if (discussion.labels().containsAll(labels)) {
                summaries.add(summaryService.summarize(repoName, discussion));
            }
        }
        if (summaries.isEmpty()) {
            return "Nothing to summarize";
        }
        if (summaries.size() == 1) {
            return summaries.get(0);
        }
        log.thinking("Summarizing " + summaries.size() + " summaries");
        String cat = String.join("\n\n", summaries);
        return summaryPrompt.summarize(cat);
    }

}
