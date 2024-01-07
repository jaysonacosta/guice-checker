package org.checkerframework.checker.caninject;

import org.checkerframework.common.accumulation.AccumulationChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.qual.StubFiles;

/** This is the entry point for pluggable type-checking. */
@StubFiles({"CanInject.astub"})
public class CanInjectChecker extends AccumulationChecker {

  public CanInjectChecker() {}

  @Override
  protected BaseTypeVisitor<?> createSourceVisitor() {
    return new CanInjectVisitor(this);
  }
}
