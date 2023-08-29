package org.checkerframework.checker.dependencyinjection;

import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.common.accumulation.AccumulationTransfer;
import org.checkerframework.common.reflection.ClassValChecker;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.FieldAccessNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.StringLiteralNode;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.framework.flow.CFAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.AnnotationUtils;

public class DependencyInjectionTransfer extends AccumulationTransfer {

  private final DependencyInjectionAnnotatedTypeFactory diATF;

  public DependencyInjectionTransfer(CFAnalysis analysis) {
    super(analysis);
    this.diATF = (DependencyInjectionAnnotatedTypeFactory) analysis.getTypeFactory();
  }

  @Override
  public TransferResult<CFValue, CFStore> visitMethodInvocation(
      final MethodInvocationNode node, final TransferInput<CFValue, CFStore> input) {

    TransferResult<CFValue, CFStore> result = super.visitMethodInvocation(node, input);

    if (diATF.isBindMethod(node.getTree())) {
      Node boundClass = node.getArgument(0);

      AnnotatedTypeMirror boundClassTypeMirror =
          this.diATF
              .getTypeFactoryOfSubchecker(ClassValChecker.class)
              .getAnnotatedType(boundClass.getTree());

      List<String> classNames =
          AnnotationUtils.getElementValueArray(
              boundClassTypeMirror.getAnnotation(), diATF.classValValueElement, String.class);

      accumulate(node, result, classNames.toArray(new String[1]));
    } else if (diATF.isAnnotatedWithMethod(node.getTree())) {
      MethodInvocationNode methodInvocationNode = node;
      MethodInvocationNode callToBindNode =
          (MethodInvocationNode) methodInvocationNode.getTarget().getReceiver();
      MethodInvocationNode namesClassNode =
          (MethodInvocationNode) methodInvocationNode.getArgument(0);
      FieldAccessNode boundClass = (FieldAccessNode) callToBindNode.getArgument(0);
      StringLiteralNode givenName = (StringLiteralNode) namesClassNode.getArgument(0);

      accumulateBindAnnotatedWith(
          node, result, boundClass.getReceiver().getType().toString(), givenName.getValue());
    }

    return result;
  }

  public void accumulateBindAnnotatedWith(
      Node node, TransferResult<CFValue, CFStore> result, String value, String name) {

    JavaExpression target = JavaExpression.fromNode(node);

    AnnotationMirror newAnno = diATF.createBindAnnotatedWithAnnotation(value, name);

    // TODO: insertValue not updating stores, insertValuePermitNondeterministic is
    if (result.containsTwoStores()) {
      result.getThenStore().insertValuePermitNondeterministic(target, newAnno);
      result.getElseStore().insertValuePermitNondeterministic(target, newAnno);
    } else {
      result.getRegularStore().insertValuePermitNondeterministic(target, newAnno);
    }

    Tree tree = node.getTree();

    if (tree != null && tree.getKind() == Tree.Kind.METHOD_INVOCATION) {
      Node receiver = ((MethodInvocationNode) node).getTarget().getReceiver();
      if (receiver != null && diATF.returnsThis((MethodInvocationTree) tree)) {
        accumulateBindAnnotatedWith(receiver, result, value, name);
      }
    }
  }
}
