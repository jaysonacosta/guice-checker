package org.checkerframework.checker.caninject;

import com.sun.source.tree.Tree;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import org.checkerframework.checker.caninject.qual.CanInject;
import org.checkerframework.checker.caninject.qual.CanInjectBottom;
import org.checkerframework.common.accumulation.AccumulationAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.javacutil.TreeUtils;

public class CanInjectAnnotatedTypeFactory extends AccumulationAnnotatedTypeFactory {

  /** The fully-qualified name of the {@link com.google.inject.Guice} class */
  private final String guiceName = "com.google.inject.Guice";

  /** The {@link com.google.inject.Guice#createInjector method */
  private final List<ExecutableElement> createInjectorMethods = new ArrayList<>(4);

  /** Helper method that initializes Guice method elements */
  private void initializeMethodElements() {
    ProcessingEnvironment processingEnv = this.getProcessingEnv();
    this.createInjectorMethods.add(
        TreeUtils.getMethod(
            guiceName, "createInjector", processingEnv, "com.google.inject.Module[]"));
    this.createInjectorMethods.add(
        TreeUtils.getMethod(
            guiceName,
            "createInjector",
            processingEnv,
            "java.lang.Iterable<? extends com.google.inject.Module>"));
    this.createInjectorMethods.add(
        TreeUtils.getMethod(
            guiceName,
            "createInjector",
            processingEnv,
            "com.google.inject.Stage",
            "com.google.inject.Module[]"));
    this.createInjectorMethods.add(
        TreeUtils.getMethod(
            guiceName,
            "createInjector",
            processingEnv,
            "com.google.inject.Stage",
            "java.lang.Iterable<? extends com.google.inject.Module>"));
  }

  public CanInjectAnnotatedTypeFactory(BaseTypeChecker checker) {
    super(checker, CanInject.class, CanInjectBottom.class);
    this.initializeMethodElements();
    this.postInit();
  }

  /**
   * Returns true iff the argument is an invocation of {@link
   * com.google.inject.Guice#createInjector}.
   *
   * @param methodTree the method invocation tree
   * @return true iff the argument is an invocation of {@link
   *     com.google.inject.Guice#createInjector}
   */
  protected boolean isCreateInjectorMethod(Tree methodTree) {
    return TreeUtils.isMethodInvocation(
        methodTree, this.createInjectorMethods, this.getProcessingEnv());
  }
}
