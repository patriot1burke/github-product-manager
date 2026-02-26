package io.quarkiverse.github.api;

import jakarta.annotation.Nullable;

import io.quarkiverse.github.api.Discussions.DiscussionCategoryConnection;
import io.quarkiverse.github.api.Discussions.DiscussionConnection;
import io.quarkiverse.github.api.Discussions.DiscussionConnectionForBasicReport;
import io.quarkiverse.github.api.GithubConnection.IterableConnection;
import io.quarkiverse.github.api.Issues.IssueConnection;
import io.quarkiverse.github.api.Issues.IssueConnectionForBasicReport;
import io.quarkiverse.github.api.Labels.LabelConnection;
import io.quarkiverse.graphql.client.ArgsOnly;
import io.quarkiverse.graphql.client.DefaultVariables;
import io.quarkiverse.graphql.client.Query;

public interface GithubAPI {

    Repository repository(String owner, String name);

    public interface Repository {

        @Query
        LabelConnection labels(int first, @Nullable String after);

        default IterableConnection<Labels.Label> labels() {
            return new IterableConnection<Labels.Label>((after) -> labels(100, after));
        }

        @Query
        DiscussionCategoryConnection discussionCategories(int first, @Nullable String after);

        default IterableConnection<Discussions.DiscussionCategory> discussionCategories() {
            return new IterableConnection<Discussions.DiscussionCategory>(
                    (after) -> discussionCategories(100, after));
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

            default IterableConnection<Discussions.Discussion> full(int pageSize) {
                return new IterableConnection<Discussions.Discussion>((after) -> full(pageSize, after));
            }

            @Query
            @ArgsOnly
            @DefaultVariables(".nodes.labels.first: 100")
            DiscussionConnectionForBasicReport basicReport(int first, @Nullable String after);

            default IterableConnection<Discussions.DiscussionForBasicReport> basicReport(int pageSize) {
                return new IterableConnection<Discussions.DiscussionForBasicReport>(
                        (after) -> basicReport(pageSize, after));
            }

        }

        @DefaultVariables("orderBy: {field: UPDATED_AT, direction: DESC}")
        TailoredDiscussions discussions();

        interface TailoredIssues {
            @Query
            @ArgsOnly
            @DefaultVariables({
                    ".nodes.labels.first: 100",
                    ".nodes.comments.first: 20",
                    ".nodes.comments.nodes.replies.first: 20"
            })
            IssueConnection full(int first, Issues.Since filterBy, @Nullable String after);

            default IterableConnection<Issues.Issue> full(int pageSize, String since) {
                return new IterableConnection<Issues.Issue>(
                        (after) -> full(pageSize, new Issues.Since(since), after));
            }

            @Query
            @ArgsOnly
            @DefaultVariables(".nodes.labels.first: 100")
            IssueConnectionForBasicReport basicReport(int first, Issues.Since filterBy, @Nullable String after);

            default IterableConnection<Issues.IssueForBasicReport> basicReport(int pageSize, String since) {
                return new IterableConnection<Issues.IssueForBasicReport>(
                        (after) -> basicReport(pageSize, new Issues.Since(since), after));
            }
        }

        @DefaultVariables("orderBy: {field: UPDATED_AT, direction: DESC}")
        TailoredIssues issues();
    }
}
