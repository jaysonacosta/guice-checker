package org.checkerframework.checker.dependencyinjection;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.value.ValueAnnotatedTypeFactory;

public class DependencyInjectionAnnotatedTypeFactory extends ValueAnnotatedTypeFactory {

  public DependencyInjectionAnnotatedTypeFactory(BaseTypeChecker c) {
    super(c);
  }
}
