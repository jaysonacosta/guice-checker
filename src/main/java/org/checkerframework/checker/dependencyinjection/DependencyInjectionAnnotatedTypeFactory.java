package org.checkerframework.checker.dependencyinjection;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import java.lang.annotation.Annotation;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import org.checkerframework.checker.dependencyinjection.qual.Bind;
import org.checkerframework.checker.dependencyinjection.qual.BindAnnotatedWith;
import org.checkerframework.checker.dependencyinjection.qual.BindBottom;
import org.checkerframework.checker.dependencyinjection.utils.KnownBindingsValue;
import org.checkerframework.checker.dependencyinjection.utils.Module;
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
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationMirrorSet;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreePathUtil;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypeKindUtils;

public class DependencyInjectionAnnotatedTypeFactory extends AccumulationAnnotatedTypeFactory {

  /** The fully-qualified name of {@code AbstractModule} */
  private final String abstractModuleName = "com.google.inject.AbstractModule";

  /** The fully-qualified name of {@code LinkedBindingBuilder} */
  private final String linkedBindingBuilderName = "com.google.inject.binder.LinkedBindingBuilder";

  /** The fully-qualified name of {@code AnnotatedBindingBuilder} */
  private final String annotatedBindingBuilderName =
      "com.google.inject.binder.AnnotatedBindingBuilder";

  /**
   * The map of <a
   * href="https://google.github.io/guice/api-docs/7.0.0/javadoc/com/google/inject/AbstractModule.html">modules</a>,
   * that may contain <a href="https://github.com/google/guice/wiki/Bindings#bindings">bindings</a>,
   * that the program may compute at run time.
   *
   * <p>The key is the fully-qualified name of the module that has been defined in the program.
   *
   * <p>The value is the {@link Module} that represents the bindings that have been defined in the
   * module.
   */
  private static HashMap<String, Module> modules = new HashMap<>();

  /**
   * The map of <a href="https://github.com/google/guice/wiki/Injections#injection-points">injection
   * points</a> that Guice must be able to satisfy. These injection points are declared through
   * annotations such as {@link com.google.inject.Inject}.
   *
   * <p>The key is the fully-qualified class name of the dependency, and the value is the program
   * element that will be used to report an error if the dependency has no corresponding binding.
   */
  private static HashMap<String, Element> injectionPoints = new HashMap<>();

  /** The {@code com.google.inject.AbstractModule.bind(Class<Baz> clazz)} method */
  private final List<ExecutableElement> bindMethods = new ArrayList<>(3);

  /** The {@code com.google.inject.binder.LinkedBindingBuilder.to(Class<? extends Baz> method */
  private final List<ExecutableElement> toMethods = new ArrayList<>(3);

  /** The {@code com.google.inject.binder.LinkedBindingBuilder.toInstance()} method */
  private final List<ExecutableElement> toInstanceMethods = new ArrayList<>(1);

  /** The {@code com.google.inject.binder.AnnotatedBindingBuilder.annotatedWith()} method */
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

  /** Debugging method that pretty prints a map */
  private static void printMap(Map<String, ?> map, String mapName) {
    System.out.println(mapName);
    map.forEach(
        (key, value) -> {
          System.out.print(String.format("<Key: %s, Value: %s>\n", key, value));
        });
    System.out.println();
  }

  /** Debugging method that pretty prints {@link #modules} */
  protected static void printModules() {
    printMap(DependencyInjectionAnnotatedTypeFactory.modules, "modules");
  }

  /** Debugging method that pretty prints {@code printDependencies} */
  protected static void printDependencies() {
    printMap(DependencyInjectionAnnotatedTypeFactory.injectionPoints, "injectionPoints");
  }

  /**
   * Adds an injection point to the map of injection points.
   *
   * @param dependencyName the fully-qualified class name of the dependency
   * @param reportingLocation the program element at which an error will be reported if this
   *     injection point has no correspsonding binding
   */
  protected static void addInjectionPoint(String dependencyName, Element reportingLocation) {
    DependencyInjectionAnnotatedTypeFactory.injectionPoints.put(dependencyName, reportingLocation);
  }

  /**
   * Adds a module to the map of {@link #modules}.
   *
   * @param moduleName the fully-qualified class name of the module
   * @param module the value of the module
   */
  public static void addModule(String moduleName, Module module) {
    DependencyInjectionAnnotatedTypeFactory.modules.put(moduleName, module);
  }

  /**
   * Returns the module with the given name.
   *
   * @param moduleName the fully-qualified class name of the module
   * @return the module with the given name
   */
  public static Module getModule(String moduleName) {
    return DependencyInjectionAnnotatedTypeFactory.modules.get(moduleName);
  }

  /** Returns the map of {@link #modules}. */
  protected static Map<String, Module> getModules() {
    return Collections.unmodifiableMap(DependencyInjectionAnnotatedTypeFactory.modules);
  }

  /** Returns the map of injection points. */
  protected static Map<String, Element> getInjectionPoints() {
    return Collections.unmodifiableMap(DependencyInjectionAnnotatedTypeFactory.injectionPoints);
  }

  /**
   * Returns true iff the map of {@link #modules} contains a module with the given name.
   *
   * @param moduleName the fully-qualified class name of the module
   * @return true iff the map of {@link #modules} contains a module with the given name
   */
  public static boolean containsModule(String moduleName) {
    return DependencyInjectionAnnotatedTypeFactory.modules.containsKey(moduleName);
  }

  /** Helper method that initializes Guice method elements */
  private void initializeMethodElements() {
    ProcessingEnvironment processingEnv = this.getProcessingEnv();
    this.bindMethods.add(
        TreeUtils.getMethod(abstractModuleName, "bind", processingEnv, "com.google.inject.Key<T>"));
    this.bindMethods.add(
        TreeUtils.getMethod(
            abstractModuleName, "bind", processingEnv, "com.google.inject.TypeLiteral<T>"));
    this.bindMethods.add(
        TreeUtils.getMethod(abstractModuleName, "bind", processingEnv, "java.lang.Class<T>"));

    this.toMethods.add(
        TreeUtils.getMethod(
            linkedBindingBuilderName, "to", processingEnv, "java.lang.Class<? extends T>"));
    this.toMethods.add(
        TreeUtils.getMethod(
            linkedBindingBuilderName,
            "to",
            processingEnv,
            "com.google.inject.TypeLiteral<? extends T>"));
    this.toMethods.add(
        TreeUtils.getMethod(
            linkedBindingBuilderName, "to", processingEnv, "com.google.inject.Key<? extends T>"));

    this.toInstanceMethods.add(
        TreeUtils.getMethod(linkedBindingBuilderName, "toInstance", processingEnv, "T"));

    this.annotatedWithMethods.add(
        TreeUtils.getMethod(
            annotatedBindingBuilderName,
            "annotatedWith",
            processingEnv,
            "java.lang.annotation.Annotation"));
    this.annotatedWithMethods.add(
        TreeUtils.getMethod(
            annotatedBindingBuilderName,
            "annotatedWith",
            processingEnv,
            "java.lang.Class<? extends java.lang.annotation.Annotation>"));
  }

  public DependencyInjectionAnnotatedTypeFactory(BaseTypeChecker c) {
    super(c, Bind.class, BindBottom.class);
    this.initializeMethodElements();
    this.postInit();
  }

  /**
   * Returns the string representation of the given type mirror. If the type mirror is a primitive
   * or boxed type, the string representation is the type kind. Otherwise, the string representation
   * is the fully-qualified name of the type.
   *
   * @param typeMirror the type mirror
   * @return the string representation of the given type mirror
   */
  protected static String resolveInjectionPointClassName(TypeMirror typeMirror) {
    TypeKind injectionKind = TypeKindUtils.primitiveOrBoxedToTypeKind(typeMirror);
    return injectionKind != null ? injectionKind.toString() : typeMirror.toString();
  }

  /**
   * Returns the resolved name of the given injection point class name. If the given injection point
   * class name is a primitive or boxed type, the resolved name is the type kind. Otherwise, the
   * resolved name is the given injection point class name.
   *
   * @param injectionClassName the fully-qualified class name of the injection point
   * @return the resolved name of the given injection point class name
   */
  protected static String resolveInjectionPointClassName(String injectionClassName) {
    switch (injectionClassName) {
      case "java.lang.Byte":
        return TypeKind.BYTE.toString();
      case "java.lang.Boolean":
        return TypeKind.BOOLEAN.toString();
      case "java.lang.Character":
        return TypeKind.CHAR.toString();
      case "java.lang.Double":
        return TypeKind.DOUBLE.toString();
      case "java.lang.Float":
        return TypeKind.FLOAT.toString();
      case "java.lang.Integer":
        return TypeKind.INT.toString();
      case "java.lang.Long":
        return TypeKind.LONG.toString();
      case "java.lang.Short":
        return TypeKind.SHORT.toString();
      default:
        return injectionClassName;
    }
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

  private String getEnclosingQualifiedClassName(Tree tree) {
    TreePath currentPath = this.getPath(tree);
    ClassTree enclosingClass = TreePathUtil.enclosingClass(currentPath);
    TypeElement enclosingClassElement = TreeUtils.elementFromDeclaration(enclosingClass);
    return enclosingClassElement.getQualifiedName().toString();
  }

  /**
   * Invoked when a {@link MethodInvocationNode} is a call to {@link
   * com.google.inject.AbstractModule#bind}.
   *
   * <p>This method takes the {@link MethodInvocationNode} and adds it's argument, the bound class,
   * to it's corresponding {@code module} in the {@link #modules} map.
   *
   * @param methodInvocationNode the node for the {@link com.google.inject.AbstractModule#bind}
   *     method invocation
   */
  private void handleBindMethodInvocation(MethodInvocationNode methodInvocationNode) {
    Node methodArgumentNode = methodInvocationNode.getArgument(0);
    // Class that is being bound - put in knownBindings
    AnnotatedTypeMirror boundClassTypeMirror =
        getTypeFactoryOfSubchecker(ClassValChecker.class)
            .getAnnotatedType(methodArgumentNode.getTree());

    String enclosingModuleQualifiedName =
        getEnclosingQualifiedClassName(methodInvocationNode.getTree());

    // TODO: getAnnotation may possibly return annotations that aren't @ClassVal
    List<String> classNames =
        AnnotationUtils.getElementValueArray(
            boundClassTypeMirror.getAnnotation(), classValValueElement, String.class);

    classNames.forEach(
        className -> {
          if (!DependencyInjectionAnnotatedTypeFactory.containsModule(
              enclosingModuleQualifiedName)) {
            Module module = new Module();
            module.addBinding(resolveInjectionPointClassName(className), null);
            DependencyInjectionAnnotatedTypeFactory.addModule(enclosingModuleQualifiedName, module);
          } else {
            Module module =
                DependencyInjectionAnnotatedTypeFactory.getModule(enclosingModuleQualifiedName);
            module.addBinding(resolveInjectionPointClassName(className), null);
          }
        });
  }

  /**
   * Invoked when a {@link MethodInvocationNode} is a call to {@link
   * com.google.inject.binder.LinkedBindingBuilder#to}.
   *
   * <p>This method takes the {@link MethodInvocationNode} and binds it's argument, a class, to an
   * existing class in its corresponding {@code module} in the {@link #modules} map.
   *
   * @param methodInvocationNode the node for the {@link
   *     com.google.inject.binder.LinkedBindingBuilder#to} method invocation
   */
  private void handleToMethodInvocation(MethodInvocationNode methodInvocationNode) {
    MethodAccessNode methodAccessNode = methodInvocationNode.getTarget();
    Node methodArgumentNode = methodInvocationNode.getArgument(0);
    Node receiver = methodAccessNode.getReceiver();

    // Class that is being bound - should be in a module
    CFValue value =
        this.getStoreBefore(methodInvocationNode).getValue(JavaExpression.fromNode(receiver));

    AnnotationMirror bindAnno = null;
    if (value != null && !value.getAnnotations().isEmpty()) {
      for (AnnotationMirror anno : value.getAnnotations()) {
        if (AnnotationUtils.areSameByName(anno, Bind.NAME)) {
          bindAnno = anno;
          break;
        }
      }
    }

    String enclosingModuleQualifiedName =
        getEnclosingQualifiedClassName(methodInvocationNode.getTree());

    if (bindAnno != null) {
      List<String> boundClassNames =
          AnnotationUtils.getElementValueArray(bindAnno, bindValValueElement, String.class);

      boundClassNames.forEach(
          boundClassName -> {
            if (DependencyInjectionAnnotatedTypeFactory.containsModule(
                enclosingModuleQualifiedName)) {
              Module module =
                  DependencyInjectionAnnotatedTypeFactory.getModule(enclosingModuleQualifiedName);

              module.removeBinding(boundClassName);

              AnnotatedTypeMirror boundToClassTypeMirror =
                  getTypeFactoryOfSubchecker(ClassValChecker.class)
                      .getAnnotatedType(methodArgumentNode.getTree());

              List<String> boundToClassNames =
                  AnnotationUtils.getElementValueArray(
                      boundToClassTypeMirror.getAnnotation(), classValValueElement, String.class);

              boundToClassNames.forEach(
                  boundToClassName -> {
                    module.addBinding(
                        resolveInjectionPointClassName(boundClassName),
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
   * @param methodInvocationNode the argument to the {@code toInstance} method
   */
  private void handleBAWAnnotation(
      AnnotationMirror bawAnno, MethodInvocationNode methodInvocationNode) {
    Node methodArgumentNode = methodInvocationNode.getArgument(0);

    List<String> boundClassNames =
        AnnotationUtils.getElementValueArray(bawAnno, bawValValueElement, String.class);

    List<String> annotatedNames =
        AnnotationUtils.getElementValueArray(bawAnno, bawAnnotatedWithValueElement, String.class);

    if (annotatedNames.isEmpty()) {
      throw new IllegalArgumentException("BindAnnotatedWith annotation must have a value.");
    }

    String enclosingModuleQualifiedName =
        getEnclosingQualifiedClassName(methodInvocationNode.getTree());

    boundClassNames.forEach(
        boundClassName -> {
          Module module =
              DependencyInjectionAnnotatedTypeFactory.getModule(enclosingModuleQualifiedName);
          module.removeBinding(boundClassName);
          // Class that is being bound to - put in knownBindings
          // <bound,boundTo>
          TypeMirror boundToClassName = methodArgumentNode.getType();

          KnownBindingsValue knowBindingValue =
              KnownBindingsValue.builder()
                  .className(resolveInjectionPointClassName(boundToClassName))
                  .annotationName(annotatedNames.get(0))
                  .build();

          module.addBinding(resolveInjectionPointClassName(boundClassName), knowBindingValue);
        });
  }

  /**
   * Invoked when the annotation of the receiver to the {@code toInstance} method is {@code Bind}
   *
   * @param bindAnno the {@code Bind} annotation
   * @param methodInvocationNode the argument to the {@code toInstance} method
   */
  private void handleBindAnnotation(
      AnnotationMirror bindAnno, MethodInvocationNode methodInvocationNode) {
    Node methodArgumentNode = methodInvocationNode.getArgument(0);

    List<String> boundClassNames =
        AnnotationUtils.getElementValueArray(bindAnno, bindValValueElement, String.class);

    String enclosingModuleQualifiedName =
        getEnclosingQualifiedClassName(methodInvocationNode.getTree());

    boundClassNames.forEach(
        boundClassName -> {
          Module module =
              DependencyInjectionAnnotatedTypeFactory.getModule(enclosingModuleQualifiedName);
          module.removeBinding(boundClassName);
          // Class that is being bound to - put in knownBindings
          // <bound,boundTo>
          TypeMirror boundToClassName = methodArgumentNode.getType();

          module.addBinding(
              resolveInjectionPointClassName(boundClassName),
              KnownBindingsValue.builder().className(boundToClassName.toString()).build());
        });
  }

  /**
   * Invoked when a {@link MethodInvocationNode} is a call to {@link
   * com.google.inject.binder.LinkedBindingBuilder#toInstance}.
   *
   * <p>This method will attempt to find the class that is being bound and add it to its
   * corresponding module in the {@link #modules} map alongside the class that is being bound to.
   *
   * <p>If the receiver is a call to {@link
   * com.google.inject.binder.AnnotatedBindingBuilder#annotatedWith}, a call to {@link
   * handleAnnotatedWithMethod} will be made
   *
   * @param methodInvocationNode the node for the {@link
   *     com.google.inject.binder.LinkedBindingBuilder#toInstance} method invocation
   */
  private void handleToInstanceMethodInvocation(MethodInvocationNode methodInvocationNode) {
    MethodAccessNode methodAccessNode = methodInvocationNode.getTarget();
    Node receiver = methodAccessNode.getReceiver();

    CFValue value =
        this.getStoreBefore(methodInvocationNode).getValue(JavaExpression.fromNode(receiver));

    if (value != null && !value.getAnnotations().isEmpty()) {
      AnnotationMirrorSet annotations = value.getAnnotations();

      annotations.forEach(
          (annotation) -> {
            if (AnnotationUtils.areSameByName(annotation, BindAnnotatedWith.NAME)) {
              handleBAWAnnotation(annotation, methodInvocationNode);
            } else if (AnnotationUtils.areSameByName(annotation, Bind.NAME)) {
              handleBindAnnotation(annotation, methodInvocationNode);
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
                    if (isBindMethod(methodInvocationNode.getTree())) {
                      handleBindMethodInvocation(methodInvocationNode);
                    } else if (isToMethod(methodInvocationNode.getTree())) {
                      handleToMethodInvocation(methodInvocationNode);
                    } else if (isToInstanceMethod(methodInvocationNode.getTree())) {
                      handleToInstanceMethodInvocation(methodInvocationNode);
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
