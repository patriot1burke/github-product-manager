package io.quarkiverse.ai.github.api;

import java.util.HashMap;
import java.util.Map;

import jakarta.annotation.Nullable;

import io.quarkiverse.ai.github.api.Discussions.DiscussionCategoryConnection;
import io.quarkiverse.ai.github.api.Discussions.DiscussionConnection;
import io.quarkiverse.ai.github.api.GithubConnection.IterableConnection;
import io.quarkiverse.ai.github.api.Issues.IssueConnection;
import io.quarkiverse.ai.github.api.Issues.IssueConnectionForBasicReport;
import io.quarkiverse.ai.github.api.Labels.LabelConnection;
import io.quarkiverse.graphql.client.ArgsOnly;
import io.quarkiverse.graphql.client.DefaultVariables;
import io.quarkiverse.graphql.client.Query;

public interface GithubAPI {

    Repository repository(String owner, String name);

    public interface Repository {

        @Query
        LabelConnection labels(int first, @Nullable String after);

        default Map<String, Labels.Label> labels() {
            Iterable<Labels.Label> labelsIterable = new IterableConnection<Labels.Label>((after) -> labels(100, after));
            Map<String, Labels.Label> labels = new HashMap<>();
            for (Labels.Label label : labelsIterable) {
                labels.put(label.name(), label);
            }
            return labels;
        }

        @Query
        DiscussionCategoryConnection discussionCategories(int first, @Nullable String after);

        default Map<String, Discussions.DiscussionCategory> discussionCategories() {
            Iterable<Discussions.DiscussionCategory> discussionCategoriesIterable = new IterableConnection<Discussions.DiscussionCategory>(
                    (after) -> discussionCategories(100, after));
            Map<String, Discussions.DiscussionCategory> discussionCategories = new HashMap<>();
            for (Discussions.DiscussionCategory discussionCategory : discussionCategoriesIterable) {
                discussionCategories.put(discussionCategory.name(), discussionCategory);
            }
            return discussionCategories;
        }

        interface TailoredDiscussions {
            @Query
            @ArgsOnly
            @DefaultVariables({
                    ".nodes.labels.first: 100",
                    ".nodes.comments.first: 20",
                    ".nodes.comments.nodes.replies.first: 20"
            })
            DiscussionConnection full(int first, @Nullable String after);

            default Iterable<Discussions.Discussion> full(int pageSize) {
                return new IterableConnection<Discussions.Discussion>((after) -> full(pageSize, after));
            }
        }

        @DefaultVariables("orderBy: {field: UPDATED_AT, direction: DESC}")
        TailoredDiscussions discussions();

        interface TailoredIssues {
            @Query
            @ArgsOnly
            @DefaultVariables({
                    ".nodes.labels.first: 100",
                    ".nodes.comments.first: 20"
            })
            IssueConnection full(int first, @Nullable Issues.Since filterBy, @Nullable String after);

            default Iterable<Issues.Issue> full(int pageSize, String since) {
                Issues.Since filterBy = since != null ? new Issues.Since(since) : null;
                return new IterableConnection<Issues.Issue>(
                        (after) -> full(pageSize, filterBy, after));
            }

            default Iterable<Issues.Issue> full(int pageSize) {
                return new IterableConnection<Issues.Issue>(
                        (after) -> full(pageSize, null, after));
            }

            @Query
            @ArgsOnly
            @DefaultVariables(".nodes.labels.first: 100")
            IssueConnectionForBasicReport basicReport(int first, Issues.Since filterBy, @Nullable String after);

            default Iterable<Issues.IssueForBasicReport> basicReport(int pageSize, String since) {
                Issues.Since filterBy = since != null ? new Issues.Since(since) : null;
                return new IterableConnection<Issues.IssueForBasicReport>(
                        (after) -> basicReport(pageSize, filterBy, after));
            }
        }

        @DefaultVariables("orderBy: {field: UPDATED_AT, direction: DESC}")
        TailoredIssues issues();
    }
}
