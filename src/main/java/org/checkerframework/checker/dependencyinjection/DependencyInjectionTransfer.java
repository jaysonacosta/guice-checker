package org.checkerframework.checker.dependencyinjection;

import java.util.List;
import org.checkerframework.common.accumulation.AccumulationTransfer;
import org.checkerframework.common.reflection.ClassValAnnotatedTypeFactory;
import org.checkerframework.common.reflection.ClassValChecker;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.framework.flow.CFAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.AnnotationUtils;

public class DependencyInjectionTransfer extends AccumulationTransfer {

  private final DependencyInjectionAnnotatedTypeFactory diATF;
  private final ClassValAnnotatedTypeFactory classValATF;

  public DependencyInjectionTransfer(CFAnalysis analysis) {
    super(analysis);
    this.diATF = (DependencyInjectionAnnotatedTypeFactory) analysis.getTypeFactory();
    this.classValATF = diATF.getTypeFactoryOfSubchecker(ClassValChecker.class);
  }

  @Override
  public TransferResult<CFValue, CFStore> visitMethodInvocation(
      final MethodInvocationNode node, final TransferInput<CFValue, CFStore> input) {

    TransferResult<CFValue, CFStore> result = super.visitMethodInvocation(node, input);

    if (diATF.isBindMethod(node.getTree())) {
      Node boundClass = node.getArgument(0);

      AnnotatedTypeMirror boundClassTypeMirror =
          this.classValATF.getAnnotatedType(boundClass.getTree());

      List<String> classNames =
          AnnotationUtils.getElementValueArray(
              boundClassTypeMirror.getAnnotation(), diATF.classValValueElement, String.class);

      classNames.forEach(name -> accumulate(node, result, name));
    }

    return result;
  }
}
