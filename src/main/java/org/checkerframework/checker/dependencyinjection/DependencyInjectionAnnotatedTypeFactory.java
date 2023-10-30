package org.checkerframework.checker.dependencyinjection;

import com.google.inject.Provides;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import java.lang.annotation.Annotation;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import org.checkerframework.checker.dependencyinjection.qual.Bind;
import org.checkerframework.checker.dependencyinjection.qual.BindAnnotatedWith;
import org.checkerframework.checker.dependencyinjection.qual.BindBottom;
import org.checkerframework.checker.dependencyinjection.utils.KnownBindingsValue;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.accumulation.AccumulationAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.reflection.ClassValChecker;
import org.checkerframework.common.reflection.qual.ClassVal;
import org.checkerframework.dataflow.cfg.ControlFlowGraph;
import org.checkerframework.dataflow.cfg.block.Block;
import org.checkerframework.dataflow.cfg.node.MethodAccessNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationMirrorSet;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;

public class DependencyInjectionAnnotatedTypeFactory extends AccumulationAnnotatedTypeFactory {

  private static HashMap<String, KnownBindingsValue> knownBindings = new HashMap<>();

  /** The {@code com.google.inject.AbstractModule.bind(Class<Baz> clazz)} method */
  private final List<ExecutableElement> bindMethods = new ArrayList<>(3);

  /** The {@code com.google.inject.binder.LinkedBindingBuilder.to(Class<? extends Baz> method */
  private final List<ExecutableElement> toMethods = new ArrayList<>(3);

  /** The {@code com.google.inject.binder.LinkedBindingBuilder.toInstance() method */
  private final List<ExecutableElement> toInstanceMethods = new ArrayList<>(1);

  private final List<ExecutableElement> annotatedWithMethods = new ArrayList<>(2);

  /** The ClassVal.value argument/element. */
  public final ExecutableElement classValValueElement =
      TreeUtils.getMethod(ClassVal.class, "value", 0, processingEnv);

  /** The Bind.value argument/element. */
  public final ExecutableElement bindValValueElement =
      TreeUtils.getMethod(Bind.class, "value", 0, processingEnv);

  /** The BindAnnotatedWith.value argument/element. */
  public final ExecutableElement bawValValueElement =
      TreeUtils.getMethod(BindAnnotatedWith.class, "value", 0, processingEnv);

  /** The BindAnnotatedWith.annotatedWith argument/element. */
  public final ExecutableElement bawAnnotatedWithValueElement =
      TreeUtils.getMethod(BindAnnotatedWith.class, "annotatedWith", 0, processingEnv);

  private void printKnownBindings() {
    System.out.println("Known Bindings:");
    DependencyInjectionAnnotatedTypeFactory.knownBindings.forEach(
        (key, value) -> {
          System.out.print(String.format("<Key: %s, Value: %s>\n", key, value));
        });

    System.out.println();
  }

  private void initializeMethodElements() {
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

    this.toInstanceMethods.add(
        TreeUtils.getMethod(
            "com.google.inject.binder.LinkedBindingBuilder", "toInstance", processingEnv, "T"));

    this.annotatedWithMethods.add(
        TreeUtils.getMethod(
            "com.google.inject.binder.AnnotatedBindingBuilder",
            "annotatedWith",
            processingEnv,
            "java.lang.annotation.Annotation"));
    this.annotatedWithMethods.add(
        TreeUtils.getMethod(
            "com.google.inject.binder.AnnotatedBindingBuilder",
            "annotatedWith",
            processingEnv,
            "java.lang.Class<? extends java.lang.annotation.Annotation>"));
  }

  public DependencyInjectionAnnotatedTypeFactory(BaseTypeChecker c) {
    super(c, Bind.class, BindBottom.class);
    this.initializeMethodElements();
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

  /* Returns true iff the argument is an invocation of LinkedBindingBuilder.to() */
  protected boolean isToMethod(Tree methodTree) {
    return TreeUtils.isMethodInvocation(methodTree, this.toMethods, this.getProcessingEnv());
  }

  /* Returns true iff the argument is an invocation of LinkedBindingBuilder.toInstance() */
  protected boolean isToInstanceMethod(Tree methodTree) {
    return TreeUtils.isMethodInvocation(
        methodTree, this.toInstanceMethods, this.getProcessingEnv());
  }

  /* Returns true iff the argument is an invocation of AnnotatedBindingBuilder.annotatedWith() */
  protected boolean isAnnotatedWithMethod(Tree methodTree) {
    return TreeUtils.isMethodInvocation(
        methodTree, this.annotatedWithMethods, this.getProcessingEnv());
  }

  /**
   * Invoked when a {@code MethodInvocationNode} is a call to {@code
   * com.google.inject.AbstractModule.bind}. This method takes the argument of a {@code
   * MethodInvocationNode}, a class, and adds the bound class to the {@code knownBindings} map.
   *
   * @param methodArgumentNode the argument to the {@code bind} method
   */
  private void handleBindMethodInvocation(Node methodArgumentNode) {
    // Class that is being bound - put in knownBindings
    AnnotatedTypeMirror boundClassTypeMirror =
        getTypeFactoryOfSubchecker(ClassValChecker.class)
            .getAnnotatedType(methodArgumentNode.getTree());

    // TODO: getAnnotation may possibly return annotations that aren't @ClassVal
    List<String> classNames =
        AnnotationUtils.getElementValueArray(
            boundClassTypeMirror.getAnnotation(), classValValueElement, String.class);

    classNames.forEach(
        className -> {
          DependencyInjectionAnnotatedTypeFactory.knownBindings.put(className, null);
        });
  }

  /**
   * Invoked when a {@code MethodInvocationNode} is a call to {@code
   * com.google.inject.binder.LinkedBindingBuilder.to}.
   *
   * <p>This method takes the argument of a {@code MethodInvocationNode}, a class, and binds it to
   * an existing class in the {@code knownBindings} map.
   *
   * @param currentNode the current node in the block
   * @param methodAccessNode the method access node (the {@code to} method)
   * @param methodArgumentNode the argument to the {@code to} method
   */
  private void handleToMethodInvocation(
      Node currentNode, MethodAccessNode methodAccessNode, Node methodArgumentNode) {
    Node receiver = methodAccessNode.getReceiver();

    // Class that is being bound - should be in knownBindings
    CFValue value = this.getStoreBefore(currentNode).getValue(JavaExpression.fromNode(receiver));

    AnnotationMirror bindAnno = null;
    if (value != null && !value.getAnnotations().isEmpty()) {
      for (AnnotationMirror anno : value.getAnnotations()) {
        if (AnnotationUtils.areSameByName(anno, Bind.NAME)) {
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
                  getTypeFactoryOfSubchecker(ClassValChecker.class)
                      .getAnnotatedType(methodArgumentNode.getTree());

              List<String> boundToClassNames =
                  AnnotationUtils.getElementValueArray(
                      boundToClassTypeMirror.getAnnotation(), classValValueElement, String.class);

              boundToClassNames.forEach(
                  boundToClassName -> {
                    DependencyInjectionAnnotatedTypeFactory.knownBindings.put(
                        boundClassName,
                        KnownBindingsValue.builder().className(boundToClassName).build());
                  });
            }
          });
    }
  }

  /**
   * Invoked when the annotation of the receiver to the {@code toInstance} method is {@code
   * BindAnnotatedWith}
   *
   * @param bawAnno the {@code BindAnnotatedWith} annotation
   * @param toInstanceMethodArgumentNode the argument to the {@code toInstance} method
   */
  private void handleBAWAnnotation(AnnotationMirror bawAnno, Node toInstanceMethodArgumentNode) {

    List<String> boundClassNames =
        AnnotationUtils.getElementValueArray(bawAnno, bawValValueElement, String.class);

    List<String> annotatedNames =
        AnnotationUtils.getElementValueArray(bawAnno, bawAnnotatedWithValueElement, String.class);

    if (annotatedNames.isEmpty()) {
      throw new IllegalArgumentException("BindAnnotatedWith annotation must have a value.");
    }

    System.out.printf("boundClassNames: %s\n", boundClassNames);
    System.out.printf("annotatedNames: %s\n", annotatedNames);
    System.out.printf("toInstanceMethodArgumentNode: %s\n", toInstanceMethodArgumentNode);

    boundClassNames.forEach(
        boundClassName -> {
          DependencyInjectionAnnotatedTypeFactory.knownBindings.remove(boundClassName);
          // Class that is being bound to - put in knownBindings
          // <bound,boundTo>
          TypeMirror boundToClassName = toInstanceMethodArgumentNode.getType();

          KnownBindingsValue knowBindingValue =
              KnownBindingsValue.builder()
                  .className(boundToClassName.toString())
                  .annotationName(annotatedNames.get(0))
                  .build();

          DependencyInjectionAnnotatedTypeFactory.knownBindings.put(
              boundClassName, knowBindingValue);
        });
  }

  /**
   * Invoked when the annotation of the receiver to the {@code toInstance} method is {@code Bind}
   *
   * @param bindAnno the {@code Bind} annotation
   * @param toInstanceMethodArgumentNode the argument to the {@code toInstance} method
   */
  private void handleBindAnnotation(AnnotationMirror bindAnno, Node toInstanceMethodArgumentNode) {

    List<String> boundClassNames =
        AnnotationUtils.getElementValueArray(bindAnno, bindValValueElement, String.class);

    boundClassNames.forEach(
        boundClassName -> {
          DependencyInjectionAnnotatedTypeFactory.knownBindings.remove(boundClassName);
          // Class that is being bound to - put in knownBindings
          // <bound,boundTo>
          TypeMirror boundToClassName = toInstanceMethodArgumentNode.getType();

          DependencyInjectionAnnotatedTypeFactory.knownBindings.put(
              boundClassName,
              KnownBindingsValue.builder().className(boundToClassName.toString()).build());
        });
  }

  /**
   * Invoked when the {@code MethodInvocationNode} is a call to {@code
   * com.google.inject.binder.LinkedBindingBuilder.toInstance}
   *
   * <p>This method will attempt to find the class that is being bound and add it to the {@code
   * knownBindings} map alongside the class that is being bound to
   *
   * <p>If the receiver is a call to {@code
   * com.google.inject.binder.AnnotatedBindingBuilder.annotatedWith}, a call to {@code
   * handleAnnotatedWithMethod} will be made
   *
   * @param currentNode the current node in the block
   * @param methodAccessNode the method access node
   * @param toInstanceMethodArgumentNode the argument to the{@code to} method
   */
  private void handleToInstanceMethodInvocation(
      Node currentNode, MethodAccessNode methodAccessNode, Node toInstanceMethodArgumentNode) {

    System.out.printf("Current node: %s\n", currentNode);

    Node receiver = methodAccessNode.getReceiver();

    System.out.printf("Current receiver: %s\n", receiver);

    CFValue value = this.getStoreBefore(currentNode).getValue(JavaExpression.fromNode(receiver));

    if (value != null && !value.getAnnotations().isEmpty()) {
      AnnotationMirrorSet annotations = value.getAnnotations();
      System.out.printf("Annotations: %s\n\n", annotations);
      annotations.forEach(
          (annotation) -> {
            if (AnnotationUtils.areSameByName(annotation, BindAnnotatedWith.NAME)) {
              handleBAWAnnotation(annotation, toInstanceMethodArgumentNode);
            } else if (AnnotationUtils.areSameByName(annotation, Bind.NAME)) {
              handleBindAnnotation(annotation, toInstanceMethodArgumentNode);
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
                    } else if (isToInstanceMethod(methodInvocationNode.getTree())) {
                      handleToInstanceMethodInvocation(node, methodAccessNode, methodArgumentNode);
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

    super.postAnalyze(cfg);
  }

  @Override
  public TreeAnnotator createTreeAnnotator() {
    return new ListTreeAnnotator(
        new DependencyInjectionTreeAnnotator(this), super.createTreeAnnotator());
  }

  public class DependencyInjectionTreeAnnotator extends TreeAnnotator {

    /**
     * Creates a ElementQualifierHierarchy from the given classes.
     *
     * @param qualifierClasses classes of annotations that are the qualifiers for this hierarchy
     * @param elements element utils
     */
    public DependencyInjectionTreeAnnotator(AnnotatedTypeFactory annotatedTypeFactory) {
      super(annotatedTypeFactory);
    }

    @Override
    public Void visitMethod(MethodTree tree, AnnotatedTypeMirror p) {
      ExecutableElement element = TreeUtils.elementFromDeclaration(tree);
      if (ElementUtils.hasAnnotation(element, Provides.class.getName())) {
        // TODO: Put fully qualified names of classes in knownBindings
        knownBindings.put(
            tree.getReturnType().toString(),
            KnownBindingsValue.builder().className(p.getUnderlyingType().toString()).build());
      }

      printKnownBindings();
      return super.visitMethod(tree, p);
    }
  }

  @Override
  protected QualifierHierarchy createQualifierHierarchy() {
    return new DependencyInjectionQualifierHierarchy(
        this.getSupportedTypeQualifiers(), this.elements);
  }

  protected class DependencyInjectionQualifierHierarchy extends AccumulationQualifierHierarchy {

    protected DependencyInjectionQualifierHierarchy(
        Collection<Class<? extends Annotation>> qualifierClasses, Elements elements) {
      super(qualifierClasses, elements);
    }

    @Override
    public boolean isSubtype(final AnnotationMirror subAnno, final AnnotationMirror superAnno) {

      if (AnnotationUtils.areSame(superAnno, top)) {
        return true;
      }

      if (AnnotationUtils.areSame(subAnno, superAnno)) {
        return true;
      }

      // TODO: Do we want to return false if superAnno is BAW all the time?
      if (AnnotationUtils.areSameByName(subAnno, BindAnnotatedWith.NAME)
          || AnnotationUtils.areSameByName(superAnno, BindAnnotatedWith.NAME)) {
        return false;
      }

      return super.isSubtype(subAnno, superAnno);
    }

    @Override
    public @Nullable AnnotationMirror leastUpperBound(
        final AnnotationMirror qualifier1, final AnnotationMirror qualifier2) {

      if (AnnotationUtils.areSame(qualifier1, qualifier2)) {
        return qualifier1;
      }

      if (AnnotationUtils.areSameByName(qualifier1, BindAnnotatedWith.NAME)
          || AnnotationUtils.areSameByName(qualifier2, BindAnnotatedWith.NAME)) {
        return top;
      }

      return super.leastUpperBound(qualifier1, qualifier2);
    }

    @Override
    public @Nullable AnnotationMirror greatestLowerBound(
        AnnotationMirror qualifier1, AnnotationMirror qualifier2) {

      if (AnnotationUtils.areSame(qualifier1, qualifier2)) {
        return qualifier1;
      }

      if (AnnotationUtils.areSameByName(qualifier1, BindAnnotatedWith.NAME)
          || AnnotationUtils.areSameByName(qualifier2, BindAnnotatedWith.NAME)) {
        return bottom;
      }

      return super.greatestLowerBound(qualifier1, qualifier2);
    }
  }

  public AnnotationMirror createBindAnnotatedWithAnnotation(String value, String name) {
    AnnotationBuilder builder = new AnnotationBuilder(processingEnv, BindAnnotatedWith.class);
    builder.setValue("value", Collections.singletonList(value));
    builder.setValue("annotatedWith", Collections.singletonList(name));
    return builder.build();
  }
}
