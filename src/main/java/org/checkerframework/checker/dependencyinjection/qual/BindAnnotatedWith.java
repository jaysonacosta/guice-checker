package org.checkerframework.checker.dependencyinjection.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * Represents classes and names that have been passed as arguments to {@link
 * com.google.inject.Binder#bind()} and {@link
 * com.google.inject.binder.AnnotatedBindingBuilder#annotatedWith()} respectively.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({Bind.class})
public @interface BindAnnotatedWith {
  /** The fully-qualified name of this annotation. */
  static final String NAME =
      "org.checkerframework.checker.dependencyinjection.qual.BindAnnotatedWith";

  /**
   * Class names that have definitely been passed as an argument to a {@link
   * com.google.inject.Binder#bind()} call.
   *
   * @return classes that have definitely been passed as an arugment to {@link
   *     com.google.inject.Binder#bind()}
   */
  public String[] value() default {};

  /**
   * Names that have been passed as an argument to an {@link
   * com.google.inject.binder.AnnotatedBindingBuilder#annotatedWith()} call.
   *
   * @return names that have been passed as an argument to {@link
   *     com.google.inject.binder.AnnotatedBindingBuilder#annotatedWith()}
   */
  public String[] annotatedWith() default {};
}
