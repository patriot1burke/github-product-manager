package io.quarkiverse.github.api;

import io.quarkiverse.github.api.Discussions.DiscussionCategoryConnection;
import io.quarkiverse.github.api.Discussions.DiscussionCommentConnection;
import io.quarkiverse.github.api.Discussions.DiscussionConnection;
import io.quarkiverse.github.api.Labels.LabelConnection;
import io.quarkiverse.github.api.Labels.LabelConnectionIdOnly;
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

        Discussion discussion(int number);

    }

    public interface Discussion {
        @Query
        DiscussionCommentConnection comments(int first);

        @Query
        DiscussionCommentConnection comments(int first, String after);

        @Query
        LabelConnectionIdOnly labels(int first);

        @Query
        LabelConnectionIdOnly labels(int first, String after);
    }

}
