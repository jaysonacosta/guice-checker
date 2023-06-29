import org.checkerframework.checker.dependencyinjection.qual.*;

// Test basic subtyping relationships for the Dependency Injection Checker.
class SubtypeTest {
    void allSubtypingRelationships(@DependencyInjectionUnknown int x, @DependencyInjectionBottom int y) {
        @DependencyInjectionUnknown int a = x;
        @DependencyInjectionUnknown int b = y;
        // :: error: assignment
        @DependencyInjectionBottom int c = x; // expected error on this line
        @DependencyInjectionBottom int d = y;
    }
}
