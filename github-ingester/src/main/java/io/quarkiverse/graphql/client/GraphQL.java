package io.quarkiverse.graphql.client;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.logging.Log;

public class GraphQL {
    private ObjectMapper objectMapper;
    private Client client;

    public GraphQL(ObjectMapper objectMapper, Client client) {
        this.objectMapper = objectMapper;
        this.client = client;
    }

    public GraphQL(ObjectMapper objectMapper) {
        this(objectMapper, ClientBuilder.newClient());
    }

    public static record QueryMapping(String query, String[] nestedValue) {

    }

    public static Pattern QUERY_WITH_PARAMS = Pattern.compile("^\\s*query\\s*\\([^)]*\\)");
    public static Pattern QUERY_WITHOUT_PARAMS = Pattern.compile("^\\s*query\\s*\\{");
    public static Pattern QUERY_FUNCTION_WITH_PARAMS = Pattern
            .compile("^\\s*query\\s*([a-zA-Z0-9_]+)\\s*\\(([^)]*)\\)");
    public static Pattern QUERY_FUNCTION_WITHOUT_PARAMS = Pattern.compile("^\\s*query\\s*([a-zA-Z0-9_]+)\\s*\\{");

    class Invoker implements InvocationHandler {
        private Map<String, String> headers;
        private Map<String, QueryMapping> queryMap;
        private WebTarget target;

        public Invoker(Map<String, String> headers, Map<String, QueryMapping> queryMap, WebTarget target) {
            this.headers = headers;
            this.queryMap = queryMap;
            this.target = target;
        }

        // we make static so we can test it easy
        public static String jsonRequest(ObjectMapper objectMapper, String query, Method method, Object[] args)
                throws Exception {
            Map<String, Object> variables = new HashMap<>();
            for (int i = 0; i < args.length; i++) {
                if (args[i] == null) {
                    throw new IllegalArgumentException(
                            "Argument " + method.getParameters()[i].getName() + " cannot be null");
                }
                variables.put(method.getParameters()[i].getName(), args[i]);
            }
            if (variables.isEmpty()) {
                query = objectMapper.writeValueAsString(Map.of("query", query));
            } else {
                query = objectMapper.writeValueAsString(Map.of("query", query, "variables", variables));
            }
            return query;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            QueryMapping queryMapping = queryMap.get(method.toGenericString());
            String query = jsonRequest(objectMapper, queryMapping.query(), method, args);
            Builder request = target.request();
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                request = request.header(entry.getKey(), entry.getValue());
            }
            Response post = request.post(Entity.json(query));
            if (post.getStatus() != 200) {
                throw new RuntimeException("Failed to execute query: " + post.getStatus());
            }
            String body = post.readEntity(String.class);
            Log.info("Response body: " + body);

            JsonNode json = objectMapper.readTree(body);
            if (json.get("errors") != null) {
                throw new RuntimeException("Failed to execute query: " + json.get("errors").toString());
            }
            JsonNode data = json.get("data");
            if (data == null) {
                throw new RuntimeException("Failed to execute query, no data returned");
            }
            JsonNode nestedValue = data;
            for (String value : queryMapping.nestedValue()) {
                nestedValue = nestedValue.get(value);
            }
            return objectMapper.treeToValue(nestedValue, objectMapper.constructType(method.getGenericReturnType()));
        }
    }

    static class VariablesBuilder {
        private Map<String, String> variables = new HashMap<>();
        private String query;

        public VariablesBuilder(String query) {
            this.query = query;
        }

        public VariablesBuilder variable(String name, Object value) {
            if (value == null) {
                return this;
            }
            variables.put(name, Variable.variable(value));
            return this;
        }

        public String build() {
            for (Map.Entry<String, String> entry : variables.entrySet()) {
                query = query.replace("$" + entry.getKey(), entry.getValue());
            }
            return query;
        }
    }

    public class QueryBuilder {
        private Map<String, String> headers = new HashMap<>();
        private String endpoint;

        QueryBuilder() {

        }

        public QueryBuilder header(String name, String value) {
            headers.put(name, value);
            return this;
        }

        public QueryBuilder bearer(String token) {
            headers.put("Authorization", "Bearer " + token);
            return this;
        }

        public QueryBuilder endpoint(String endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        public static String queryMapping(Method method) {
            Query annotation = method.getAnnotation(Query.class);
            if (annotation == null) {
                throw new RuntimeException("No query specified for method: " + method.getName());
            }
            String query = annotation.value().trim();
            if (QUERY_WITH_PARAMS.matcher(query).lookingAt()) {
                return query;
            }
            if (QUERY_FUNCTION_WITH_PARAMS.matcher(query).lookingAt()) {
                return query;
            }
            if (!QUERY_WITHOUT_PARAMS.matcher(query).lookingAt() && !QUERY_FUNCTION_WITHOUT_PARAMS.matcher(query).lookingAt()) {
                throw new RuntimeException("Query must start with 'query {' or 'query function {' but got: " + query);
            }

            boolean hasParams = false;
            StringBuilder variableDeclaration = new StringBuilder();
            for (int i = 0; i < method.getParameterCount(); i++) {
                Parameter parameter = method.getParameters()[i];
                if (!hasParams) {
                    hasParams = true;
                } else {
                    variableDeclaration.append(", ");
                }
                variableDeclaration.append("$").append(parameter.getName()).append(": ")
                        .append(Variable.inputType(parameter));
            }
            if (hasParams) {
                String params = "(" + variableDeclaration.toString() + ")";
                Matcher matcher = QUERY_WITHOUT_PARAMS.matcher(query);
                if (matcher.lookingAt()) {
                    query = matcher.replaceFirst(Matcher.quoteReplacement("query" + params + " {"));
                } else {
                    matcher = QUERY_FUNCTION_WITHOUT_PARAMS.matcher(query);
                    if (matcher.lookingAt()) {
                        query = matcher
                                .replaceFirst(Matcher.quoteReplacement("query " + matcher.group(1) + params + " {"));
                    } else {
                        throw new RuntimeException("Invalid query: " + query);
                    }
                }
            }
            Log.info("Executing query: " + query);

            return query;
        }

        public static Map<String, QueryMapping> getQueryMap(Class<?> type) {
            Map<String, QueryMapping> queryMap = new HashMap<>();
            for (Method method : type.getMethods()) {
                String[] nestedValue = new String[0];
                NestedValue nestedValueAnnotation = method.getAnnotation(NestedValue.class);
                if (nestedValueAnnotation != null) {
                    nestedValue = nestedValueAnnotation.value().split("\\.");
                }
                String query = queryMapping(method);
                queryMap.put(method.toGenericString(), new QueryMapping(query, nestedValue));
            }
            return queryMap;
        }

        public <T> T graphql(Class<T> type) {
            Map<String, QueryMapping> queryMap = getQueryMap(type);
            if (endpoint == null) {
                GraphQLEndpoint annotation = type.getAnnotation(GraphQLEndpoint.class);
                if (annotation == null) {
                    throw new RuntimeException("No endpoint specified for type: " + type.getName());
                }
                endpoint = annotation.value();
            }
            WebTarget target = client.target(endpoint);
            return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type },
                    new Invoker(headers, queryMap, target));
        }

    }

    public <T> T graphql(String endpoint, Class<T> type) {
        return new QueryBuilder().endpoint(endpoint).graphql(type);
    }

    public QueryBuilder query() {
        return new QueryBuilder();
    }

}
