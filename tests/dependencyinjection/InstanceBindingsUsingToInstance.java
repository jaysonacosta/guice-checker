import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

public class InstanceBindingsUsingToInstance {
  /** Guice module that provides bindings for message and count used in {@link Greeter}. */
  static class DemoModule extends AbstractModule {
    @Override
    protected void configure() {
      bind(String.class).toInstance("jdbc:mysql://localhost/pizza");
      bind(Integer.class).annotatedWith(Names.named("login timeout seconds")).toInstance(10);
    }
  }

  static class Database {
    private String url;
    private int timeout;

    @Inject
    public Database(String url, @Named("login timeout seconds") int timeout) {
      this.url = url;
      this.timeout = timeout;
    }

    public String getUrl() {
      return url;
    }

    public int getTimeout() {
      return timeout;
    }
  }

  public static void main(String args[]) {
    /*
     * Guice.createInjector() takes one or more modules, and returns a new Injector
     * instance. Most applications will call this method exactly once, in their
     * main() method.
     */
    Injector injector = Guice.createInjector(new DemoModule());

    Database database = injector.getInstance(Database.class);
  }
}
