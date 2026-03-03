package io.quarkiverse.github.index;

import java.util.Collection;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;

import io.quarkiverse.github.api.Discussions.Discussion;
import io.quarkiverse.github.api.Issues.Issue;
import io.quarkiverse.github.api.Labels.Label;

@ApplicationScoped
@ActivateRequestContext
public class PromptWrapper {

    @Inject
    DetermineLabelsPrompt determineLabelsPrompt;

    @Inject
    SummaryPrompt summaryPrompt;

    public Set<String> labelDiscussion(Collection<Label> labels, Discussion discussion) {
        return determineLabelsPrompt.labelDiscussion(labels, discussion);
    }

    public Set<String> labelIssue(Collection<Label> labels, Issue issue) {
        return determineLabelsPrompt.labelIssue(labels, issue);
    }

    public String summarize(String issue) {
        return summaryPrompt.summarize(issue);
    }
}
