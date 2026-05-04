package io.quarkiverse.ai.github.scanner.model;

import java.util.List;

public record DiscussionCommentModel(String author, String body, List<CommentModel> replies) {

}
