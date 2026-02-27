package io.quarkiverse.github.api;

import java.util.List;

import io.quarkiverse.github.api.Labels.LabelConnectionNameOnly;
import io.quarkiverse.graphql.client.InputType;

public interface Issues {

    record IssueType(String name, String description) {
    }

    record IssueTypeNameOnly(String name) {
    }

    public record IssueComment(Actor author, String body) {

    }

    public record IssueCommentConnection(PageInfo pageInfo,
            List<IssueComment> nodes) implements GithubConnection<IssueComment> {
    }

    @InputType("IssueFilters")
    record Since(String since) {

    }

    record Issue(int number, String title, Actor author, IssueTypeNameOnly issueType, String body, String createdAt,
            String updatedAt, LabelConnectionNameOnly labels, IssueCommentConnection comments) {

    }

    public record IssueConnection(PageInfo pageInfo, List<Issue> nodes) implements GithubConnection<Issue> {
    }

    record IssueForBasicReport(IssueTypeNameOnly issueType, String createdAt,
            String updatedAt, LabelConnectionNameOnly labels) {

    }

    public record IssueConnectionForBasicReport(PageInfo pageInfo,
            List<IssueForBasicReport> nodes) implements GithubConnection<IssueForBasicReport> {
    }
}
