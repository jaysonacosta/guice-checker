import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provides;
import java.lang.annotation.Retention;
import javax.inject.Qualifier;
import org.checkerframework.common.value.qual.*;

public class MissingImplementation {
  @Qualifier @Retention(RUNTIME)
  @interface Message {}

  @Qualifier @Retention(RUNTIME)
  @interface Count {}

  /** Guice module that provides bindings for message and count used in {@link Greeter}. */
  static class DemoModule extends AbstractModule {
    @Provides
    @Count
    static Integer provideCount() {
      return 3;
    }

    @Provides
    @Message
    static String provideMessage() {
      return "hello world";
    }
  }

  static class Greeter {
    private final String message;
    private final int count;

    // Greeter declares that it needs a string message and an integer
    // representing the number of time the message to be printed.
    // The @Inject annotation marks this constructor as eligible to be used by
    // Guice.
    @Inject
    Greeter(@Message String message, @Count int count) {
      this.message = message;
      this.count = count;
    }

    void sayHello() {
      for (int i = 0; i < count; i++) {
        System.out.println(message);
      }
    }

    String getMessage() {
      return message;
    }
  }

  public static void main(String args[]) {
    /*
     * Guice.createInjector() takes one or more modules, and returns a new Injector
     * instance. Most applications will call this method exactly once, in their
     * main() method.
     */
    Injector injector = Guice.createInjector();

    // :: error: missing.implementation
    Greeter greeter = injector.getInstance(Greeter.class);
  }
}
