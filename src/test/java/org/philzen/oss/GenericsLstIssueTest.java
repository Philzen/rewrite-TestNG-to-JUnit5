package org.philzen.oss;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class GenericsLstIssueTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new GenericsLstIssue())
          .parser(JavaParser.fromJavaVersion().classpath(JavaParser.runtimeClasspath()));
    }

    @Test void javaTemplateNotMatched_example1() {
        // language=java
        rewriteRun(java(
          """
            import java.util.Map;
            import org.philzen.oss.SomeClassWithGenericMethod;
            
            class MyTest {
                void testMethod() {
                    SomeClassWithGenericMethod.someMethod(Map.of("key", new Object[] {1,2,3}));
                }
            }
            """, 
            """
            import java.util.Map;
            import org.philzen.oss.SomeClassWithGenericMethod;
            
            class MyTest {
                void testMethod() {
                    SomeClassWithGenericMethod.someSimilarMethod(Map.of("key", new Object[] {1,2,3}));
                }
            }
            """
        ));
    }
    
    @Test void javaTemplateNotMatched_example2() {
        // language=java
        rewriteRun(java(
          """
            import java.util.Map;
            import org.philzen.oss.SomeClassWithGenericMethod;
            
            class MyTest {
                void testMethod() {
                    SomeClassWithGenericMethod.someMethod(Map.of("key", new int[] {1,2,3}));
                }
            }
            """, 
            """
            import java.util.Map;
            import org.philzen.oss.SomeClassWithGenericMethod;
            
            class MyTest {
                void testMethod() {
                    SomeClassWithGenericMethod.someSimilarMethod(Map.of("key", new int[] {1,2,3}));
                }
            }
            """
        ));
    }
    
    @Test void lstError_example1_methodInvocationType() {
        // language=java
        rewriteRun(java(
          """
            import java.util.Map;
            import org.philzen.oss.SomeClassWithGenericMethod;
            
            class MyTest {
                void testMethod() {
                    SomeClassWithGenericMethod.someMethod(Map.of("key", "value"));
                }
            }
            """, 
            """
            import java.util.Map;
            import org.philzen.oss.SomeClassWithGenericMethod;
            
            class MyTest {
                void testMethod() {
                    SomeClassWithGenericMethod.someSimilarMethod(Map.of("key", "value"));
                }
            }
            """
        ));
    }

    @Test void lstError_example2_methodInvocationType() {
        // language=java
        rewriteRun(java(
          """
            import java.util.Collections;
            import org.philzen.oss.SomeClassWithGenericMethod;
            
            class MyTest {
                void testMethod() {
                    SomeClassWithGenericMethod.someMethod(Collections.singletonMap("key", "value"));
                }
            }
            """,
            """
            import java.util.Collections;
            import org.philzen.oss.SomeClassWithGenericMethod;
            
            class MyTest {
                void testMethod() {
                    SomeClassWithGenericMethod.someSimilarMethod(Collections.singletonMap("key", "value"));
                }
            }
            """
        ));
    }
    
    @Test void lstError_example3_methodInvocationType() {
        // language=java
        rewriteRun(java(
          """
            import java.util.Map;
            import java.util.Collections;
            import org.philzen.oss.SomeClassWithGenericMethod;
            
            class MyTest {
                void testMethod() {
                    Map<String, String> map = Collections.singletonMap("key", "value");
                    SomeClassWithGenericMethod.someMethod(map);
                }
            }
            """,
            """
            import java.util.Map;
            import java.util.Collections;
            import org.philzen.oss.SomeClassWithGenericMethod;
            
            class MyTest {
                void testMethod() {
                    Map<String, String> map = Collections.singletonMap("key", "value");
                    SomeClassWithGenericMethod.someSimilarMethod(map);
                }
            }
            """
        ));
    }
}
