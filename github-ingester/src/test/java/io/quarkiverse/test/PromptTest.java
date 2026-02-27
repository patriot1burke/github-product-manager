package io.quarkiverse.test;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import io.quarkiverse.github.api.Discussions.Discussion;
import io.quarkiverse.github.api.Github;
import io.quarkiverse.github.api.Issues.Issue;
import io.quarkiverse.github.api.Labels.Label;
import io.quarkiverse.github.api.Labels.LabelNameOnly;
import io.quarkiverse.github.index.CalculateLabelsPrompt;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@EnabledIfSystemProperty(named = "test.prompt", matches = "true")
public class PromptTest {

    @Inject
    CalculateLabelsPrompt calculateLabelsPrompt;

    @Inject
    Github github;

    @Test
    public void testDiscussionLabelsPrompt() {

        Map<String, Label> labels = github.repository("quarkusio/quarkus").labels();
        Set<String> ignoredCategories = Set.of("Announcements", "Events", "Introductiosn", "Jobs and Opportunities", "Polls",
                "Quarkus Blog/Website");
        Iterator<Discussion> discussions = github.repository("quarkusio/quarkus").discussions().full(10).iterator();
        for (int i = 0; i < 10; i++) {
            Discussion discussion = discussions.next();
            if (ignoredCategories.contains(discussion.category().name())) {
                continue;
            }
            String actual = discussion.labels().nodes().stream().map(LabelNameOnly::name).collect(Collectors.joining(", "));
            Set<String> prompt = calculateLabelsPrompt.discussionLabels(labels.values(), discussion);
            System.out.println("-------" + discussion.title() + "-------");
            System.out.println("Actual: " + actual);
            System.out.println("Prompt: " + prompt.stream().collect(Collectors.joining(", ")));
            System.out.println(prompt.stream().collect(Collectors.joining(", ")));

        }
    }

    @Test
    public void testIssueLabelsPrompt() {

        Map<String, Label> labels = github.repository("quarkusio/quarkus").labels();
        Iterator<Issue> issues = github.repository("quarkusio/quarkus").issues().full(10).iterator();
        for (int i = 0; i < 10; i++) {
            Issue issue = issues.next();
            String actual = issue.labels().nodes().stream().map(LabelNameOnly::name).collect(Collectors.joining(", "));
            Set<String> prompt = calculateLabelsPrompt.issueLabels(labels.values(), issue);
            System.out.println("-------" + issue.title() + "-------");
            System.out.println("Actual: " + actual);
            System.out.println("Prompt: " + prompt.stream().collect(Collectors.joining(", ")));
            System.out.println(prompt.stream().collect(Collectors.joining(", ")));

        }
    }

}
