package org.checkerframework.checker.dependencyinjection.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * If an expression has type {@code @Bind({"foo"}, {"bar"})}, then {@code foo.class} has been passed
 * as an argument to {@code bind} and {@code Names.named("bar")} hsa been passed to {@code
 * annotatedWith} in a Guice module.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({Bind.class})
public @interface BindAnnotatedWith {
  /**
   * Classes that have definitely been passed as an arugment to a {@code bind} call whose result is
   * the expression whose type is annotated. The arguments are "fully qualified binary names"
   * ({@link org.checkerframework.checker.signature.qual.FqBinaryName}): a primitive or <a
   * href="https://docs.oracle.com/javase/specs/jls/se17/html/jls-13.html#jls-13.1">binary name</a>,
   * possibly followed by some number of array brackets.
   *
   * @return classes that have definitely been passed as an arugment to {@code bind} and names that
   *     have definitely been passed as an argument to {@code annotatedWith}
   */
  public String[] value() default {};

  public String[] annotatedWith() default {};
}
