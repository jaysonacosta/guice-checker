package org.checkerframework.checker.dependencyinjection;

import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.common.accumulation.AccumulationVisitor;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.javacutil.TreeUtils;

public class DependencyInjectionVisitor extends AccumulationVisitor {

  public DependencyInjectionVisitor(BaseTypeChecker c) {
    super(c);
  }

  @Override
  protected DependencyInjectionAnnotatedTypeFactory createTypeFactory() {
    return new DependencyInjectionAnnotatedTypeFactory(this.checker);
  }

  @Override
  public Void visitMethod(MethodTree tree, Void p) {
    ExecutableElement element = TreeUtils.elementFromDeclaration(tree);
    AnnotationMirror annotation =
        this.getTypeFactory().getDeclAnnotation(element, com.google.inject.Inject.class);

    if (annotation != null) {
      DependencyInjectionAnnotatedTypeFactory.addInjectionPoint(annotation.toString(), element);
    }
    return super.visitMethod(tree, p);
  }

  @Override
  public Void visitVariable(VariableTree tree, Void p) {
    VariableElement element = TreeUtils.elementFromDeclaration(tree);

    AnnotationMirror annotation =
        this.getTypeFactory().getDeclAnnotation(element, com.google.inject.Inject.class);

    if (annotation != null) {
      TypeMirror elementTypeMirror = element.asType();

      DependencyInjectionAnnotatedTypeFactory.addInjectionPoint(
          elementTypeMirror.toString(), element);
    }
    return super.visitVariable(tree, p);
  }
}
