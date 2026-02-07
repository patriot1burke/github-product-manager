package io.quarkiverse.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;

import org.junit.jupiter.api.Test;

import io.quarkiverse.graphql.client.GraphQL;
import io.quarkiverse.graphql.client.Query;

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
        @Query("query { repository(owner: \"quarkusio\", name: \"quarkus\") { name } }")
        Repository queryNoParams();

        @Query("query($owner: Foo!) { repository(owner: $owner, name: \"quarkus\") { name } }")
        Repository queryStringParams();

        @Query("query { repository(owner: $owner, name: \"quarkus\") { name } }")
        Repository queryParams(String owner);

        @Query("query Repository { repository(owner: \"quarkusio\", name: \"quarkus\") { name } }")
        Repository functionNoParams();

        @Query("query Repository { repository(owner: $owner, name: \"quarkus\") { name } }")
        Repository functionParams(String owner);

        @Query("query Repository($owner: Foo!) { repository(owner: $owner, name: \"quarkus\") { name } }")
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
}
