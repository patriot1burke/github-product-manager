package io.quarkiverse.github.index.model;

import java.util.List;

public record RecordSet(List<DiscussionModel> discussions, List<IssueModel> issues) {

}