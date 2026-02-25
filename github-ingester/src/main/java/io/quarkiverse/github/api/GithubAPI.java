package io.quarkiverse.github.api;

import io.quarkiverse.github.api.Comments.CommentConnection;
import io.quarkiverse.github.api.Discussions.DiscussionCategoryConnection;
import io.quarkiverse.github.api.Discussions.DiscussionConnection;
import io.quarkiverse.github.api.GithubConnection.IterableConnection;
import io.quarkiverse.github.api.Issues.IssueConnection;
import io.quarkiverse.github.api.Labels.LabelConnection;
import io.quarkiverse.github.api.Labels.LabelConnectionNameOnly;
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

        @Query
        @DefaultVariables({
                @DefaultVariable(name = "orderBy", value = "{field: UPDATED_AT, direction: DESC}"),
                @DefaultVariable(namespace = ".nodes.labels", name = "first", value = "100"),
                @DefaultVariable(namespace = ".nodes.comments", name = "first", value = "20")
        })
        DiscussionConnection discussions(int first);

        @Query
        @DefaultVariables({
                @DefaultVariable(name = "orderBy", value = "{field: UPDATED_AT, direction: DESC}"),
                @DefaultVariable(namespace = ".nodes.labels", name = "first", value = "100"),
                @DefaultVariable(namespace = ".nodes.comments", name = "first", value = "20")
        })
        DiscussionConnection discussions(int first, String after);

        static IterableConnection<Discussions.Discussion> discussions(Repository repo, int pageSize) {
            return new IterableConnection<Discussions.Discussion>(repo::discussions, repo::discussions, pageSize);
        }

        Discussion discussion(int number);

        @Query
        @DefaultVariables({
                @DefaultVariable(name = "orderBy", value = "{field: UPDATED_AT, direction: DESC}"),
                @DefaultVariable(namespace = ".nodes.labels", name = "first", value = "100"),
                @DefaultVariable(namespace = ".nodes.comments", name = "first", value = "20")
        })
        IssueConnection issues(int first, Issues.Since filterBy);

        @Query
        @DefaultVariables({
                @DefaultVariable(name = "orderBy", value = "{field: UPDATED_AT, direction: DESC}"),
                @DefaultVariable(namespace = ".nodes.labels", name = "first", value = "100"),
                @DefaultVariable(namespace = ".nodes.comments", name = "first", value = "20")
        })
        IssueConnection issues(int first, Issues.Since filterBy, String after);

        static IterableConnection<Issues.Issue> issues(Repository repo, int pageSize, String since) {
            return new IterableConnection<Issues.Issue>((first) -> repo.issues(first, new Issues.Since(since)),
                    (first, after) -> repo.issues(first, new Issues.Since(since), after), pageSize);
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
