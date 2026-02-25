package io.quarkiverse.github.api;

import java.util.List;

import io.quarkiverse.github.api.Comments.CommentConnection;
import io.quarkiverse.github.api.Labels.LabelConnectionNameOnly;

public interface Discussions {

    public record DiscussionCategoryNameOnly(String name) {

    }

    public record DiscussionCategory(String id, String name, String description, boolean isAnswerable) {

    }

    public record DiscussionCategoryConnection(PageInfo pageInfo,
            List<DiscussionCategory> nodes) implements GithubConnection<DiscussionCategory> {
    }

    public record Discussion(int number, String title, Actor author, DiscussionCategoryNameOnly category,
            String body, String createdAt,
            String updatedAt, LabelConnectionNameOnly labels, CommentConnection comments) {
    }

    public record DiscussionConnection(PageInfo pageInfo, List<Discussion> nodes) implements GithubConnection<Discussion> {
    }
}
