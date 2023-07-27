package dependencyinjection;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import javax.inject.Inject;

import org.checkerframework.common.value.qual.*;

public class SimpleMissingImplementation {

  /**
   * Guice module that provides bindings for message and count used in
   * {@link Greeter}.
   */
  static class DemoModule extends AbstractModule {
    @Override
    protected void configure() {
      bind(Baz.class).to(BazImpl.class);
    }
  }

  interface Baz {
    public void foo();
  }

  class BazImpl implements Baz {
    public void foo() {
      System.out.println("GuiceDemo.BazImpl.foo()");
    }
  }

  public static void main(String args[]) {
    /*
     * Guice.createInjector() takes one or more modules, and returns a new Injector
     * instance. Most applications will call this method exactly once, in their
     * main() method.
     */
    Injector injector = Guice.createInjector(new DemoModule());
    // :: error: MISSING_IMPLEMENTATION
    Baz baz = injector.getInstance(BazImpl.class);
  }
}