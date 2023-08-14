package org.checkerframework.checker.dependencyinjection;

import org.checkerframework.common.accumulation.AccumulationTransfer;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.framework.flow.CFAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFValue;

public class DependencyInjectionTransfer extends AccumulationTransfer {

  public DependencyInjectionTransfer(CFAnalysis analysis) {
    super(analysis);
  }

  @Override
  public TransferResult<CFValue, CFStore> visitMethodInvocation(
      final MethodInvocationNode node, final TransferInput<CFValue, CFStore> input) {
    System.out.printf("node: %s\n", node);
    System.out.printf("\tnode type: %s\n", node.getType());
    System.out.printf("\tnode tree: %s\n", node.getTree());
    System.out.printf("\tnode type: %s\n\n", node.getArguments());
    TransferResult<CFValue, CFStore> result = super.visitMethodInvocation(node, input);
    return result;
  }
}
