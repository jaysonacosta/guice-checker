package org.checkerframework.checker.dependencyinjection.utils;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/** A class that represents another class that is known to be bound in a Guice module. */
@Builder
@ToString
public class KnownBindingsValue {
  /** The class that has been bound. This is a required property. */
  @Getter @lombok.NonNull private String className;

  /** The name that the class has been bound under. This is an optional property. */
  @Getter private String annotationName;
}
