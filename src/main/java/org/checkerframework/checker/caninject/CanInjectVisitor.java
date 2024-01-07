package org.checkerframework.checker.caninject;

import org.checkerframework.common.accumulation.AccumulationVisitor;
import org.checkerframework.common.basetype.BaseTypeChecker;

public class CanInjectVisitor extends AccumulationVisitor {
  public CanInjectVisitor(BaseTypeChecker checker) {
    super(checker);
  }
}
