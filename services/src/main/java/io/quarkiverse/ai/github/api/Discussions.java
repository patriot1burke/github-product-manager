package io.quarkiverse.ai.github.api;

import java.util.List;

import io.quarkiverse.ai.github.api.Labels.LabelConnectionNameOnly;

public interface Discussions {

    public record Reply(Actor author, String body) {

    }

    public record ReplyConnection(PageInfo pageInfo, List<Reply> nodes) implements GithubConnection<Reply> {
    }

    public record DiscussionComment(Actor author, String body, ReplyConnection replies) {

    }

    public record DiscussionCommentConnection(PageInfo pageInfo,
            List<DiscussionComment> nodes) implements GithubConnection<DiscussionComment> {
    }

    public record DiscussionCategoryNameOnly(String name) {

    }

    public record DiscussionCategory(String name, String description, boolean isAnswerable) {

    }

    public record DiscussionCategoryConnection(PageInfo pageInfo,
            List<DiscussionCategory> nodes) implements GithubConnection<DiscussionCategory> {
    }

    public record Discussion(int number, String title, Actor author, DiscussionCategoryNameOnly category,
            String body, boolean closed, String closedAt, String createdAt,
            String updatedAt, LabelConnectionNameOnly labels, DiscussionCommentConnection comments) {
    }

    public record DiscussionConnection(PageInfo pageInfo, List<Discussion> nodes)
            implements
                GithubConnection<Discussion> {
    }
}
