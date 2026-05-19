package io.quarkiverse.graphql.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class QueryError extends RuntimeException {
    JsonNode errorNode;
    ObjectMapper mapper;

    public QueryError(String message, JsonNode errorNode, ObjectMapper mapper) {
        super(message);
        this.errorNode = errorNode;
        this.mapper = mapper;
    }

    public <T> T toErrorData(Class<T> clazz) {
        try {
            return mapper.treeToValue(errorNode, clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public JsonNode errorNode() {
        return errorNode;
    }

    public String toString() {
        return errorNode.toString();
    }
}
