package io.quarkiverse.github.api;

import java.util.List;

public interface Labels {
    public record Label(String id, String name, String description) {

    }

    public record LabelIdOnly(String id) {

    }

    public record LabelConnection(PageInfo pageInfo, List<Label> nodes) {
    }

    public record LabelConnectionIdOnly(PageInfo pageInfo, List<LabelIdOnly> nodes) {
    }

}
