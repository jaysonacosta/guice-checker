package org.checkerframework.checker.dependencyinjection;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.value.ValueAnnotatedTypeFactory;
import org.checkerframework.dataflow.cfg.ControlFlowGraph;

public class DependencyInjectionAnnotatedTypeFactory extends ValueAnnotatedTypeFactory {

  public DependencyInjectionAnnotatedTypeFactory(BaseTypeChecker c) {
    super(c);
    super.postInit();
  }

  @Override
  protected void postAnalyze(ControlFlowGraph cfg) {
    super.postAnalyze(cfg);
  }
}
