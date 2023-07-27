package org.checkerframework.checker.dependencyinjection;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.reflection.ClassValVisitor;

public class DependencyInjectionVisitor extends ClassValVisitor {

  public DependencyInjectionVisitor(BaseTypeChecker c) {
    super(c);
  }

  @Override
  protected DependencyInjectionAnnotatedTypeFactory createTypeFactory() {
    return new DependencyInjectionAnnotatedTypeFactory(this.checker);
  }
}
