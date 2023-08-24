package org.checkerframework.checker.dependencyinjection;

import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import java.util.Arrays;
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
      System.out.println("\u001B[34mDependencyInjectionTransfer.visitMethodInvocation()\u001B[0m");
      AnnotatedTypeMirror t = diATF.getAnnotatedType(node.getTree());

      MethodInvocationNode methodInvocationNode = node;
      MethodInvocationNode callToBindNode =
          (MethodInvocationNode) methodInvocationNode.getTarget().getReceiver();
      MethodInvocationNode namesClassNode =
          (MethodInvocationNode) methodInvocationNode.getArgument(0);
      FieldAccessNode boundClass = (FieldAccessNode) callToBindNode.getArgument(0);
      StringLiteralNode givenName = (StringLiteralNode) namesClassNode.getArgument(0);

      accumulateBindAnnotatedWith(node, result, boundClass.toString(), givenName.getValue());
    }

    return result;
  }

  public void accumulateBindAnnotatedWith(
      Node node, TransferResult<CFValue, CFStore> result, String value, String name) {
    List<String> valuesAsList = Arrays.asList(value);
    List<String> namesAsList = Arrays.asList(name);
    // If dataflow has already recorded information about the target, fetch it and integrate
    // it into the list of values in the new annotation.
    JavaExpression target = JavaExpression.fromNode(node);
    // TODO: Not sure if this is necessary.
    // if (CFAbstractStore.canInsertJavaExpression(target)) {
    //   CFValue flowValue = result.getRegularStore().getValue(target);
    //   System.out.println("flowValue: " + flowValue);
    //   if (flowValue != null) {
    //     AnnotationMirrorSet flowAnnos = flowValue.getAnnotations();
    //     assert flowAnnos.size() <= 1;
    //     for (AnnotationMirror anno : flowAnnos) {
    //       if (diATF.isAccumulatorAnnotation(anno)) {
    //         List<String> oldFlowValues = diATF.getAccumulatedValues(anno);
    //         if (!oldFlowValues.isEmpty()) {
    //           // valuesAsList cannot have its length changed -- it is backed by an
    //           // array -- but if oldFlowValues is not empty, it is a new, modifiable
    //           // list.
    //           oldFlowValues.addAll(valuesAsList);
    //           valuesAsList = oldFlowValues;
    //         }
    //       }
    //     }
    //   }
    // }

    AnnotationMirror newAnno = diATF.createBindAnnotatedWithAnnotation(valuesAsList, namesAsList);
    insertIntoStores(result, target, newAnno);
    System.out.printf("newAnno: %s\n", newAnno);
    Tree tree = node.getTree();

    if (tree != null && tree.getKind() == Tree.Kind.METHOD_INVOCATION) {
      Node receiver = ((MethodInvocationNode) node).getTarget().getReceiver();
      System.out.println("receiver: " + receiver);
      if (receiver != null && diATF.returnsThis((MethodInvocationTree) tree)) {
        accumulateBindAnnotatedWith(receiver, result, value, name);
      }
    }
  }
}
