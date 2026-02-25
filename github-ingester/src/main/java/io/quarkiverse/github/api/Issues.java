package io.quarkiverse.github.api;

import java.util.List;

import io.quarkiverse.github.api.Comments.CommentConnection;
import io.quarkiverse.github.api.Labels.LabelConnectionNameOnly;
import io.quarkiverse.graphql.client.InputType;

public interface Issues {

    record IssueType(String name, String description) {
    }

    record IssueTypeNameOnly(String name) {
    }

    @InputType("IssueFilters!")
    record Since(String since) {

    }

    record Issue(int number, String title, Actor author, IssueTypeNameOnly issueType, String body, String createdAt,
            String updatedAt, LabelConnectionNameOnly labels, CommentConnection comments) {

    }

    public record IssueConnection(PageInfo pageInfo, List<Issue> nodes) implements GithubConnection<Issue> {
    }
}
