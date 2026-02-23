package io.quarkiverse.github.api;

import java.util.List;

import io.quarkiverse.github.api.Labels.LabelConnectionIdOnly;

public interface Discussions {

    public record DiscussionCategoryIdOnly(String id) {

    }

    public record DiscussionCategory(String id, String name, String description, boolean isAnswerable) {

    }

    public record DiscussionCategoryConnection(PageInfo pageInfo, List<DiscussionCategory> nodes) {
    }

    public record DiscussionCommentConnection(PageInfo pageInfo, List<Comment> nodes) {
    }

    public record Discussion(int number, String title, Actor author, DiscussionCategoryIdOnly category,
            String body, String createdAt,
            String updatedAt, LabelConnectionIdOnly labels, DiscussionCommentConnection comments) {
    }

    public record DiscussionIdOnly(String id, String updatedAt) {

    }

    public record DiscussionConnection(PageInfo pageInfo, List<Discussion> nodes) {
    }

    public record DiscussionConnectionIdOnly(PageInfo pageInfo, List<DiscussionIdOnly> nodes) {
    }

}
