package io.quarkiverse.ai.github.scanner;

public interface GithubMetadata {
    public static final String REPOSITORY = "repo";
    public static final String ID = "number";
    public static final String TYPE = "type";
    public static final String UPDATED_AT = "updatedAt";
    public static final String CREATED_AT = "createdAt";
    public static final String AUTHOR = "author";
    public static final String CLOSED = "closed";
    public static final String CLOSED_AT = "closedAt";

    public static String label(String name) {
        return "label_" + name;
    }
}
