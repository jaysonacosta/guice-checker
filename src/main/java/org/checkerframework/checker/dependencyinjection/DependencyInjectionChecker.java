package org.checkerframework.checker.dependencyinjection;

import java.util.LinkedHashSet;
import java.util.Map;
import org.checkerframework.checker.caninject.CanInjectChecker;
import org.checkerframework.checker.dependencyinjection.utils.Module;
import org.checkerframework.common.accumulation.AccumulationChecker;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.common.reflection.ClassValChecker;
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
  protected LinkedHashSet<Class<? extends BaseTypeChecker>> getImmediateSubcheckerClasses() {
    LinkedHashSet<Class<? extends BaseTypeChecker>> checkers =
        super.getImmediateSubcheckerClasses();
    checkers.add(ClassValChecker.class);
    checkers.add(CanInjectChecker.class);

    return checkers;
  }

  @Override
  public void typeProcessingOver() {
    Map<String, Module> modules = DependencyInjectionAnnotatedTypeFactory.getModules();
    modules.forEach(
        (moduleName, module) -> {
          System.out.println(moduleName);
          module
              .getBindings()
              .forEach(
                  (dependencyName, knownBindingsValue) -> {
                    System.out.println("\t" + dependencyName + " -> " + knownBindingsValue);
                  });
          System.out.println();
        });
    // Map<String, KnownBindingsValue> knownBindings =
    //     DependencyInjectionAnnotatedTypeFactory.getModules();
    // Map<String, Element> injectionPoints =
    //     DependencyInjectionAnnotatedTypeFactory.getInjectionPoints();

    // injectionPoints
    //     .entrySet()
    //     .forEach(
    //         (injectionPoint) -> {
    //           if (!knownBindings.containsKey(injectionPoint.getKey())) {
    //             reportError(
    //                 injectionPoint.getValue(), "missing.implementation",
    // injectionPoint.getKey());
    //           }
    //         });

    // DependencyInjectionAnnotatedTypeFactory.printKnownBindings();
    // DependencyInjectionAnnotatedTypeFactory.printDependencies();

    super.typeProcessingOver();
  }
}
