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
   * <p>The arguments are "fully qualified binary names" ({@link
   * org.checkerframework.checker.signature.qual.FqBinaryName}): a primitive or <a
   * href="https://docs.oracle.com/javase/specs/jls/se17/html/jls-13.html#jls-13.1">binary name</a>.
   *
   * @return classes that have definitely been passed as an arugment to {@link
   *     com.google.inject.Binder#bind()}
   */
  public String[] value() default {};
}
