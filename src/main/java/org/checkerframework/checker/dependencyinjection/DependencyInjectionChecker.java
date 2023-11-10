package org.checkerframework.checker.dependencyinjection;

import java.util.Map;
import javax.lang.model.element.Element;
import org.checkerframework.checker.dependencyinjection.utils.KnownBindingsValue;
import org.checkerframework.common.accumulation.AccumulationChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.qual.StubFiles;

/** This is the entry point for pluggable type-checking. */
@StubFiles({"Guice.astub"})
public class DependencyInjectionChecker extends AccumulationChecker {

  public DependencyInjectionChecker() {}

  @Override
  protected BaseTypeVisitor<?> createSourceVisitor() {
    return new DependencyInjectionVisitor(this);
  }

  @Override
  public void typeProcessingOver() {
    Map<String, KnownBindingsValue> knownBindings =
        DependencyInjectionAnnotatedTypeFactory.getKnownBindings();
    Map<String, Element> injectionPoints =
        DependencyInjectionAnnotatedTypeFactory.getInjectionPoints();

    injectionPoints
        .entrySet()
        .forEach(
            (injectionPoint) -> {
              if (!knownBindings.containsKey(injectionPoint.getKey())) {
                reportError(
                    injectionPoint.getValue(), "missing.implementation", injectionPoint.getKey());
              }
            });

    DependencyInjectionAnnotatedTypeFactory.printKnownBindings();
    DependencyInjectionAnnotatedTypeFactory.printDependencies();

    super.typeProcessingOver();
  }
}
