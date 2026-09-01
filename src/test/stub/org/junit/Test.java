// Minimal org.junit.Test annotation stub so the test sources can carry the
// @Test marker (and therefore integrate with JUnit-based tooling) while the
// dependency-free `build.sh` path can still compile the suite without any
// external jars. The stub lives outside Maven's source roots, so it is never
// compiled by Maven/Gradle — those build tools use the real JUnit 4 test
// dependency declared in pom.xml. No production code references JUnit.
//
// Author: S1
// GitHub: github.com/JokerPython3
// Language: Java
package org.junit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Test {
}