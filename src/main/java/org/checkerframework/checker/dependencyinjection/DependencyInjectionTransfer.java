package org.checkerframework.checker.dependencyinjection;

import org.checkerframework.framework.flow.CFAnalysis;
import org.checkerframework.framework.flow.CFTransfer;

public class DependencyInjectionTransfer extends CFTransfer {

  public DependencyInjectionTransfer(CFAnalysis analysis) {
    super(analysis);
  }
}
