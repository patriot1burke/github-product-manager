package io.quarkiverse.graphql.client;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;

public class Properties {
    public record Property(String name, Class<?> type, Type genericType, Member member) {
    }

    public static boolean isGraphType(Class<?> type, Type genericType) {
        if (isPrimitive(type)) {
            return false;
        }
        return !Collection.class.isAssignableFrom(type);
    }

    private static boolean isPrimitive(Class<?> type) {
        return type.isPrimitive() || type.equals(String.class) || type.equals(Boolean.class) || type.equals(Integer.class)
                || type.equals(Long.class) || type.equals(Float.class) || type.equals(Double.class) || type.equals(Byte.class)
                || type.equals(Short.class) || type.equals(Character.class);
    }

    public static List<Property> getProperties(Class<?> clazz, ObjectMapper objectMapper) {
        List<Property> properties = new ArrayList<>();
        JavaType javaType = objectMapper.getTypeFactory().constructType(clazz);
        DeserializationConfig deserializationConfig = objectMapper.getDeserializationConfig();
        BeanDescription beanDescription = deserializationConfig.introspect(javaType);
        List<BeanPropertyDefinition> beanPropertyDefinitions = beanDescription.findProperties();
        for (BeanPropertyDefinition bean : beanPropertyDefinitions) {
            String name = bean.getName();
            Member member = bean.getAccessor().getMember();
            Type genericType = null;
            Class<?> type = null;
            if (member instanceof Field) {
                Field field = (Field) member;
                genericType = field.getGenericType();
                type = field.getType();
            } else if (member instanceof Method) {
                Method method = (Method) member;
                genericType = method.getGenericReturnType();
                type = method.getReturnType();
            } else {
                throw new RuntimeException("Unsupported member type: " + member.getClass().getName());
            }
            if (Collection.class.isAssignableFrom(type)) {
                if (genericType instanceof ParameterizedType) {
                    ParameterizedType parameterizedType = (ParameterizedType) genericType;
                    Type actualType = parameterizedType.getActualTypeArguments()[0];
                    if (actualType instanceof Class<?>) {
                        Class<?> actualClass = (Class<?>) actualType;
                        if (isPrimitive(actualClass)) {
                            continue;
                        }
                        type = (Class<?>) actualType;
                        genericType = actualType;
                    }
                }
            }

            properties.add(new Property(name, type, genericType, member));
        }
        return properties;
    }

}
