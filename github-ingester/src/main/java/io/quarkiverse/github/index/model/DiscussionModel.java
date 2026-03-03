package io.quarkiverse.github.index.model;

import java.util.List;
import java.util.Set;

public record DiscussionModel(int number, String title, String author, String body, long createdAt, long updatedAt,
        String category, Set<String> labels, List<DiscussionCommentModel> comments) {

}
