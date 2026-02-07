package io.quarkiverse.graphql.client;

import java.lang.reflect.Parameter;
import java.lang.reflect.RecordComponent;

public class Variable {

    public static String inputType(Parameter parameter) {
        InputType annotation = parameter.getAnnotation(InputType.class);
        if (annotation != null) {
            return annotation.value();
        }
        return inputType(parameter.getType());
    }

    public static String inputType(Class<?> type) {
        InputType annotation = type.getAnnotation(InputType.class);
        if (annotation != null) {
            return annotation.value();
        }
        if (type == String.class) {
            return "String!";
        }
        if (type.isPrimitive()) {
            if (type == boolean.class) {
                return "Boolean!";
            }
            if (type == int.class) {
                return "Int!";
            }
            if (type == float.class) {
                return "Float!";
            }
            throw new IllegalArgumentException("Unsupported primitive type: " + type);
        }
        return type.getSimpleName() + "!";
    }

    public static String variable(Object value) {
        try {
            StringBuilder sb = new StringBuilder();
            variable(sb, value);
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalArgumentException("Error converting variable to string: " + e.getMessage(), e);
        }
    }

    private static void variable(StringBuilder sb, Object value) throws Exception {
        if (value.getClass() == String.class) {
            sb.append("\"" + value + "\"");
            return;
        }
        if (value.getClass().isEnum()) {
            sb.append(((Enum<?>) value).name());
            return;
        }
        if (value.getClass().isPrimitive()) {
            sb.append(value.toString());
            return;
        }
        if (value.getClass().isRecord()) {
            fromRecord(sb, value);
            return;
        }
        throw new IllegalArgumentException("Unsupported type: " + value.getClass());
    }

    private static void fromRecord(StringBuilder sb, Object value) throws Exception {
        sb.append("{");
        RecordComponent[] components = value.getClass().getRecordComponents();
        for (int i = 0; i < components.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            RecordComponent component = components[i];
            Object val = component.getAccessor().invoke(value);
            if (val == null) {
                continue;
            }
            sb.append(component.getName() + ": ");
            variable(sb, val);
        }
        sb.append("}");
    }
}
