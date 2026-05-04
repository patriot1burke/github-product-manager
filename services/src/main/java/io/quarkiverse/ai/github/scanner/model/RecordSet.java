package io.quarkiverse.ai.github.scanner.model;

import java.util.List;

public record RecordSet(List<DiscussionModel> discussions, List<IssueModel> issues) {

}