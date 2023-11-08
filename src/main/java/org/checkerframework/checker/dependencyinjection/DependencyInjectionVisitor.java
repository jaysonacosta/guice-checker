package org.checkerframework.checker.dependencyinjection;

import com.sun.source.tree.MethodTree;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
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
      DependencyInjectionAnnotatedTypeFactory.addDependency(annotation.toString(), tree);
    }
    return super.visitMethod(tree, p);
  }
}
