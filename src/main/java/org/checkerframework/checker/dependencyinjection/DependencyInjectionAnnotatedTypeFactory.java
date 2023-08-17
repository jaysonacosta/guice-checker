package org.checkerframework.checker.dependencyinjection;

import com.sun.source.tree.Tree;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import org.checkerframework.checker.dependencyinjection.qual.Bind;
import org.checkerframework.checker.dependencyinjection.qual.BindBottom;
import org.checkerframework.common.accumulation.AccumulationAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.reflection.ClassValAnnotatedTypeFactory;
import org.checkerframework.common.reflection.ClassValChecker;
import org.checkerframework.common.reflection.qual.ClassVal;
import org.checkerframework.dataflow.cfg.ControlFlowGraph;
import org.checkerframework.dataflow.cfg.block.Block;
import org.checkerframework.dataflow.cfg.node.MethodAccessNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreeUtils;

public class DependencyInjectionAnnotatedTypeFactory extends AccumulationAnnotatedTypeFactory {

  // TODO: Not a good idea to use this logger
  Logger logger = Logger.getLogger(DependencyInjectionAnnotatedTypeFactory.class.getName());

  ClassValAnnotatedTypeFactory classValATF = getTypeFactoryOfSubchecker(ClassValChecker.class);

  static HashMap<String, String> knownBindings = new HashMap<>();

  /** The {@code com.google.inject.AbstractModule.bind(Class<Baz> clazz)} method */
  private final List<ExecutableElement> bindMethods = new ArrayList<>(3);

  /** The {@code com.google.inject.binder.LinkedBindingBuilder.to(Class<? extends Baz> method */
  private final List<ExecutableElement> toMethods = new ArrayList<>(3);

  /** The ClassVal.value argument/element. */
  public final ExecutableElement classValValueElement =
      TreeUtils.getMethod(ClassVal.class, "value", 0, processingEnv);

  /** The Bind.value argument/element. */
  public final ExecutableElement bindValValueElement =
      TreeUtils.getMethod(Bind.class, "value", 0, processingEnv);

  public DependencyInjectionAnnotatedTypeFactory(BaseTypeChecker c) {
    super(c, Bind.class, BindBottom.class);
    ProcessingEnvironment processingEnv = this.getProcessingEnv();
    this.bindMethods.add(
        TreeUtils.getMethod(
            "com.google.inject.AbstractModule", "bind", processingEnv, "com.google.inject.Key<T>"));
    this.bindMethods.add(
        TreeUtils.getMethod(
            "com.google.inject.AbstractModule",
            "bind",
            processingEnv,
            "com.google.inject.TypeLiteral<T>"));
    this.bindMethods.add(
        TreeUtils.getMethod(
            "com.google.inject.AbstractModule", "bind", processingEnv, "java.lang.Class<T>"));

    this.toMethods.add(
        TreeUtils.getMethod(
            "com.google.inject.binder.LinkedBindingBuilder",
            "to",
            processingEnv,
            "java.lang.Class<? extends T>"));
    this.toMethods.add(
        TreeUtils.getMethod(
            "com.google.inject.binder.LinkedBindingBuilder",
            "to",
            processingEnv,
            "com.google.inject.TypeLiteral<? extends T>"));
    this.toMethods.add(
        TreeUtils.getMethod(
            "com.google.inject.binder.LinkedBindingBuilder",
            "to",
            processingEnv,
            "com.google.inject.Key<? extends T>"));

    this.postInit();
  }

  /* Returns true iff the argument is an invocation of AbstractModule.bind.
   *
   * @param methodTree the method invocation tree
   * @return true iff the argument is an invocation of AbstractModule.bind()
   */
  protected boolean isBindMethod(Tree methodTree) {
    return TreeUtils.isMethodInvocation(methodTree, this.bindMethods, this.getProcessingEnv());
  }

  /* Returns true iff the argument is an invocation of AbstractModule.to() */
  protected boolean isToMethod(Tree methodTree) {
    return TreeUtils.isMethodInvocation(methodTree, this.toMethods, this.getProcessingEnv());
  }

  /* Invoked when the `MethodInvocationNode` is a call to `com.google.inject.AbstractModule.bind`

  * This method will add the bound class to the `knownBindings` map
  *
  * @param methodArgumentNode the argument to the `bind` method
  */
  private void handleBindMethodInvocation(Node methodArgumentNode) {
    // Class that is being bound - put in knownBindings
    AnnotatedTypeMirror boundClassTypeMirror =
        classValATF.getAnnotatedType(methodArgumentNode.getTree());

    // TODO: getAnnotation may possibly return annotations that aren't @ClassVal
    List<String> classNames =
        AnnotationUtils.getElementValueArray(
            boundClassTypeMirror.getAnnotation(), classValValueElement, String.class);

    classNames.forEach(
        className -> {
          DependencyInjectionAnnotatedTypeFactory.knownBindings.put(className, null);
        });
  }

  /* Invoked when the `MethodInvocationNode` is a call to `com.google.inject.binder.LinkedBindingBuilder.to`

  * This method will attempt to find the class that is being bound and add it to the `knownBindings` map
  * alongside the class that is being bound to
  *
  * @param currentNode the current node in the block
  * @param methodAccessNode the method access node
  * @param methodArgumentNode the argument to the `to` method
  */
  private void handleToMethodInvocation(
      Node currentNode, MethodAccessNode methodAccessNode, Node methodArgumentNode) {
    Node receiver = methodAccessNode.getReceiver();

    // Class that is being bound - should be in knownBindings
    CFValue value = this.getStoreBefore(currentNode).getValue(JavaExpression.fromNode(receiver));

    AnnotationMirror bindAnno = null;
    if (value != null && !value.getAnnotations().isEmpty()) {
      for (AnnotationMirror anno : value.getAnnotations()) {
        if (AnnotationUtils.areSameByName(
            anno, "org.checkerframework.checker.dependencyinjection.qual.Bind")) {
          bindAnno = anno;
          break;
        }
      }
    }

    if (bindAnno != null) {
      List<String> boundClassNames =
          AnnotationUtils.getElementValueArray(bindAnno, bindValValueElement, String.class);

      boundClassNames.forEach(
          boundClassName -> {
            if (DependencyInjectionAnnotatedTypeFactory.knownBindings.containsKey(boundClassName)) {
              DependencyInjectionAnnotatedTypeFactory.knownBindings.remove(boundClassName);
              // Class that is being bound to - put in knownBindings
              // <bound,boundTo>
              AnnotatedTypeMirror boundToClassTypeMirror =
                  classValATF.getAnnotatedType(methodArgumentNode.getTree());

              List<String> boundToClassNames =
                  AnnotationUtils.getElementValueArray(
                      boundToClassTypeMirror.getAnnotation(), classValValueElement, String.class);

              boundToClassNames.forEach(
                  boundToClassName -> {
                    DependencyInjectionAnnotatedTypeFactory.knownBindings.put(
                        boundClassName, boundToClassName);
                  });
            }
          });
    }
  }

  @Override
  protected void postAnalyze(ControlFlowGraph cfg) {
    Set<Block> visited = new HashSet<>();
    Deque<Block> worklist = new ArrayDeque<>();

    Block entry = cfg.getEntryBlock();
    worklist.add(entry);
    visited.add(entry);

    while (!worklist.isEmpty()) {
      Block current = worklist.remove();

      current
          .getNodes()
          .forEach(
              node -> {
                if (node instanceof MethodInvocationNode) {
                  MethodInvocationNode methodInvocationNode = (MethodInvocationNode) node;

                  if (methodInvocationNode.getOperands().size() >= 2) {
                    MethodAccessNode methodAccessNode = methodInvocationNode.getTarget();
                    Node methodArgumentNode = methodInvocationNode.getArgument(0);

                    if (isBindMethod(methodInvocationNode.getTree())) {
                      handleBindMethodInvocation(methodArgumentNode);
                    } else if (isToMethod(methodInvocationNode.getTree())) {
                      handleToMethodInvocation(node, methodAccessNode, methodArgumentNode);
                    }
                  }
                }
              });

      visited.add(current);
      current
          .getSuccessors()
          .forEach(
              block -> {
                if (!visited.contains(block)) {
                  worklist.add(block);
                }
              });
    }

    System.out.println("Known Bindings:");
    DependencyInjectionAnnotatedTypeFactory.knownBindings.forEach(
        (key, value) -> {
          System.out.print(String.format("<Key: %s, Value: %s>\n", key, value));
        });

    System.out.println();

    super.postAnalyze(cfg);
  }
}
