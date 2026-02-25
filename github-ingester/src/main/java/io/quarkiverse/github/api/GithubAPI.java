package io.quarkiverse.github.api;

import io.quarkiverse.github.api.Comments.CommentConnection;
import io.quarkiverse.github.api.Discussions.DiscussionCategoryConnection;
import io.quarkiverse.github.api.Discussions.DiscussionConnection;
import io.quarkiverse.github.api.Discussions.DiscussionConnectionForBasicReport;
import io.quarkiverse.github.api.GithubConnection.IterableConnection;
import io.quarkiverse.github.api.Issues.IssueConnection;
import io.quarkiverse.github.api.Issues.IssueConnectionForBasicReport;
import io.quarkiverse.github.api.Labels.LabelConnection;
import io.quarkiverse.github.api.Labels.LabelConnectionNameOnly;
import io.quarkiverse.graphql.client.ArgsOnly;
import io.quarkiverse.graphql.client.DefaultVariable;
import io.quarkiverse.graphql.client.DefaultVariables;
import io.quarkiverse.graphql.client.Query;

public interface GithubAPI {

    Repository repository(String owner, String name);

    public interface Repository {

        @Query
        LabelConnection labels(int first);

        @Query
        LabelConnection labels(int first, String after);

        @Query
        DiscussionCategoryConnection discussionCategories(int first);

        @Query
        DiscussionCategoryConnection discussionCategories(int first, String after);

        interface TailoredDiscussions {
            @Query
            @ArgsOnly
            @DefaultVariables({
                    @DefaultVariable(namespace = ".nodes.labels", name = "first", value = "100"),
                    @DefaultVariable(namespace = ".nodes.comments", name = "first", value = "20")
            })
            DiscussionConnection full(int first);

            @Query
            @ArgsOnly
            @DefaultVariables({
                    @DefaultVariable(namespace = ".nodes.labels", name = "first", value = "100"),
                    @DefaultVariable(namespace = ".nodes.comments", name = "first", value = "20")
            })
            DiscussionConnection full(int first, String after);

            @Query
            @ArgsOnly
            @DefaultVariable(namespace = ".nodes.labels", name = "first", value = "100")
            DiscussionConnectionForBasicReport basicReport(int first);

            @Query
            @ArgsOnly
            @DefaultVariable(namespace = ".nodes.labels", name = "first", value = "100")
            DiscussionConnectionForBasicReport basicReport(int first, String after);

        }

        @DefaultVariable(name = "orderBy", value = "{field: UPDATED_AT, direction: DESC}")
        TailoredDiscussions discussions();

        static IterableConnection<Discussions.Discussion> fullDiscussions(Repository repo, int pageSize) {
            return new IterableConnection<Discussions.Discussion>(() -> repo.discussions().full(pageSize),
                    (after) -> repo.discussions().full(pageSize, after));
        }

        static IterableConnection<Discussions.DiscussionForBasicReport> basicReportDiscussions(Repository repo, int pageSize) {
            return new IterableConnection<Discussions.DiscussionForBasicReport>(() -> repo.discussions().basicReport(pageSize),
                    (after) -> repo.discussions().basicReport(pageSize, after));
        }

        Discussion discussion(int number);

        interface TailoredIssues {
            @Query
            @ArgsOnly
            @DefaultVariables({
                    @DefaultVariable(namespace = ".nodes.labels", name = "first", value = "100"),
                    @DefaultVariable(namespace = ".nodes.comments", name = "first", value = "20")
            })
            IssueConnection full(int first, Issues.Since filterBy);

            @Query
            @ArgsOnly
            @DefaultVariables({
                    @DefaultVariable(namespace = ".nodes.labels", name = "first", value = "100"),
                    @DefaultVariable(namespace = ".nodes.comments", name = "first", value = "20")
            })
            IssueConnection full(int first, Issues.Since filterBy, String after);

            @Query
            @ArgsOnly
            @DefaultVariable(namespace = ".nodes.labels", name = "first", value = "100")
            IssueConnectionForBasicReport basicReport(int first, Issues.Since filterBy);

            @Query
            @ArgsOnly
            @DefaultVariable(namespace = ".nodes.labels", name = "first", value = "100")
            IssueConnectionForBasicReport basicReport(int first, Issues.Since filterBy, String after);
        }

        @DefaultVariable(name = "orderBy", value = "{field: UPDATED_AT, direction: DESC}")
        TailoredIssues issues();

        static IterableConnection<Issues.Issue> fullIssues(Repository repo, int pageSize, String since) {
            return new IterableConnection<Issues.Issue>(() -> repo.issues().full(pageSize, new Issues.Since(since)),
                    (after) -> repo.issues().full(pageSize, new Issues.Since(since), after));
        }

        static IterableConnection<Issues.IssueForBasicReport> basicReportIssues(Repository repo, int pageSize, String since) {
            return new IterableConnection<Issues.IssueForBasicReport>(
                    () -> repo.issues().basicReport(pageSize, new Issues.Since(since)),
                    (after) -> repo.issues().basicReport(pageSize, new Issues.Since(since), after));
        }
    }

    public interface Discussion {
        @Query
        CommentConnection comments(int first);

        @Query
        CommentConnection comments(int first, String after);

        @Query
        LabelConnectionNameOnly labels(int first);

        @Query
        LabelConnectionNameOnly labels(int first, String after);
    }

}
