package org.philzen.oss.testng;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class MigrateAssertEqualsDeepTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new MigrateAssertEqualsDeep())
          .parser(JavaParser.fromJavaVersion().classpath(JavaParser.runtimeClasspath()));
    }
    
    @Nested class WithoutErrorMessage {
        
        @Test void map() {
            // language=java
            rewriteRun(java(
                """
                import java.util.Map;
                import org.testng.Assert;
                
                class MyTest {
                    void testMethod() {
                        Assert.assertEqualsDeep(Map.of("actual", 12), Map.of("expected", 11));
                    }
                }
                """, """
                import org.junit.jupiter.api.Assertions;
                
                import java.util.AbstractMap;
                import java.util.Arrays;
                import java.util.Map;
                import java.util.stream.Collectors;
                
                class MyTest {
                    void testMethod() {
                        Assertions.assertIterableEquals(
                                Map.of("expected", 11).entrySet().stream().map(
                                        entry -> entry.getValue() == null || !entry.getValue().getClass().isArray() ? entry
                                                // convert array to List as the assertion needs an Iterable for proper comparison
                                                : new AbstractMap.SimpleEntry<>(entry.getKey(), Arrays.asList(Object[].class.cast(entry.getValue())))
                                ).collect(Collectors.toSet()),
                                Map.of("actual", 12).entrySet().stream().map(
                                        entry -> entry.getValue() == null || !entry.getValue().getClass().isArray() ? entry
                                                // convert array to List as the assertion needs an Iterable for proper comparison
                                                : new AbstractMap.SimpleEntry<>(entry.getKey(), Arrays.asList(Object[].class.cast(entry.getValue())))
                                ).collect(Collectors.toSet())
                        );
                    }
                }
                """
            ));
        }
    }
}
