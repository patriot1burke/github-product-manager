package io.quarkiverse.ai.github.scanner.model;

import java.util.List;
import java.util.Set;

public record DiscussionModel(String repo, int number, String title, String author, String body, boolean closed, long closedAt,
        long createdAt, long updatedAt,
        String category, Set<String> labels, List<DiscussionCommentModel> comments) {

}
