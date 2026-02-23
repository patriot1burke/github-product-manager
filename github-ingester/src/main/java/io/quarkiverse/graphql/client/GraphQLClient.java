package io.quarkiverse.graphql.client;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.logging.Log;

public class GraphQLClient {
    private ObjectMapper objectMapper;
    private Client client;

    public GraphQLClient(ObjectMapper objectMapper, Client client) {
        this.objectMapper = objectMapper;
        this.client = client;
    }

    public GraphQLClient(ObjectMapper objectMapper) {
        this(objectMapper, ClientBuilder.newClient());
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

        public <T> T target(Class<T> type) {
            Map<String, MethodMapping> methodMapping = getMethodMapping(objectMapper, type);
            Map<String, MethodInvoker> methodInvokerMap = buildInvokerMap(methodMapping);
            if (endpoint == null) {
                GraphQLEndpoint annotation = type.getAnnotation(GraphQLEndpoint.class);
                if (annotation == null) {
                    throw new RuntimeException("No endpoint specified for GraphQL client");
                }
                endpoint = annotation.value();
            }
            GraphTarget target = new GraphTarget(client.target(endpoint), headers);
            return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type },
                    new ClientInvoker(methodInvokerMap, target));
        }

        public record GraphTarget(WebTarget target, Map<String, String> headers) {

        }

        public interface MethodInvoker {
            Object invoke(Object[] args, GraphTarget target, Map<String, Object> variables);
        }

        public record NamespaceMethod(Class<?> proxyType, List<String> argMap, Map<String, MethodInvoker> methodMap) {
        }

        public class NamespaceInvoker implements MethodInvoker {
            NamespaceMethod namespaceMethod;

            public NamespaceInvoker(NamespaceMethod namespaceMethod) {
                this.namespaceMethod = namespaceMethod;
            }

            @Override
            public Object invoke(Object[] args, GraphTarget target, Map<String, Object> variables) {
                variables = new HashMap<>(variables);
                for (int i = 0; i < namespaceMethod.argMap.size(); i++) {
                    String arg = namespaceMethod.argMap.get(i);
                    variables.put(arg, args[i]);
                }

                return Proxy.newProxyInstance(namespaceMethod.proxyType.getClassLoader(),
                        new Class<?>[] { namespaceMethod.proxyType },
                        new ProxyInvoker(namespaceMethod.methodMap, target, variables));
            }
        }

        public class ProxyInvoker implements InvocationHandler {
            Map<String, MethodInvoker> methodMap;
            Map<String, Object> variables;
            GraphTarget target;

            public ProxyInvoker(Map<String, MethodInvoker> methodMap, GraphTarget target,
                    Map<String, Object> variables) {
                this.methodMap = methodMap;
                this.variables = variables;
                this.target = target;
            }

            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                MethodInvoker methodInvoker = methodMap.get(method.toGenericString());
                if (methodInvoker == null) {
                    throw new RuntimeException("Method implementation not found: " + method.toGenericString());
                }
                return methodInvoker.invoke(args, target, variables);
            }
        }

        public class QueryInvoker implements MethodInvoker {
            String query;
            List<String> argMap;
            List<String> fieldPrefix;
            Type returnType;

            public QueryInvoker(String query, List<String> fieldPrefix, List<String> argMap, Type returnType) {
                this.query = query;
                this.fieldPrefix = fieldPrefix;
                this.returnType = returnType;
                this.argMap = argMap;
            }

            @Override
            public Object invoke(Object[] args, GraphTarget target, Map<String, Object> variables) {
                try {
                    variables = new HashMap<>(variables);
                    for (int i = 0; i < argMap.size(); i++) {
                        String arg = argMap.get(i);
                        variables.put(arg, args[i]);
                    }

                    String jsonRequest = null;
                    if (variables.isEmpty()) {
                        jsonRequest = objectMapper.writeValueAsString(Map.of("query", query));
                    } else {
                        jsonRequest = objectMapper.writeValueAsString(Map.of("query", query, "variables", variables));
                    }
                    // System.out.println("---- JSON Request ----");
                    // System.out.println(jsonRequest);
                    // System.out.println("---- End JSON Request ----");
                    Builder request = target.target.request();
                    for (Map.Entry<String, String> entry : headers.entrySet()) {
                        request = request.header(entry.getKey(), entry.getValue());
                    }
                    Response post = request.post(Entity.json(jsonRequest));
                    if (post.getStatus() != 200) {
                        throw new RuntimeException("Failed to execute query: " + post.getStatus());
                    }

                    String body = post.readEntity(String.class);
                    Log.debugv("Response body: {0}", body);

                    JsonNode json = objectMapper.readTree(body);
                    if (json.get("errors") != null) {
                        throw new RuntimeException("Failed to execute query: " + json.get("errors").toString());
                    }
                    JsonNode data = json.get("data");
                    if (data == null) {
                        throw new RuntimeException("Failed to execute query, no data returned");
                    }
                    for (String field : fieldPrefix) {
                        data = data.get(field);
                    }
                    return objectMapper.treeToValue(data, objectMapper.constructType(returnType));
                } catch (Exception e) {
                    throw new RuntimeException("Failed to execute query: ", e);
                }
            }
        }

        class ClientInvoker implements InvocationHandler {
            Map<String, MethodInvoker> methodMap;
            GraphTarget target;

            ClientInvoker(Map<String, MethodInvoker> methodMap, GraphTarget target) {
                this.methodMap = methodMap;
                this.target = target;
            }

            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                MethodInvoker methodInvoker = methodMap.get(method.toGenericString());
                if (methodInvoker == null) {
                    throw new RuntimeException("Method implementation not found: " + method.toGenericString());
                }
                return methodInvoker.invoke(args, target, new HashMap<>());
            }
        }

        public record MethodMapping(String query, Type genericReturnType, Class<?> returnType, List<String> fieldPrefix,
                List<String> argMap, Map<String, MethodMapping> methodMap) {

        }

        public static class MappingBuilder {

            public List<String> paramTypes = new ArrayList<>();
            public Map<String, Object> variableValues = new HashMap<>();
            // public List<String> queryFields = new ArrayList<>();
            public List<String> fields = new ArrayList<>();
            public Map<String, List<String>> namespacedParams = new HashMap<>();
            public String baseVariable = "";
            public String baseNamespace;

            public MappingBuilder copy() {
                MappingBuilder builder = new MappingBuilder();
                builder.paramTypes = new ArrayList<>(paramTypes);
                builder.variableValues = new HashMap<>(variableValues);
                builder.fields = new ArrayList<>(fields);
                builder.namespacedParams = new HashMap<>(namespacedParams);
                builder.baseVariable = baseVariable;
                builder.baseNamespace = baseNamespace;
                return builder;
            }
        }

        protected Map<String, MethodInvoker> buildInvokerMap(Map<String, MethodMapping> methodMapping) {
            Map<String, MethodInvoker> methodInvokerMap = new HashMap<>();
            for (Map.Entry<String, MethodMapping> entry : methodMapping.entrySet()) {
                MethodMapping mapping = entry.getValue();
                if (mapping.query() != null) {
                    methodInvokerMap.put(entry.getKey(),
                            new QueryInvoker(mapping.query(), mapping.fieldPrefix(), mapping.argMap(), mapping.returnType()));
                } else if (mapping.methodMap() != null) {
                    Map<String, MethodInvoker> proxyMap = buildInvokerMap(mapping.methodMap());
                    NamespaceMethod namespaceMethod = new NamespaceMethod(mapping.returnType, mapping.argMap(),
                            proxyMap);
                    methodInvokerMap.put(entry.getKey(), new NamespaceInvoker(namespaceMethod));
                } else {
                    throw new RuntimeException("Method mapping not found: " + entry.getKey());
                }
            }
            return methodInvokerMap;
        }

        public static Map<String, MethodMapping> getMethodMapping(ObjectMapper objectMapper, Class<?> type) {
            return getMethodMapping(objectMapper, type, new MappingBuilder());
        }

        public static Map<String, MethodMapping> getMethodMapping(ObjectMapper objectMapper, Class<?> type,
                MappingBuilder base) {
            Map<String, MethodMapping> methodMap = new HashMap<>();
            for (Method method : type.getMethods()) {
                MappingBuilder requestBuilder = base.copy();
                List<String> argMap = fillBuilder(method, requestBuilder);
                Map<String, MethodMapping> proxyMapping = null;
                String query = null;
                if (method.isAnnotationPresent(Query.class)) {
                    query = generateQuery(objectMapper, requestBuilder, method);
                } else if (method.getReturnType().isInterface()) {
                    proxyMapping = getMethodMapping(objectMapper, method.getReturnType(), requestBuilder);
                } else {
                    throw new RuntimeException(
                            "Method must be annotated with @Query or return an interface: " + method.toGenericString());
                }
                MethodMapping methodMapping = new MethodMapping(query, method.getGenericReturnType(),
                        method.getReturnType(),
                        requestBuilder.fields, argMap, proxyMapping);
                methodMap.put(method.toGenericString(), methodMapping);
            }
            return methodMap;
        }

        public static List<String> fillBuilder(Method method, MappingBuilder builder) {
            // System.out.println("fillBuilder: " + method.toGenericString());
            Parameter[] parameters = method.getParameters();
            GraphField graphField = method.getAnnotation(GraphField.class);
            String fieldName = graphField == null ? method.getName() : graphField.value();

            if (builder.baseNamespace == null) {
                builder.baseNamespace = "";
            } else {
                builder.baseNamespace = builder.baseNamespace + ".";
            }
            Namespace methodNamespace = method.getAnnotation(Namespace.class);
            if (methodNamespace != null) {
                for (String value : methodNamespace.value().split("\\.")) {
                    builder.fields.add(value);
                }
                builder.baseNamespace = builder.baseNamespace + methodNamespace.value() + ".";
            }
            builder.baseNamespace += fieldName;

            // String fieldCall = fieldName;
            builder.fields.add(fieldName);
            List<String> argMap = new ArrayList<>();
            for (int i = 0; i < parameters.length; i++) {
                String variable = builder.baseVariable + parameters[i].getName();
                String paramType = "$" + variable + ": " + Variables.inputType(parameters[i]);
                builder.paramTypes.add(paramType);
                Variable graphParam = parameters[i].getAnnotation(Variable.class);
                String paramName = graphParam == null ? parameters[i].getName() : graphParam.value();
                String param = paramName + ": $" + variable;
                String mapKey = null;
                Namespace namespace = parameters[i].getAnnotation(Namespace.class);
                if (namespace != null) {
                    if (namespace.value().startsWith(".")) {
                        mapKey = builder.baseNamespace + namespace.value();
                    } else {
                        mapKey = namespace.value();
                    }
                } else {
                    mapKey = builder.baseNamespace;
                }
                builder.namespacedParams.computeIfAbsent(mapKey, k -> new ArrayList<>()).add(param);
                argMap.add(variable);
            }
            DefaultVariables defaultVariables = method.getAnnotation(DefaultVariables.class);
            if (defaultVariables != null) {
                for (DefaultVariable defaultVariable : defaultVariables.value()) {
                    addDefaultVariable(builder, defaultVariable);
                }
            }
            DefaultVariable defaultVariable = method.getAnnotation(DefaultVariable.class);
            if (defaultVariable != null) {
                addDefaultVariable(builder, defaultVariable);
            }
            builder.baseVariable += fieldName;
            return argMap;
        }

        private static void addDefaultVariable(MappingBuilder builder, DefaultVariable defaultVariable) {
            String mapKey = null;
            String namespace = defaultVariable.namespace();
            if (namespace.isEmpty()) {
                mapKey = builder.baseNamespace;
            } else if (namespace.startsWith(".")) {
                mapKey = builder.baseNamespace + namespace;
            } else {
                mapKey = namespace;
            }
            String param = defaultVariable.name() + ": " + defaultVariable.value();
            builder.namespacedParams.computeIfAbsent(mapKey, k -> new ArrayList<>()).add(param);
        }

        private static void generateParams(StringBuilder query, List<String> params) {
            if (params.isEmpty()) {
                return;
            }
            query.append("(");
            for (int i = 0; i < params.size(); i++) {
                if (i > 0) {
                    query.append(", ");
                }
                query.append(params.get(i));
            }
            query.append(")");
        }

        public static String generateQuery(ObjectMapper objectMapper, MappingBuilder builder, Method method) {
            // System.out.println("generateQuery: " + method.toGenericString());
            StringBuilder query = new StringBuilder();
            int indent = 0;
            query.append("query");
            generateParams(query, builder.paramTypes);
            query.append(" {\n");
            indent += 3;
            int nested = 1;
            String ns = null;
            for (String fieldName : builder.fields) {
                if (ns == null) {
                    ns = fieldName;
                } else {
                    ns = ns + "." + fieldName;
                }
                query.repeat(" ", indent).append(fieldName);
                List<String> params = builder.namespacedParams.get(ns);
                if (params != null) {
                    generateParams(query, params);
                }
                query.append(" {\n");
                nested++;
                indent += 3;
            }
            // System.out.println("generateQuery: " +
            // method.getGenericReturnType().toString());
            generateQuery(objectMapper, query, builder, method.getReturnType(), method.getGenericReturnType(), indent, ns,
                    true);

            for (int i = 0; i < nested; i++) {
                indent -= 3;
                query.repeat(" ", indent).append("}\n");
            }
            query.append("\n");
            return query.toString();
        }

        public static void generateQuery(ObjectMapper objectMapper, StringBuilder query, MappingBuilder builder, Class<?> clazz,
                Type type, int indent,
                String namespace, boolean first) {

            if (!Properties.isGraphType(clazz, type)) {
                query.append("\n");
                return;
            }

            if (!first)
                query.append(" {\n");
            for (Properties.Property component : Properties.getProperties(clazz, objectMapper)) {
                String name = component.name();
                query.repeat(" ", indent).append(name);
                String nextNamespace = null;
                if (namespace == null) {
                    nextNamespace = name;
                } else {
                    nextNamespace = namespace + "." + name;
                }
                List<String> params = builder.namespacedParams.get(nextNamespace);
                //System.out.println("lookup: " + nextNamespace + ": " + params);
                if (params != null) {
                    generateParams(query, params);
                }
                generateQuery(objectMapper, query, builder, component.type(), component.genericType(), indent + 3,
                        nextNamespace, false);
            }
            if (!first)
                query.repeat(" ", indent - 3).append("}\n");
        }

    }

    public QueryBuilder query() {
        return new QueryBuilder();
    }

}
