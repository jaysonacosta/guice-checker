package org.checkerframework.checker.dependencyinjection;

import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.common.value.ValueChecker;

/** This is the entry point for pluggable type-checking. */
public class DependencyInjectionChecker extends ValueChecker {

  public DependencyInjectionChecker() {}

  @Override
  protected BaseTypeVisitor<?> createSourceVisitor() {
    return new DependencyInjectionVisitor(this);
  }
}
