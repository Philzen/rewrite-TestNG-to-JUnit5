package org.philzen.oss.testng;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class UpdateTestAnnotationToJunit5Test implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UpdateTestAnnotationToJunit5());
    }

    // TODO @Test(dataProvider = "…")
    // TODO @Test(enabled = false) → org.junit.jupiter.api.Disabled
    // TODO @Test(description = "…") → org.junit.jupiter.api.DisplayName
    // TODO inner `public static class` → `@org.junit.jupiter.api.Nested static class`
    // TODO @Test(groups = {"…"}) → org.junit.jupiter.api.Tag

    /*
     * TODO Nice-to-have:
     * - @Test(enabled = {CONSTANT_EXPRESSION}) → org.junit.jupiter.api.condition.EnabledIf
     * - @Test(dataProvider = "…", dataProviderClass = "…") → org.junit.jupiter.params.provider.ArgumentsSource
     * - what to do with @Test(priority = …)? → org.junit.jupiter.api.Order / TestClassOrder / TestMethodOrder ?
     * - @Factory → org.junit.jupiter.api.TestFactory
     */

    @Nested class NoAttributes {

        @Test void isMigratedToJunitTestAnnotationWithoutParameters() {
            rewriteRun(
              // language=java
              java(
                """
                package de.foo.bar;
                
                import org.testng.annotations.Test;
                
                public class BazTest {
    
                    @Test
                    public void shouldDoStuff() {
                        //
                    }
                }
                """,
                """
                package de.foo.bar;
                
                import org.junit.jupiter.api.Test;
                
                public class BazTest {
    
                    @Test
                    public void shouldDoStuff() {
                        //
                    }
                }
                """
              )
            );
        }

        @Test void isMigratedPreservingOtherAnnotationsAndComments() {
            // language=java
            rewriteRun(
              java(
                """
                package org.openrewrite;
                public @interface Issue {
                    String value();
                }
                """
              ),
              java(
                """
                import org.testng.annotations.Test;
                import org.openrewrite.Issue;
                
                public class MyTest {
                
                    // some comments
                    @Issue("some issue")
                    @Test
                    public void test() {
                    }
                
                    // some more comments
                    @Test
                    public void test1() {
                    }
                
                    @Test
                    // even more comments
                    public void test2() {
                    }
                }
                """,
                """
                import org.junit.jupiter.api.Test;
                import org.openrewrite.Issue;
                
                public class MyTest {
                
                    // some comments
                    @Issue("some issue")
                    @Test
                    public void test() {
                    }
                
                    // some more comments
                    @Test
                    public void test1() {
                    }
                
                    @Test
                    // even more comments
                    public void test2() {
                    }
                }
                """
              )
            );
        }

        @Test void isMigratedWhenReferencedAsVariable() {
            // language=java
            rewriteRun(
              java(
                """
                import org.testng.annotations.Test;
                public class MyTest {
                    Object o = Test.class;
                }
                """,
                """
                import org.junit.jupiter.api.Test;
                
                public class MyTest {
                    Object o = Test.class;
                }
                """
              )
            );
        }

        @Test void isMigrated_whenUsedInJavadoc() {
            // language=java
            rewriteRun(
              java(
                """
                import org.testng.annotations.Test;
    
                /** @see org.testng.annotations.Test */
                public class MyTest {
                    @Test
                    public void test() {
                    }
                }
                """,
                """
                import org.junit.jupiter.api.Test;
                
                /** @see org.junit.jupiter.api.Test */
                public class MyTest {
                    @Test
                    public void test() {
                    }
                }
                """
              )
            );
        }

        @Test void noTestAnnotationValues_sameLine_multipleImports() {
            rewriteRun(
              // language=java
              java(
                """
                package de.foo.bar;
                
                import java.util.List;
                import org.testng.annotations.Test;
                import static org.assertj.core.api.Assertions.assertThat;
                
                public class Baz {
    
                    @Test public void shouldDoStuff() {
                        //
                    }
                }
                """,
                """
                package de.foo.bar;
                
                import org.junit.jupiter.api.Test;
    
                import java.util.List;
                import static org.assertj.core.api.Assertions.assertThat;
                
                public class Baz {
    
                    @Test public void shouldDoStuff() {
                        //
                    }
                }
                """
              )
            );
        }

        @Test void fullyQualified() {
            rewriteRun(
              // language=java
              java(
                """
                package de.foo.bar;
                
                class Baz {
                
                    @org.testng.annotations.Test
                    public void shouldDoStuff() {
                        //
                    }
                }
                """,
                """
                package de.foo.bar;
                
                class Baz {
                
                    @org.junit.jupiter.api.Test
                    public void shouldDoStuff() {
                        //
                    }
                }
                """
              )
            );
        }

        @Test void fullyQualified_sameLineAsMethodDeclaration() {
            rewriteRun(
              // language=java
              java(
                """
                package de.foo.bar;
                
                class Baz {
                
                    @org.testng.annotations.Test public void shouldDoStuff() {
                        //
                    }
                }
                """,
                """
                package de.foo.bar;
                
                class Baz {
                
                    @org.junit.jupiter.api.Test public void shouldDoStuff() {
                        //
                    }
                }
                """
              )
            );
        }

        @Test void mixedFullyQualifiedAndNot() {
            rewriteRun(
              // language=java
              java(
                """
                import org.testng.annotations.Test;
                public class MyTest {
                    @org.testng.annotations.Test
                    public void feature1() {
                    }
                
                    @Test
                    public void feature2() {
                    }
                }
                """,
                """
                import org.junit.jupiter.api.Test;
                
                public class MyTest {
                    @org.junit.jupiter.api.Test
                    public void feature1() {
                    }
                
                    @Test
                    public void feature2() {
                    }
                }
                """
              )
            );
        }

        @SuppressWarnings("JUnitMalformedDeclaration")
        @Test void nestedClass() {
            rewriteRun(
              // language=java
              java(
                """
                import org.testng.annotations.Test;
                
                class Baz {
                
                    @Test public void shouldDoStuff() {
                        //
                    }
                
                    public class NestedGroupedTests {
                
                        @Test public void shouldDoStuff() {
                            //
                        }
                    }
                }
                """,
                """
                import org.junit.jupiter.api.Test;
                
                class Baz {
                
                    @Test public void shouldDoStuff() {
                        //
                    }
                
                    public class NestedGroupedTests {
                
                        @Test public void shouldDoStuff() {
                            //
                        }
                    }
                }
                """
              )
            );
        }

        @Test void noChangeNecessary() {
            rewriteRun(
              // language=java
              java(
                """
                package de.foo.bar;
                
                import org.junit.jupiter.api.Test;
                
                class Baz {
                
                    @Test public void shouldDoStuff() {
                        //
                    }
                }
                """
              )
            );
        }

        @Test void noChangeNecessary_fullyQualified() {
            rewriteRun(
              // language=java
              java(
                """
                package de.foo.bar;
                
                class Baz {
                
                    @org.junit.jupiter.api.Test public void shouldDoStuff() {
                        //
                    }
                }
                """
              )
            );
        }

        @Test void noChangeNecessary_nestedClass() {
            rewriteRun(
              // language=java
              java(
                """
                package de.foo.bar;
                
                import org.junit.jupiter.api.Nested;
                import org.junit.jupiter.api.Test;
                
                class Baz {
                
                    @Test public void shouldDoStuff() {
                        //
                    }
                
                    @Nested class NestedGroupedTests {
                
                        @Test public void shouldDoStuff() {
                            //
                        }
                    }
                }
                """
              )
            );
        }
    }

    @Nested class Attribute_expectedExceptions {

        @Test void isMigratedToBodyWrappedInAssertThrows_forLiteralJavaExceptionThrown() {
            // language=java
            rewriteRun(
              java(
                """
                import org.testng.annotations.Test;
                
                public class MyTest {
                
                  @Test(expectedExceptions = IllegalArgumentException.class)
                  public void test() {
                      throw new IllegalArgumentException("boom");
                  }
                }
                """,
                """
                import org.junit.jupiter.api.Test;
                
                import static org.junit.jupiter.api.Assertions.assertThrows;
                
                public class MyTest {
                
                  @Test
                  public void test() {
                      assertThrows(IllegalArgumentException.class, () -> {
                          throw new IllegalArgumentException("boom");
                      });
                  }
                }
                """
              )
            );
        }

        @Test void Attribute_expectedExceptionsMessageRegExp() {
            // language=java
            rewriteRun(
              java(
                """
                import org.testng.annotations.Test;
                
                public class MyTest {
                
                    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "boom.*!")
                    public void test() {
                        throw new IllegalArgumentException("boom     !");
                    }
                }
                """,
                """
                import org.junit.jupiter.api.Test;
                
                import static org.junit.jupiter.api.Assertions.assertThrows;
                import static org.junit.jupiter.api.Assertions.assertTrue;
                
                public class MyTest {
                
                    @Test
                    public void test() {
                        Throwable thrown = assertThrows(IllegalArgumentException.class, () -> {
                            throw new IllegalArgumentException("boom     !");
                        });
                        assertTrue(thrown.getMessage().matches("boom.*!"));
                    }
                }
                """
              )
            );
        }

        @Test void isMigratedToBodyWrappedInAssertThrows_forLiteralCustomExceptionThrown() {
            // language=java
            rewriteRun(
              java(
                """
                package com.abc;
                public class MyException extends Exception {
                    public MyException(String message) {
                        super(message);
                    }
                }
                """
              ),
              java(
                """
                import com.abc.MyException;
                import org.testng.annotations.Test;
                
                public class MyTest {
                
                    @Test(expectedExceptions = MyException.class)
                    public void test() {
                        throw new MyException("my exception");
                    }
                }
                """,
                """
                import com.abc.MyException;
                import org.junit.jupiter.api.Test;
                
                import static org.junit.jupiter.api.Assertions.assertThrows;
                
                public class MyTest {
                
                    @Test
                    public void test() {
                        assertThrows(MyException.class, () -> {
                            throw new MyException("my exception");
                        });
                    }
                }
                """
              )
            );
        }

        @SuppressWarnings("ConstantConditions")
        @Test void isMigratedToBodyWrappedInAssertThrows_forSingleStatement() {
            // language=java
            rewriteRun(
              java(
                """
                import org.testng.annotations.Test;
                
                public class MyTest {
                
                    @Test(expectedExceptions = IndexOutOfBoundsException.class)
                    public void test() {
                        int arr = new int[]{}[0];
                    }
                }
                """,
                """
                import org.junit.jupiter.api.Test;
                
                import static org.junit.jupiter.api.Assertions.assertThrows;
                
                public class MyTest {
                
                    @Test
                    public void test() {
                        assertThrows(IndexOutOfBoundsException.class, () -> {
                            int arr = new int[]{}[0];
                        });
                    }
                }
                """
              )
            );
        }

        @Test void isMigratedToBodyWrappedInAssertThrows() {
            // language=java
            rewriteRun(
              java(
                """
                import org.testng.annotations.Test;
                
                public class MyTest {
                
                    @Test(expectedExceptions = IllegalArgumentException.class)
                    public void test() {
                        String foo = "foo";
                        throw new IllegalArgumentException("boom");
                    }
                }
                """,
                """
                import org.junit.jupiter.api.Test;
                
                import static org.junit.jupiter.api.Assertions.assertThrows;
                
                public class MyTest {
                
                    @Test
                    public void test() {
                        assertThrows(IllegalArgumentException.class, () -> {
                            String foo = "foo";
                            throw new IllegalArgumentException("boom");
                        });
                    }
                }
                """
              )
            );
        }
    }

    @Nested class Attribute_timeOut {

        @Test void isMigratedToTimeoutAnnotation() {
            // language=java
            rewriteRun(
              java(
                """
                import org.testng.annotations.Test;
                
                public class MyTest {
                
                    @Test(timeOut = 500)
                    public void test() {
                    }
                }
                """,
                """
                import org.junit.jupiter.api.Test;
                import org.junit.jupiter.api.Timeout;
                
                import java.util.concurrent.TimeUnit;
                
                public class MyTest {
                
                    @Test
                    @Timeout(value = 500, unit = TimeUnit.MILLISECONDS)
                    public void test() {
                    }
                }
                """
              )
            );
        }

        /**
         * Unfortunately doesn't keep annotation on same line
         * TODO investigate how this could be achieved
         */
        @Test void isMigratedToTimeoutAnnotation_butNotPreservingSameLine() {
            // language=java
            rewriteRun(
              java(
                """
                import org.testng.annotations.Test;
                
                public class MyTest {
                
                    @Test(timeOut = 500) public void test() {
                    }
                }
                """,
                """
                import org.junit.jupiter.api.Test;
                import org.junit.jupiter.api.Timeout;
                
                import java.util.concurrent.TimeUnit;
                
                public class MyTest {
                
                    @Test
                    @Timeout(value = 500, unit = TimeUnit.MILLISECONDS)
                    public void test() {
                    }
                }
                """
              )
            );
        }
    }

    @Nested class MultipleAttributes {

        @Test void expectedExceptions_and_timeOut() {
            // language=java
            rewriteRun(
              java(
                """
                import org.testng.annotations.Test;
                
                public class MyTest {
                
                    @Test(expectedExceptions = IllegalArgumentException.class, timeOut = 500)
                    public void test() {
                        throw new IllegalArgumentException("boom");
                    }
                }
                """,
                """
                import org.junit.jupiter.api.Test;
                import org.junit.jupiter.api.Timeout;
                
                import java.util.concurrent.TimeUnit;
                
                import static org.junit.jupiter.api.Assertions.assertThrows;
                
                public class MyTest {
                
                    @Test
                    @Timeout(value = 500, unit = TimeUnit.MILLISECONDS)
                    public void test() {
                        assertThrows(IllegalArgumentException.class, () -> {
                            throw new IllegalArgumentException("boom");
                        });
                    }
                }
                """
              )
            );
        }
    }

    @Nested class Attributes_NotImplemented_willBeDeleted {

        // TODO
        @Disabled("TODO: needs to remove unknown annotation attributes")
        @Test void annotationAttributesWillBeDropped_WhenCannotBeHandled() {
            // language=java
            rewriteRun(
              java(
                """
                package de.foo.bar;
                
                import org.testng.annotations.Test;
                
                @Test(threadPoolSize = 8)
                class Baz {
                
                    @Test public void shouldDoStuff() {
                        //
                    }
                }
                """,
                """
                package de.foo.bar;
                
                import org.testng.annotations.Test;
                
                @Test
                class Baz {
                
                    @Test public void shouldDoStuff() {
                        //
                    }
                }
                """
              )
            );
        }
    }
}