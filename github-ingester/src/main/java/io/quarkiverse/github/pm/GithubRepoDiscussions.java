package io.quarkiverse.github.pm;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkiverse.github.pm.util.AppLogger;
import io.quarkiverse.graphql.client.GraphQL;
import io.quarkiverse.graphql.client.NestedValue;
import io.quarkiverse.graphql.client.Query;

@ApplicationScoped
public class GithubRepoDiscussions {
    static AppLogger log = AppLogger.getLogger(GithubRepoDiscussions.class);

    @ConfigProperty(name = "quarkus.smallrye-graphql-client.github.header.Authorization")
    String authorization;;

    @ConfigProperty(name = "product.manager.cache.dir")
    String baseDirectory;

    @Inject
    @ConfigProperty(name = "product.manager.github.token")
    String githubToken;

    @Inject
    Github githubService;

    public enum Field {
        CREATED_AT,
        UPDATED_AT;
    }

    public enum Direction {
        ASC,
        DESC;
    }

    public record DiscussionOrder(Field field, Direction direction) {

    }

    public record Discussion(String title, String createdAt) {
    }

    public record DiscussionConnection(List<Discussion> nodes) {
    }

    public record RepositoryConnection(DiscussionConnection discussions) {
    }

    public interface Repository {
        @Query("""
                query {
                    repository(owner: $owner, name: $name) {
                        discussions(first: 10, orderBy: $orderBy) {
                            nodes {
                                title,
                                createdAt
                            }
                        }
                    }
                }""")
        @NestedValue("repository.discussions.nodes")
        List<Discussion> discussions(String owner, String name, DiscussionOrder orderBy);
    }

    @Inject
    ObjectMapper objectMapper;

    public void pullDiscussions3(String repoName) throws Exception {
        String owner = repoName.split("/")[0];
        String name = repoName.split("/")[1];
        GraphQL graphql = new GraphQL(objectMapper);
        Repository api = graphql.query().endpoint("https://api.github.com/graphql").bearer(githubToken)
                .graphql(Repository.class);
        List<Discussion> discussionsList = api.discussions(owner, name,
                new DiscussionOrder(Field.CREATED_AT, Direction.ASC));
        for (Discussion discussion : discussionsList) {
            log.info("Discussion: " + discussion.title() + " - " + discussion.createdAt());
        }
    }
}
