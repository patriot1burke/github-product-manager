package io.quarkiverse.github.api;

import java.util.List;

public interface Comments {

    public record Reply(Actor author, String body) {

    }

    public record ReplyConnection(PageInfo pageInfo, List<Reply> nodes) implements GithubConnection<Reply> {
    }

    public record Comment(Actor author, String body, ReplyConnection replies) {

    }

    public record CommentConnection(PageInfo pageInfo, List<Comment> nodes) implements GithubConnection<Comment> {
    }

}
