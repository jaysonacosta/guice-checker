package org.checkerframework.checker.dependencyinjection.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.DefaultQualifierInHierarchy;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * Represents classes and names that have been passed as arguments to {@link
 * com.google.inject.Binder#bind()}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({})
@DefaultQualifierInHierarchy
public @interface Bind {
  /** The fully-qualified name of this annotation. */
  static final String NAME = "org.checkerframework.checker.dependencyinjection.qual.Bind";
  /**
   * Class names that have definitely been passed as an argument to a {@link
   * com.google.inject.Binder#bind()} call.
   *
   * @return classes that have definitely been passed as an arugment to {@link
   *     com.google.inject.Binder#bind()}
   */
  public String[] value() default {};
}
