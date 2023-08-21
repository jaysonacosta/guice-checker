import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import org.checkerframework.common.value.qual.*;

public class LinkedBindings {

  /** Guice module that provides bindings for message and count used in {@link Greeter}. */
  static class BillingModule extends AbstractModule {
    @Provides
    TransactionLog provideTransactionLog(DatabaseTransactionLog impl) {
      return impl;
    }
  }

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
    Injector injector = Guice.createInjector(new BillingModule());

    DatabaseTransactionLog baz =
        (DatabaseTransactionLog) injector.getInstance(TransactionLog.class);
  }
}
