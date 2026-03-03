package io.quarkiverse.github.index.model;

import java.util.List;

public record DiscussionCommentModel(String author, String body, List<CommentModel> replies) {

}
