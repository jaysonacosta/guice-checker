package org.checkerframework.checker.dependencyinjection.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.DefaultQualifierInHierarchy;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * If an expression has type {@code @Bind({"foo"})}, then {@code foo.class} has been passed as an
 * argument to {@code bind} in a Guice module.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({})
@DefaultQualifierInHierarchy
public @interface Bind {
  /**
   * Classes that have definitely been passed as an arugment to {@code bind} on the expression whose
   * type is annotated.
   *
   * @return classes that have definitely been passed as an arugment to {@code bind}
   */
  public String[] value() default {};
}
