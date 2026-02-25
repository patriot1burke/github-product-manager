package io.quarkiverse.github.index;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkiverse.github.api.Discussions.Discussion;
import io.quarkiverse.github.api.Github;
import io.quarkiverse.github.api.GithubAPI.Repository;
import io.quarkiverse.github.api.GithubConnection.IterableConnection;
import io.quarkiverse.github.api.Issues;
import io.quarkiverse.github.api.Labels.LabelNameOnly;
import io.quarkiverse.github.pm.util.AppLogger;

@ApplicationScoped
public class ReportService {
    static AppLogger log = AppLogger.getLogger(ReportService.class);

    @Inject
    Github github;

    public static record LabelReport(String name, int count) {

    }

    public static record Tally(int total, int unlabeled, List<LabelReport> labelCounts) {
    }

    public static record BasicReport(String startDate, String endDate, List<LabelReport> labelCounts, Tally discussions,
            Tally issues) {
    }

    public enum DateRange {

        LAST_30_DAYS(30),
        LAST_90_DAYS(90),
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

    public BasicReport basicReport(RepositoryIndex repoIndex, DateRange dateRange) {
        Repository repository = github.repository(repoIndex.repo);
        String start = Instant.now().toString();
        String end = dateRange.fromString();
        Tally discussions = tallyDiscussions(repository, repoIndex, dateRange);
        Tally issues = tallyIssues(repository, repoIndex, dateRange);
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

    private Tally tallyDiscussions(Repository repository, RepositoryIndex repoIndex, DateRange dateRange) {
        IterableConnection<Discussion> discussions = Repository.discussions(repository, 20);
        Map<String, AtomicInteger> labelCounts = new HashMap<>();
        long afterTime = dateRange.fromToday();
        int numDiscussions = 0;
        int unlabeledDiscussions = 0;
        for (Discussion discussion : discussions) {
            if (repoIndex.ignoredCategories.contains(discussion.category().name())) {
                continue;
            }
            numDiscussions++;
            String updatedAt = discussion.updatedAt();
            Instant instant = Instant.parse(updatedAt);
            //log.thinking(instant.toString());
            if (instant.toEpochMilli() < afterTime) {
                break;
            }
            if (discussion.labels().nodes().isEmpty()) {
                unlabeledDiscussions++;
                continue;
            }
            for (LabelNameOnly label : discussion.labels().nodes()) {
                labelCounts.computeIfAbsent(label.name(), k -> new AtomicInteger()).incrementAndGet();
            }
        }
        List<LabelReport> labelReports = labelCounts.entrySet().stream()
                .map(entry -> new LabelReport(entry.getKey(), entry.getValue().get()))
                .collect(Collectors.toList());
        labelReports.sort(Comparator.comparingInt(LabelReport::count).reversed());
        return new Tally(numDiscussions, unlabeledDiscussions, labelReports);
    }

    private Tally tallyIssues(Repository repository, RepositoryIndex repoIndex, DateRange dateRange) {
        IterableConnection<Issues.Issue> issues = Repository.issues(repository, 20, dateRange.fromString());
        Map<String, AtomicInteger> labelCounts = new HashMap<>();
        int numIssues = 0;
        int unlabeledIssues = 0;
        for (Issues.Issue issue : issues) {
            numIssues++;
            if (issue.labels().nodes().isEmpty()) {
                unlabeledIssues++;
                continue;
            }
            for (LabelNameOnly label : issue.labels().nodes()) {
                labelCounts.computeIfAbsent(label.name(), k -> new AtomicInteger()).incrementAndGet();
            }
        }
        List<LabelReport> labelReports = labelCounts.entrySet().stream()
                .map(entry -> new LabelReport(entry.getKey(), entry.getValue().get()))
                .collect(Collectors.toList());
        labelReports.sort(Comparator.comparingInt(LabelReport::count).reversed());
        return new Tally(numIssues, unlabeledIssues, labelReports);
    }

}
