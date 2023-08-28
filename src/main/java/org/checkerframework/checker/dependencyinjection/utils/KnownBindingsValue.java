package org.checkerframework.checker.dependencyinjection.utils;

/** A class that represents another class that is known to be bound in a Guice module. */
public class KnownBindingsValue {
  /** The class that has been bound. This is a required property. */
  private String className;

  /** The name that the class has been bound under. This is an optional property. */
  private String annotationName;

  public String getClassName() {
    return className;
  }

  public String getAnnotationName() {
    return annotationName;
  }

  private KnownBindingsValue(KnownBindingsValueBuilder builder) {
    this.className = builder.className;
    this.annotationName = builder.annotationName;
  }

  public String toString() {
    return String.format("(Class Name: %s, Annotation Name: %s)", className, annotationName);
  }

  /** Builder class for creating instances of {@code KnownBindingsValue} */
  public static class KnownBindingsValueBuilder {
    // Required parameters
    private final String className;

    // Optional parameters
    private String annotationName;

    public KnownBindingsValueBuilder(String className) {
      this.className = className;
    }

    public KnownBindingsValueBuilder setAnnotationName(String annotationName) {
      this.annotationName = annotationName;
      return this;
    }

    public KnownBindingsValue build() {
      return new KnownBindingsValue(this);
    }
  }
}
