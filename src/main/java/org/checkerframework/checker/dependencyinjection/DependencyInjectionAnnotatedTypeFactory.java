package org.checkerframework.checker.dependencyinjection;

import com.sun.source.tree.Tree;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.reflection.ClassValAnnotatedTypeFactory;
import org.checkerframework.dataflow.cfg.ControlFlowGraph;
import org.checkerframework.dataflow.cfg.block.Block;
import org.checkerframework.dataflow.cfg.node.MethodAccessNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.TreeUtils;

public class DependencyInjectionAnnotatedTypeFactory extends ClassValAnnotatedTypeFactory {

  /** The {@code com.google.inject.AbstractModule.bind(Class<Baz> clazz)} method */
  private final List<ExecutableElement> bindMethods = new ArrayList<>(3);

  /** The {@code com.google.inject.binder.LinkedBindingBuilder.to(Class<? extends Baz> method */
  private final List<ExecutableElement> toMethods = new ArrayList<>(3);

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

    System.out.println("--------------------");

    Set<Block> visited = new HashSet<>();
    Deque<Block> worklist = new ArrayDeque<>();
    HashMap<Integer, Integer> knownBindings = new HashMap<>();

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

                  System.out.printf("Method Invocation: %s\n", methodInvocationNode);
                  System.out.printf("Operands: %s\n\n", methodInvocationNode.getOperands());

                  if (methodInvocationNode.getOperands().size() >= 2) {
                    MethodAccessNode methodAccessNode = methodInvocationNode.getTarget();
                    Node fieldAccessNode = methodInvocationNode.getArgument(0);

                    if (isBindMethod(methodInvocationNode.getTree())) {
                      // Class that is being bound - put in knownBindings
                      AnnotatedTypeMirror boundClassTypeMirror =
                          this.getAnnotatedType(fieldAccessNode.getTree());

                      knownBindings.put(boundClassTypeMirror.getUnderlyingTypeHashCode(), null);
                    } else if (isToMethod(methodInvocationNode.getTree())) {
                      Node receiver = methodAccessNode.getReceiver();

                      // TODO: This feels a little hacky to get the bound class type mirror
                      // Class that is being bound - should be in knownBindings
                      AnnotatedTypeMirror boundClassTypeMirror =
                          this.getAnnotatedType(
                              receiver.getOperands().toArray(new Node[2])[1].getTree());

                      if (knownBindings.containsKey(
                          boundClassTypeMirror.getUnderlyingTypeHashCode())) {
                        knownBindings.remove(boundClassTypeMirror.getUnderlyingTypeHashCode());
                        // Class that is being bound to - put in knownBindings <bound, boundTo>
                        AnnotatedTypeMirror boundToClassTypeMirror =
                            this.getAnnotatedType(fieldAccessNode.getTree());
                        knownBindings.put(
                            boundClassTypeMirror.getUnderlyingTypeHashCode(),
                            boundToClassTypeMirror.getUnderlyingTypeHashCode());
                      }
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

      System.out.println("Worklist");
      worklist.forEach(block -> System.out.printf("Block: %s\n", block));
      System.out.println("Visited");
      visited.forEach(block -> System.out.printf("Block: %s\n", block));
    }

    knownBindings.forEach(
        (key, value) -> {
          System.out.printf("Key: %d, Value: %d\n", key, value);
        });

    super.postAnalyze(cfg);
  }
}
