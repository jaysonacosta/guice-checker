# Guice Checker

One common challenge is dealing with dependencies and ensuring they are properly defined. Omitting or misconfiguring dependencies can lead to runtime errors, impacting the reliability and stability of code.

The Guice Checker is a static analysis tool designed to address this issue by providing compile-time guarantees that your Guice dependency mappings are accurately defined.

## How to run the checker

First, publish the checker to your local Maven repository by running
`./gradlew publishToMavenLocal` in this repositor               y.

Then, if you use Gradle, add the following to the `build.gradle` file in
the project you wish to type-check (using Maven is similar):

```
repositories {
    mavenLocal()
    mavenCentral()
}
dependencies {
    annotationProcessor 'org.checkerframework:dependencyinjection-checker:0.1-SNAPSHOT'
}
```

Now, when you build your project, the Dependency Injection Checker will also run,
informing you of any potential errors related to TODO.


## How to specify your code

At compile time, the Guice Checker estimates what bindings the program may compute at run time.  It issues a warning if the program attempts to request a binding that has not been properly defined or configured. It works via a technique called pluggable typechecking.

You specify your code by writing *qualifiers* such as `@BindBottom` on types, to indicate more precisely what values the type represents. Here are the type qualifiers that are supported by the Guice Checker:

`@Bind`

- The value represents a class that has definitely been passed as an argument to a call to `com.google.inject.AbstractModule.bind`. It also represents the fact that no classes may have been passed to `bind`. This is the default type, so programmers usually do not need to write it.

`@BindAnnotatedWith`

- The value represents a class and annotation name that has definitely been passed as an arugment to a call to `com.google.inject.binder.AnnotatedBindingBuilder.annotatedWith` on an `AnnotatedBindingBuilder`. This value cannot be used on its own, meaning it is derived from the `@Bind` annotation. This is because the `.annotatedWith` is a method that is defined in the `AnnotatedBindingBuilder` class.

`@BindBottom`

-

## How to build the checker

Run these commands from the top-level directory.

`./gradlew build`: build the checker

`./gradlew publishToMavenLocal`: publish the checker to your local Maven repository.
This is useful for testing before you publish it elsewhere, such as to Maven Central.


## More information

The Dependency Injection Checker is built upon the Checker Framework.  Please see
the [Checker Framework Manual](https://checkerframework.org/manual/) for
more information about using pluggable type-checkers, including this one.
