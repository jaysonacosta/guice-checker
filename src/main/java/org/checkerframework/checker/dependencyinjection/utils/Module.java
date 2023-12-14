package org.checkerframework.checker.dependencyinjection.utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A class that represents a {@link com.google.inject.AbstractModule} defined in the program as a
 * map of its bindings.
 */
public class Module {
  /**
   * The map of <a href="https://github.com/google/guice/wiki/Bindings#bindings">bindings</a> that
   * the program may compute at run time for this particular module. Bindings are configured in
   * {@link com.google.inject.AbstractModule}. If a dependency does exist in this map, it has
   * definitely been properly defined or configured. Otherwise, it may or may not have been properly
   * defined or configured.
   *
   * <p>The key is the fully-qualified class name of the class being bound.
   *
   * <p>The value is the {@link KnownBindingsValue} that represents the class that has been bound
   * to.
   */
  private HashMap<String, KnownBindingsValue> bindings;

  public Module() {
    this.bindings = new HashMap<>();
  }

  /**
   * Adds a known binding to the map of bindings.
   *
   * @param dependencyName the fully-qualified class name of the dependency
   * @param knownBindingsValue the value of the known binding, containing the fully-qualified class
   *     name of the class that the dependency is bound to and the optional annotation name
   */
  public void addBinding(String dependencyName, KnownBindingsValue knownBindingsValue) {
    this.bindings.put(dependencyName, knownBindingsValue);
  }

  /**
   * Removes a known binding from the map of bindings.
   *
   * @param dependencyName the fully-qualified class name of the dependency
   */
  public void removeBinding(String dependencyName) {
    this.bindings.remove(dependencyName);
  }

  /**
   * Returns the map of bindings.
   *
   * @return the map of bindings
   */
  public Map<String, KnownBindingsValue> getBindings() {
    return Collections.unmodifiableMap(this.bindings);
  }

  public boolean containsBinding(String dependencyName) {
    return this.bindings.containsKey(dependencyName);
  }
}
