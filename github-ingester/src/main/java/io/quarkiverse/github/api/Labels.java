package io.quarkiverse.github.api;

import java.util.List;

public interface Labels {
    public record Label(String name, String description) {

    }

    public record LabelNameOnly(String name) {

    }

    public record LabelConnection(PageInfo pageInfo, List<Label> nodes) implements GithubConnection<Label> {
    }

    public record LabelConnectionNameOnly(PageInfo pageInfo,
            List<LabelNameOnly> nodes) implements GithubConnection<LabelNameOnly> {
    }

}
