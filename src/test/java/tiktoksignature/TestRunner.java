// Minimal test runner (no external JUnit dependency). Methods whose names begin
// with "test" are executed as individual tests. The same test methods are also
// discoverable by JUnit 4 (they are annotated with @Test) so `mvn test` works.
//
// Author: S1
// GitHub: github.com/JokerPython3
// Based on: Go port of SignerPy (Python)
// Language: Java
package tiktoksignature;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

public final class TestRunner {

    public static void main(String[] args) throws Exception {
        List<Class<?>> suites = new ArrayList<>();
        suites.add(UnitTest.class);
        suites.add(GoldenVectorTest.class);
        suites.add(RequestCompareTest.class);

        int ran = 0, failed = 0;
        List<String> failures = new ArrayList<>();
        for (Class<?> suite : suites) {
            System.out.println("=== " + suite.getSimpleName() + " ===");
            Object instance;
            try {
                instance = suite.getDeclaredConstructor().newInstance();
            } catch (NoSuchMethodException e) {
                System.out.println("  SKIP " + suite.getSimpleName() + ": no no-arg constructor");
                continue;
            }
            for (Method m : suite.getDeclaredMethods()) {
                if (m.getName().startsWith("test")
                        && Modifier.isPublic(m.getModifiers())
                        && m.getParameterCount() == 0) {
                    ran++;
                    try {
                        m.invoke(instance);
                        System.out.println("  PASS " + m.getName());
                    } catch (java.lang.reflect.InvocationTargetException e) {
                        failed++;
                        Throwable cause = e.getCause() != null ? e.getCause() : e;
                        System.out.println("  FAIL " + m.getName() + ": " + cause.getMessage());
                        failures.add(suite.getSimpleName() + "." + m.getName());
                        cause.printStackTrace(System.out);
                    }
                }
            }
        }
        System.out.println();
        System.out.println("Ran " + ran + " tests, " + failed + " failed.");
        if (!failures.isEmpty()) {
            System.out.println("Failures:");
            for (String f : failures) System.out.println("  - " + f);
            System.exit(1);
        }
    }
}
