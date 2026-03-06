package io.quarkiverse.github.index.model;

import java.util.List;
import java.util.Set;

public record IssueModel(String repo, int number, String title, String author, String body, long createdAt, long updatedAt,
        String issueType,
        Set<String> labels, List<CommentModel> comments) {

}
