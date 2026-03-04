package io.quarkiverse.graphql.client;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * When this annotation is present, the method does not define a field. Arguments to the method will assign field variables
 * to the current field namespace.
 *
 * This is useful in situations where you've mapped out query field variables, but want to request
 * a different graph for the query.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ArgsOnly {

}
