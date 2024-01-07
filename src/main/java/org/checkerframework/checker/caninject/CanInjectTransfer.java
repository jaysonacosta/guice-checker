package org.checkerframework.checker.caninject;

import java.util.List;
import org.checkerframework.common.accumulation.AccumulationTransfer;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.ArrayCreationNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.framework.flow.CFAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;

public class CanInjectTransfer extends AccumulationTransfer {

  public CanInjectTransfer(CFAnalysis analysis) {
    super(analysis);
  }

  @Override
  public TransferResult<CFValue, CFStore> visitMethodInvocation(
      final MethodInvocationNode node, final TransferInput<CFValue, CFStore> input) {

    TransferResult<CFValue, CFStore> result = super.visitMethodInvocation(node, input);

    CanInjectAnnotatedTypeFactory atf = (CanInjectAnnotatedTypeFactory) analysis.getTypeFactory();

    if (atf.isCreateInjectorMethod(node.getTree())) {
      ArrayCreationNode argumentNode = (ArrayCreationNode) node.getArgument(0);
      List<Node> modules = argumentNode.getInitializers();

      System.out.println("node");
      System.out.println();
      modules.forEach(
          module -> {
            System.out.println("module");
            System.out.println(module);
            System.out.println();
            String moduleFullyQualifiedName =
                ElementUtils.getQualifiedClassName(TreeUtils.elementFromTree(module.getTree()))
                    .toString();

            System.out.println(result);
            System.out.println();
            this.accumulate(node, result, moduleFullyQualifiedName);
            System.out.println(result);
          });
    }

    return result;
  }
}
