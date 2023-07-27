package org.checkerframework.checker.dependencyinjection;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.value.ValueVisitor;

public class DependencyInjectionVisitor extends ValueVisitor {

  public DependencyInjectionVisitor(BaseTypeChecker c) {
    super(c);
  }

  @Override
  protected DependencyInjectionAnnotatedTypeFactory createTypeFactory() {
    return new DependencyInjectionAnnotatedTypeFactory(this.checker);
  }
}
