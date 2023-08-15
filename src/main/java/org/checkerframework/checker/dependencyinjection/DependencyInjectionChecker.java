package org.checkerframework.checker.dependencyinjection;

import java.util.LinkedHashSet;
import org.checkerframework.common.accumulation.AccumulationChecker;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.common.reflection.ClassValChecker;
import org.checkerframework.framework.qual.StubFiles;

/** This is the entry point for pluggable type-checking. */
@StubFiles({"Guice.astub"})
public class DependencyInjectionChecker extends AccumulationChecker {

  public DependencyInjectionChecker() {}

  @Override
  protected BaseTypeVisitor<?> createSourceVisitor() {
    return new DependencyInjectionVisitor(this);
  }

  @Override
  protected LinkedHashSet<Class<? extends BaseTypeChecker>> getImmediateSubcheckerClasses() {
    LinkedHashSet<Class<? extends BaseTypeChecker>> checkers =
        super.getImmediateSubcheckerClasses();
    checkers.add(ClassValChecker.class);

    return checkers;
  }
}
