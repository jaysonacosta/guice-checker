package org.checkerframework.checker.dependencyinjection;

import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.dependencyinjection.qual.Bind;
import org.checkerframework.checker.dependencyinjection.qual.BindAnnotatedWith;
import org.checkerframework.common.accumulation.AccumulationTransfer;
import org.checkerframework.common.reflection.ClassValChecker;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.AssignmentNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.StringLiteralNode;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.framework.flow.CFAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.AnnotationMirrorSet;
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

      classNames.forEach(
          className ->
              accumulate(
                  node,
                  result,
                  DependencyInjectionAnnotatedTypeFactory.resolveInjectionPointClassName(
                      className)));

    } else if (diATF.isAnnotatedWithMethod(node.getTree())) {

      MethodInvocationNode methodInvocationNode = node;

      CFValue value =
          diATF
              .getStoreBefore(node)
              .getValue(JavaExpression.fromNode(methodInvocationNode.getTarget().getReceiver()));

      if (value != null && !value.getAnnotations().isEmpty()) {
        AnnotationMirrorSet annotations = value.getAnnotations();
        annotations.forEach(
            (annotation) -> {
              if (AnnotationUtils.areSameByName(annotation, Bind.NAME)) {
                List<String> classNames =
                    AnnotationUtils.getElementValueArray(
                        annotation, diATF.bindValValueElement, String.class);
                MethodInvocationNode namesClassNode =
                    (MethodInvocationNode) methodInvocationNode.getArgument(0);
                StringLiteralNode givenName = (StringLiteralNode) namesClassNode.getArgument(0);

                accumulateBindAnnotatedWith(node, result, classNames.get(0), givenName.getValue());
              }
            });
      }
    }

    return result;
  }

  /**
   * TODO: Workaround for a known issue where the transfer function fails to propogate the Guice
   * annotations from the right-hand side (rhs) to the left-hand side (lhs) of an assignment.
   *
   * <p>This method manually annotates the lhs of an assignment with the annotations from the rhs to
   * temporarily fix the issue.
   *
   * <p>Attempts to resolve the issue included:
   *
   * <ol>
   *   <li>Including the Guice methods in an astub file to indicate their determinism.
   *   <li>Running the checker with the -AassumePure flag.
   * </ol>
   */
  @Override
  public TransferResult<CFValue, CFStore> visitAssignment(
      AssignmentNode node, TransferInput<CFValue, CFStore> input) {

    TransferResult<CFValue, CFStore> result = super.visitAssignment(node, input);

    if (!diATF.isBindMethod(node.getExpression().getTree())
        && !diATF.isAnnotatedWithMethod(node.getExpression().getTree())) {
      return result;
    }

    CFValue value =
        diATF.getStoreBefore(node).getValue(JavaExpression.fromNode(node.getExpression()));

    if (value != null && !value.getAnnotations().isEmpty()) {
      AnnotationMirrorSet annotations = value.getAnnotations();
      annotations.forEach(
          annotation -> {
            if (AnnotationUtils.areSameByName(annotation, Bind.NAME)) {
              List<String> classNames =
                  AnnotationUtils.getElementValueArray(
                      annotation, diATF.bindValValueElement, String.class);

              accumulate(node.getTarget(), result, classNames.toArray(new String[1]));
            } else if (AnnotationUtils.areSameByName(annotation, BindAnnotatedWith.NAME)) {
              List<String> classNames =
                  AnnotationUtils.getElementValueArray(
                      annotation, diATF.bawValValueElement, String.class);
              List<String> annotationNames =
                  AnnotationUtils.getElementValueArray(
                      annotation, diATF.bawAnnotatedWithValueElement, String.class);

              accumulateBindAnnotatedWith(
                  node.getTarget(), result, classNames.get(0), annotationNames.get(0));
            }
          });
    }

    return result;
  }

  public void accumulateBindAnnotatedWith(
      Node node, TransferResult<CFValue, CFStore> result, String value, String name) {

    JavaExpression target = JavaExpression.fromNode(node);

    AnnotationMirror newAnno = diATF.createBindAnnotatedWithAnnotation(value, name);

    insertIntoStores(result, target, newAnno);
  }
}
