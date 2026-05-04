package io.quarkiverse.test;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import io.quarkiverse.ai.github.api.Discussions.Discussion;
import io.quarkiverse.ai.github.api.Github;
import io.quarkiverse.ai.github.api.Issues.Issue;
import io.quarkiverse.ai.github.api.Labels.Label;
import io.quarkiverse.ai.github.api.Labels.LabelNameOnly;
import io.quarkiverse.ai.github.scanner.DetermineLabelsPrompt;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@EnabledIfSystemProperty(named = "test.prompt", matches = "true")
public class PromptTest {
    @Inject
    DetermineLabelsPrompt prompt;

    @Inject
    Github github;

    @Test
    public void testDiscussionLabelsPrompt() {

        Map<String, Label> labels = github.repository("quarkusio/quarkus").labels();
        Set<String> ignoredCategories = Set.of("Announcements", "Events", "Introductiosn", "Jobs and Opportunities", "Polls",
                "Quarkus Blog/Website");
        Iterator<Discussion> discussions = github.repository("quarkusio/quarkus").discussions().full(10).iterator();
        int NUM = 2;
        int count = 0;
        for (int i = 0; i < 100 && count < NUM; i++) {
            Discussion discussion = discussions.next();
            if (ignoredCategories.contains(discussion.category().name())) {
                continue;
            }
            count++;
            String actual = discussion.labels().nodes().stream().map(LabelNameOnly::name).collect(Collectors.joining(", "));
            Set<String> promptLabels = prompt.labelDiscussion(labels.values(), discussion);
            System.out.println("-------" + discussion.title() + "-------");
            System.out.println("Actual: " + actual);
            System.out.println("Prompt: " + promptLabels.size());
            for (String label : promptLabels) {
                System.out.println("  '" + label + "'");
            }

        }
    }

    @Test
    public void testIssueLabelsPrompt() {

        Map<String, Label> labels = github.repository("quarkusio/quarkus").labels();
        Iterator<Issue> issues = github.repository("quarkusio/quarkus").issues().full(10).iterator();
        for (int i = 0; i < 1; i++) {
            Issue issue = issues.next();
            String actual = issue.labels().nodes().stream().map(LabelNameOnly::name).collect(Collectors.joining(", "));
            Set<String> promptLabels = prompt.labelIssue(labels.values(), issue);
            System.out.println("-------" + issue.title() + "-------");
            System.out.println("Actual: " + actual);
            System.out.println("Prompt: " + promptLabels.size());
            for (String label : promptLabels) {
                System.out.println("  '" + label + "'");
            }

        }
    }

}
