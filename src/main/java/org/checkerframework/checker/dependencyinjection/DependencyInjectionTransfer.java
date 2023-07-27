package org.checkerframework.checker.dependencyinjection;

import org.checkerframework.common.value.ValueTransfer;
import org.checkerframework.framework.flow.CFAnalysis;

public class DependencyInjectionTransfer extends ValueTransfer {

  public DependencyInjectionTransfer(CFAnalysis analysis) {
    super(analysis);
  }
}
