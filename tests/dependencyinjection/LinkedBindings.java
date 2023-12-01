import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import org.checkerframework.common.value.qual.*;

/**
 * Linked bindings map a type to its implementation. {@link
 * https://github.com/google/guice/wiki/LinkedBindings}
 */
public class LinkedBindings {

  static class BillingModuleContainingBinding extends AbstractModule {
    @Provides
    TransactionLog provideTransactionLog(DatabaseTransactionLog impl) {
      return impl;
    }
  }

  static class BillingModuleWithoutBinding extends AbstractModule {}

  interface TransactionLog {
    public void foo();
  }

  class DatabaseTransactionLog implements TransactionLog {
    public void foo() {
      System.out.println("LinkedBindings.DatabaseTransactionLog.foo()");
    }
  }

  public static void main(String args[]) {
    /*
     * Guice.createInjector() takes one or more modules, and returns a new Injector
     * instance. Most applications will call this method exactly once, in their
     * main() method.
     */
    Injector injectorWithBinding = Guice.createInjector(new BillingModuleContainingBinding());
    Injector injectorWithoutBinding = Guice.createInjector(new BillingModuleWithoutBinding());

    /*
     * When you call injector.getInstance(TransactionLog.class), or when the injector
     * encounters a dependency on TransactionLog, it will use a DatabaseTransactionLog.
     */
    DatabaseTransactionLog baz =
        (DatabaseTransactionLog) injectorWithBinding.getInstance(TransactionLog.class);

    // :: error: missing.implementation
    DatabaseTransactionLog bar =
        (DatabaseTransactionLog) injectorWithoutBinding.getInstance(TransactionLog.class);
  }
}
