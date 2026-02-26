package io.quarkiverse.test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import io.quarkiverse.github.api.Discussions.Discussion;
import io.quarkiverse.github.api.Github;
import io.quarkiverse.github.api.Labels.Label;
import io.quarkiverse.github.api.Labels.LabelNameOnly;
import io.quarkiverse.github.index.CalculateLabelsPrompt;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class PromptTest {

    @Inject
    CalculateLabelsPrompt calculateLabelsPrompt;

    @Inject
    Github github;

    //@Test
    public void testCalculateLabelsPrompt() {

        Iterable<Label> labelsIterable = github.repository("quarkusio/quarkus").labels();
        List<Label> labels = new ArrayList<>();
        for (Label label : labelsIterable) {
            labels.add(label);
        }
        Set<String> ignoredCategories = Set.of("Announcements", "Events", "Introductiosn", "Jobs and Opportunities", "Polls",
                "Quarkus Blog/Website");
        Iterator<Discussion> discussions = github.repository("quarkusio/quarkus").discussions().full(10).iterator();
        for (int i = 0; i < 10; i++) {
            Discussion discussion = discussions.next();
            if (ignoredCategories.contains(discussion.category().name())) {
                continue;
            }
            String actual = discussion.labels().nodes().stream().map(LabelNameOnly::name).collect(Collectors.joining(", "));
            Set<String> prompt = calculateLabelsPrompt.prompt(labels, discussion);
            System.out.println("-------" + discussion.title() + "-------");
            System.out.println("Actual: " + actual);
            System.out.println("Prompt: " + prompt.stream().collect(Collectors.joining(", ")));
            System.out.println(prompt.stream().collect(Collectors.joining(", ")));

        }
    }

}
