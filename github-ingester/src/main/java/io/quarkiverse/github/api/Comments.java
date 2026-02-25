package io.quarkiverse.github.api;

import java.util.List;

public interface Comments {
    public record Comment(Actor author, String body) {

    }

    public record CommentConnection(PageInfo pageInfo, List<Comment> nodes) implements GithubConnection<Comment> {
    }

}
