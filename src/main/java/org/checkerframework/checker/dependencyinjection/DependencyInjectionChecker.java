package org.checkerframework.checker.dependencyinjection;

import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.common.reflection.ClassValChecker;

/** This is the entry point for pluggable type-checking. */
public class DependencyInjectionChecker extends ClassValChecker {

  public DependencyInjectionChecker() {}

  @Override
  protected BaseTypeVisitor<?> createSourceVisitor() {
    return new DependencyInjectionVisitor(this);
  }
}
