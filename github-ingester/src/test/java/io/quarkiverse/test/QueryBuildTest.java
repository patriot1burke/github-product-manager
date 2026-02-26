package io.quarkiverse.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkiverse.graphql.client.ArgsOnly;
import io.quarkiverse.graphql.client.DefaultVariables;
import io.quarkiverse.graphql.client.GraphQL;
import io.quarkiverse.graphql.client.GraphQLClient;
import io.quarkiverse.graphql.client.Namespace;
import io.quarkiverse.graphql.client.Query;
import io.quarkiverse.graphql.client.QueryTemplate;
import io.quarkiverse.graphql.client.Variable;

public class QueryBuildTest {

    @Test
    public void testQueryExpression() {
        onlyMatchesQueryWithoutParams("query { repository(owner: \"quarkusio\", name: \"quarkus\") { name } }");
        onlyMatchesQueryWithoutParams("   query{ repository(owner: \"quarkusio\", name: \"quarkus\") { name } }");
        onlyMatchesQueryWithParams("query($owner: String!) { repository(owner: $owner, name: \"quarkus\") { name } }");
        onlyMatchesQueryWithParams(
                "   query  ($owner: String!){ repository(owner: $owner, name: \"quarkus\") { name } }");
        onlyMatchesQueryFunctionWithoutParams(
                "query Repository { repository(owner: \"quarkusio\", name: \"quarkus\") { name } }");
        onlyMatchesQueryFunctionWithoutParams(
                "   query Repository { repository(owner: \"quarkusio\", name: \"quarkus\") { name } }");
        onlyMatchesQueryFunctionParams(
                "query Repository($owner: String!) { repository(owner: $owner, name: \"quarkus\") { name } }");
        onlyMatchesQueryFunctionParams(
                "   query Repository($owner: String!) { repository(owner: $owner, name: \"quarkus\") { name } }");
    }

    @Test
    public void testAddParams() {
        {
            Matcher matcher = GraphQL.QUERY_WITHOUT_PARAMS
                    .matcher("query { repository(owner: $owner, name: \"quarkus\") { name } }");
            String query = matcher.replaceFirst(Matcher.quoteReplacement("query($owner: String!) {"));
            assertEquals("query($owner: String!) { repository(owner: $owner, name: \"quarkus\") { name } }", query);
        }
        {
            Matcher matcher = GraphQL.QUERY_FUNCTION_WITHOUT_PARAMS
                    .matcher("query Repository { repository(owner: $owner, name: \"quarkus\") { name } }");
            assertTrue(matcher.lookingAt());
            String function = matcher.group(1);
            String query = matcher.replaceFirst(Matcher.quoteReplacement("query " + function + "($owner: String!) {"));
            assertEquals("query Repository($owner: String!) { repository(owner: $owner, name: \"quarkus\") { name } }", query);
        }
    }

    static void onlyMatchesQueryWithParams(String query) {
        assertTrue(GraphQL.QUERY_WITH_PARAMS.matcher(query).lookingAt());
        assertFalse(GraphQL.QUERY_WITHOUT_PARAMS.matcher(query).lookingAt());
        assertFalse(GraphQL.QUERY_FUNCTION_WITH_PARAMS.matcher(query).lookingAt());
        assertFalse(GraphQL.QUERY_FUNCTION_WITHOUT_PARAMS.matcher(query).lookingAt());
    }

    static void onlyMatchesQueryWithoutParams(String query) {
        assertTrue(GraphQL.QUERY_WITHOUT_PARAMS.matcher(query).lookingAt());
        assertFalse(GraphQL.QUERY_WITH_PARAMS.matcher(query).lookingAt());
        assertFalse(GraphQL.QUERY_FUNCTION_WITH_PARAMS.matcher(query).lookingAt());
        assertFalse(GraphQL.QUERY_FUNCTION_WITHOUT_PARAMS.matcher(query).lookingAt());
    }

    static void onlyMatchesQueryFunctionParams(String query) {
        assertTrue(GraphQL.QUERY_FUNCTION_WITH_PARAMS.matcher(query).lookingAt());
        assertFalse(GraphQL.QUERY_WITH_PARAMS.matcher(query).lookingAt());
        assertFalse(GraphQL.QUERY_WITHOUT_PARAMS.matcher(query).lookingAt());
        assertFalse(GraphQL.QUERY_FUNCTION_WITHOUT_PARAMS.matcher(query).lookingAt());
    }

    static void onlyMatchesQueryFunctionWithoutParams(String query) {
        assertTrue(GraphQL.QUERY_FUNCTION_WITHOUT_PARAMS.matcher(query).lookingAt());
        assertFalse(GraphQL.QUERY_WITH_PARAMS.matcher(query).lookingAt());
        assertFalse(GraphQL.QUERY_WITHOUT_PARAMS.matcher(query).lookingAt());
        assertFalse(GraphQL.QUERY_FUNCTION_WITH_PARAMS.matcher(query).lookingAt());
    }

    record Repository(String name) {
    }

    public interface GithubClient {
        @QueryTemplate("query { repository(owner: \"quarkusio\", name: \"quarkus\") { name } }")
        Repository queryNoParams();

        @QueryTemplate("query($owner: Foo!) { repository(owner: $owner, name: \"quarkus\") { name } }")
        Repository queryStringParams();

        @QueryTemplate("query { repository(owner: $owner, name: \"quarkus\") { name } }")
        Repository queryParams(String owner);

        @QueryTemplate("query Repository { repository(owner: \"quarkusio\", name: \"quarkus\") { name } }")
        Repository functionNoParams();

        @QueryTemplate("query Repository { repository(owner: $owner, name: \"quarkus\") { name } }")
        Repository functionParams(String owner);

        @QueryTemplate("query Repository($owner: Foo!) { repository(owner: $owner, name: \"quarkus\") { name } }")
        Repository functionStringParams();

    }

    @Test
    public void testQueryBuild() {
        Map<String, String> queryMap = new HashMap<>();
        for (Method method : GithubClient.class.getMethods()) {
            String query = GraphQL.QueryBuilder.queryMapping(method);
            queryMap.put(method.getName(), query);
        }
        assertEquals(queryMap.get("queryNoParams"), "query { repository(owner: \"quarkusio\", name: \"quarkus\") { name } }");
        assertEquals(queryMap.get("queryStringParams"),
                "query($owner: Foo!) { repository(owner: $owner, name: \"quarkus\") { name } }");
        assertEquals(queryMap.get("queryParams"),
                "query($owner: String!) { repository(owner: $owner, name: \"quarkus\") { name } }");
        assertEquals(queryMap.get("functionNoParams"),
                "query Repository { repository(owner: \"quarkusio\", name: \"quarkus\") { name } }");
        assertEquals(queryMap.get("functionParams"),
                "query Repository($owner: String!) { repository(owner: $owner, name: \"quarkus\") { name } }");
        assertEquals(queryMap.get("functionStringParams"),
                "query Repository($owner: Foo!) { repository(owner: $owner, name: \"quarkus\") { name } }");
        assertEquals(queryMap.size(), 6);
    }

    public record Author(String login) {
    }

    public record Discussion(String id, String title, Author author, String createdAt, String updatedAt) {
    }

    public record PageInfo(boolean hasNextPage, String endCursor) {
    }

    public record DiscussionConnection(int totalCount, PageInfo pageInfo, List<Discussion> nodes) {
    }

    public interface Github {
        public interface Repository {
            @Query
            DiscussionConnection discussions(int first, @Namespace(".nodes") @Variable("first") int commentsFirst);
        }

        Repository repository(String owner, String name);

        @Namespace("repository")
        @Query
        @DefaultVariables("orderBy: {field: CREATED_AT, direction: DESC}")
        DiscussionConnection discussions(@Namespace("repository") String owner, @Namespace("repository") String name,
                int first);
    }

    @Test
    public void testPreMap() throws Exception {
        Map<String, GraphQLClient.QueryBuilder.MethodMapping> methodMapping = GraphQLClient.QueryBuilder
                .getMethodMapping(new ObjectMapper(), Github.class);
        printMapping(methodMapping);
    }

    public interface API {

        public interface Repository {
            Discussions discussions(int first);

        }

        public interface Discussions {
            @ArgsOnly
            @Query
            DiscussionConnection nextPage(String after);
        }

        Repository repository(String owner, String name);

    }

    @Test
    public void testArgsOnly() throws Exception {
        System.out.println("==================== testArgsOnly ====================");
        Map<String, GraphQLClient.QueryBuilder.MethodMapping> methodMapping = GraphQLClient.QueryBuilder
                .getMethodMapping(new ObjectMapper(), API.class);
        printMapping(methodMapping);
    }

    private void printMapping(Map<String, GraphQLClient.QueryBuilder.MethodMapping> methodMapping) {
        for (Map.Entry<String, GraphQLClient.QueryBuilder.MethodMapping> entry : methodMapping.entrySet()) {
            GraphQLClient.QueryBuilder.MethodMapping mapping = entry.getValue();
            if (mapping.query() != null) {
                System.out.println("-------" + entry.getKey() + "-------");
                System.out.println(mapping.query());
                System.out.println("------- END -------");
            } else if (mapping.methodMap() != null) {
                System.out.println(">>>>>>>>>>>>>>>>" + entry.getKey() + ">>>>>>>>>>>>>>>>");
                printMapping(mapping.methodMap());
                System.out.println("<<<<<<<<<<<<<<<<<" + entry.getKey() + "<<<<<<<<<<<<<<<<<<<");
            }
        }
    }
}
