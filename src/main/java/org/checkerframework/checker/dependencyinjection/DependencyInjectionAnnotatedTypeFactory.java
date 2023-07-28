package org.checkerframework.checker.dependencyinjection;

import com.sun.source.tree.Tree;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.reflection.ClassValAnnotatedTypeFactory;
import org.checkerframework.common.reflection.qual.ClassVal;
import org.checkerframework.dataflow.cfg.ControlFlowGraph;
import org.checkerframework.dataflow.cfg.block.Block;
import org.checkerframework.dataflow.cfg.node.MethodAccessNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreeUtils;

public class DependencyInjectionAnnotatedTypeFactory extends ClassValAnnotatedTypeFactory {

  Logger logger = Logger.getLogger(DependencyInjectionAnnotatedTypeFactory.class.getName());

  /** The {@code com.google.inject.AbstractModule.bind(Class<Baz> clazz)} method */
  private final List<ExecutableElement> bindMethods = new ArrayList<>(3);

  /** The {@code com.google.inject.binder.LinkedBindingBuilder.to(Class<? extends Baz> method */
  private final List<ExecutableElement> toMethods = new ArrayList<>(3);

  /** The ClassVal.value argument/element. */
  public final ExecutableElement classValValueElement =
      TreeUtils.getMethod(ClassVal.class, "value", 0, processingEnv);

  public DependencyInjectionAnnotatedTypeFactory(BaseTypeChecker c) {
    super(c);
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
    super.postInit();
  }

  /* Returns true iff the argument is an invocation of AbstractModule.bind.
   *
   * @param methodTree the method invocation tree
   * @return true iff the argument is an invocation of AbstractModule.bind()
   */
  private boolean isBindMethod(Tree methodTree) {
    return TreeUtils.isMethodInvocation(methodTree, this.bindMethods, this.getProcessingEnv());
  }

  /* Returns true iff the argument is an invocation of AbstractModule.to() */
  private boolean isToMethod(Tree methodTree) {
    return TreeUtils.isMethodInvocation(methodTree, this.toMethods, this.getProcessingEnv());
  }

  @Override
  protected void postAnalyze(ControlFlowGraph cfg) {

    Set<Block> visited = new HashSet<>();
    Deque<Block> worklist = new ArrayDeque<>();
    HashMap<String, String> knownBindings = new HashMap<>();

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
                      // Class that is being bound - put in knownBindings
                      AnnotatedTypeMirror boundClassTypeMirror =
                          this.getAnnotatedType(methodArgumentNode.getTree());

                      List<String> classNames =
                          AnnotationUtils.getElementValueArray(
                              boundClassTypeMirror.getAnnotation(),
                              classValValueElement,
                              String.class);

                      classNames.forEach(
                          className -> {
                            knownBindings.put(className, null);
                          });

                    } else if (isToMethod(methodInvocationNode.getTree())) {
                      Node receiver = methodAccessNode.getReceiver();

                      // TODO: This feels a little hacky to get the bound class type mirror
                      // Class that is being bound - should be in knownBindings
                      AnnotatedTypeMirror boundClassTypeMirror =
                          this.getAnnotatedType(
                              receiver.getOperands().toArray(new Node[2])[1].getTree());

                      List<String> boundClassNames =
                          AnnotationUtils.getElementValueArray(
                              boundClassTypeMirror.getAnnotation(),
                              classValValueElement,
                              String.class);

                      boundClassNames.forEach(
                          boundClassName -> {
                            if (knownBindings.containsKey(boundClassName)) {
                              knownBindings.remove(boundClassName);
                              // Class that is being bound to - put in knownBindings <bound,boundTo>
                              AnnotatedTypeMirror boundToClassTypeMirror =
                                  this.getAnnotatedType(methodArgumentNode.getTree());

                              List<String> boundToClassNames =
                                  AnnotationUtils.getElementValueArray(
                                      boundToClassTypeMirror.getAnnotation(),
                                      classValValueElement,
                                      String.class);

                              boundToClassNames.forEach(
                                  boundToClassName -> {
                                    knownBindings.put(boundClassName, boundToClassName);
                                  });
                            }
                          });
                    }
                  }
                }
              });

      current
          .getSuccessors()
          .forEach(
              block -> {
                if (!visited.contains(block)) {
                  worklist.add(block);
                }
              });
    }

    knownBindings.forEach(
        (key, value) -> {
          logger.log(Level.INFO, String.format("<Key: %s, Value: %s>\n", key, value));
        });

    super.postAnalyze(cfg);
  }
}
