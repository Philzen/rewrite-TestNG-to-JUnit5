package org.philzen.oss.testng;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

@SuppressWarnings({"groupsTestNG", "NewClassNamingConvention"})
class UpdateTestAnnotationToJunit5Test implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UpdateTestAnnotationToJunit5());
    }

    @Nested class NoAttributes {

        @Nested class onClass {

            @Test void isMigratedToMethods() {
                // language=java
                rewriteRun(java(
                    """
                    import org.testng.annotations.Test;
                    
                    @Test
                    public class BazTest {
                    
                        public void shouldDoStuff() {
                            //
                        }
                    
                        public void shouldDoMoreStuff() {
                            //
                        }
                    }
                    """,
                    """
                    import org.junit.jupiter.api.Test;
                    
                    public class BazTest {
                    
                        @Test
                        public void shouldDoStuff() {
                            //
                        }
                    
                        @Test
                        public void shouldDoMoreStuff() {
                            //
                        }
                    }
                    """
                ));
            }

            @Test void isMigratedToMethods_preservingOtherAnnotations() {
                // language=java
                rewriteRun(java(
                  """
                    import org.testng.annotations.Test;
                    
                    @Test
                    @Deprecated
                    class BazTest {
                    
                        public void shouldDoStuff() {
                            //
                        }
                    }
                    """,
                    """
                    import org.junit.jupiter.api.Test;
                    
                    @Deprecated
                    class BazTest {
                    
                        @Test
                        public void shouldDoStuff() {
                            //
                        }
                    }
                    """
                ));
            }

            @Test void isMigratedToMethods_preservingOtherAnnotations_onSameLine() {
                // language=java
                rewriteRun(java(
                    """
                    import org.testng.annotations.Test;
                    
                    @Deprecated @Test
                    public class BazTest {
                    
                        public void shouldDoStuff() {
                            //
                        }
                    }
                    """,
                    """
                    import org.junit.jupiter.api.Test;
                    
                    @Deprecated
                    public class BazTest {
                    
                        @Test
                        public void shouldDoStuff() {
                            //
                        }
                    }
                    """
                ));
            }

            /**
             * Non-public method are executed only if they are explicitly annotated with `@Test`
             */
            @Test void isMigratedOnlyToPublicMethods() {
                // language=java
                rewriteRun(java(
                    """
                    import org.testng.annotations.Test;
                    
                    @Test
                    public class BazTest {
                    
                        public void shouldDoStuff() {
                            //
                        }
                    
                        void thisAintNoTest() {
                            //
                        }
                    }
                    """,
                    """
                    import org.junit.jupiter.api.Test;
                    
                    public class BazTest {
                    
                        @Test
                        public void shouldDoStuff() {
                            //
                        }
                    
                        void thisAintNoTest() {
                            //
                        }
                    }
                    """
                ));
            }

            @Test void isMigratedToMethods_whenClassIsTyped() {
                // language=java
                rewriteRun(java(
                    """
                    import org.testng.annotations.Test;
                    
                    @Test
                    class BazTest<String> {
                    
                        public void shouldDoStuff() {
                            //
                        }
                    
                        public void shouldDoMoreStuff() {
                            //
                        }
                    }
                    """,
                    """
                    import org.junit.jupiter.api.Test;
                    
                    class BazTest<String> {
                    
                        @Test
                        public void shouldDoStuff() {
                            //
                        }
                    
                        @Test
                        public void shouldDoMoreStuff() {
                            //
                        }
                    }
                    """
                ));
            }

            @Test void isMigratedToMethods_whenFullyQualified() {
                // language=java
                rewriteRun(java(
                    """
                    package de.foo.bar;
                    
                    @org.testng.annotations.Test
                    public class BazTest {
                    
                        public void shouldDoStuff() {
                            //
                        }
                    
                        public void shouldDoMoreStuff() {
                            //
                        }
                    }
                    """,
                    """
                    package de.foo.bar;
                    
                    public class BazTest {
                    
                        @org.junit.jupiter.api.Test
                        public void shouldDoStuff() {
                            //
                        }
                    
                        @org.junit.jupiter.api.Test
                        public void shouldDoMoreStuff() {
                            //
                        }
                    }
                    """
                ));
            }

            @Test void doesNotOverwriteMethodLevelTestAnnotations() {
                // language=java
                rewriteRun(java(
                    """
                    import org.testng.annotations.Test;
                    
                    @Test
                    class BazTest {
                    
                        public void shouldDoStuff() { }
                    
                        @Test(enabled = false)
                        public void shouldDoMoreStuff() { }
                    }
                    """,
                    """
                    import org.junit.jupiter.api.Disabled;
                    import org.junit.jupiter.api.Test;
                    
                    class BazTest {
                    
                        @Test
                        public void shouldDoStuff() { }
                    
                        @Test
                        @Disabled
                        public void shouldDoMoreStuff() { }
                    }
                    """
                ));
            }

            /**
             * Inner class methods are executed by the TestNG runner only when they are explicitly annotated with @Test, 
             * class-level annotation will not make them execute.
             */
            @Test void doesNotAnnotateInnerClassMethods() {
                // language=java
                rewriteRun(java(
                    """
                    import org.testng.annotations.Test;
                    
                    @Test
                    public class BazTest {
                        public void test() { }
                    
                        public static class Inner {
                            public void noTest() { }
                        }
                    }
                    """,
                    """
                    import org.junit.jupiter.api.Test;
                    
                    public class BazTest {
                        @Test
                        public void test() { }
                    
                        public static class Inner {
                            public void noTest() { }
                        }
                    }
                    """
                ));
            }
            
            @Test void isRemoved_WhenOnlyInnerClassMethods() {
                // language=java
                rewriteRun(java(
                    """
                    import org.testng.annotations.Test;
                    
                    @Test
                    public class BazTest {
                        public static class Inner {
                            public void noTest() { }
                        }
                    }
                    """,
                    """
                    public class BazTest {
                        public static class Inner {
                            public void noTest() { }
                        }
                    }
                    """
                ));
            }

            @Test void noChangeOnOtherAnnotations() {
                // language=java
                rewriteRun(java(
                    """
                    @Deprecated
                    public class BazTest {
                    }
                    """
                ));
            }
        }

        @Test void isMigratedToJunitTestAnnotationWithoutParameters() {
            // language=java
            rewriteRun(java(
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
            ));
        }

        @Test void isMigratedPreservingOtherAnnotationsAndComments() {
            // language=java
            rewriteRun(java(
                """
                package org.openrewrite;
                public @interface Issue {
                    String value();
                }
                """
            ), java(
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
            ));
        }

        @Test void isMigratedWhenReferencedAsVariable() {
            // language=java
            rewriteRun(java(
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
            ));
        }

        @Test void isMigrated_whenUsedInJavadoc() {
            // language=java
            rewriteRun(java(
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
            ));
        }

        @Test void noTestAnnotationValues_sameLine_multipleImports() {
            // language=java
            rewriteRun(java(
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
            ));
        }

        @Test void fullyQualified() {
            // language=java
            rewriteRun(java(
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
            ));
        }

        @Test void fullyQualified_sameLineAsMethodDeclaration() {
            // language=java
            rewriteRun(java(
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
            ));
        }

        @Test void mixedFullyQualifiedAndNot() {
            // language=java
            rewriteRun(java(
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
            ));
        }

        @SuppressWarnings("JUnitMalformedDeclaration")
        @Test void nestedClass() {
            // language=java
            rewriteRun(java(
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
            ));
        }

        @Test void noChangeNecessary() {
            // language=java
            rewriteRun(java(
                """
                package de.foo.bar;
                
                import org.junit.jupiter.api.Test;
                
                class Baz {
                
                    @Test public void shouldDoStuff() {
                        //
                    }
                }
                """
            ));
        }

        @Test void noChangeNecessary_fullyQualified() {
            // language=java
            rewriteRun(java(
                """
                package de.foo.bar;
                
                class Baz {
                
                    @org.junit.jupiter.api.Test public void shouldDoStuff() {
                        //
                    }
                }
                """
            ));
        }

        @Test void noChangeNecessary_nestedClass() {
            // language=java
            rewriteRun(java(
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
            ));
        }
    }

    @Nested class Attribute_description {

        @Test void isMigratedToDisplayNameAnnotation_whenNotEmpty() {
            // language=java
            rewriteRun(java(
                """
                import org.testng.annotations.Test;
                
                public class MyTest {
                
                    @Test(description = "A test that tests something")
                    public void test() {
                        // some content
                    }
                }
                """,
                """
                import org.junit.jupiter.api.DisplayName;
                import org.junit.jupiter.api.Test;
                
                public class MyTest {
                
                    @Test
                    @DisplayName("A test that tests something")
                    public void test() {
                        // some content
                    }
                }
                """
            ));
        }

        @SuppressWarnings("DefaultAnnotationParam")
        @Test void isIgnored_whenEmpty() {
            // language=java
            rewriteRun(java(
                """
                import org.testng.annotations.Test;
                
                public class MyTest {
                
                    @Test(description = "")
                    public void test() {
                        // some content
                    }
                }
                """,
                """
                import org.junit.jupiter.api.Test;
                
                public class MyTest {
                
                    @Test
                    public void test() {
                        // some content
                    }
                }
                """
            ));
        }
    }

    @Nested class Attribute_enabled {

        @Test void isMigratedToDisabledAnnotation_whenFalse() {
            // language=java
            rewriteRun(java(
                """
                import org.testng.annotations.Test;
                
                public class MyTest {
                
                    @Test(enabled = false)
                    public void test() {
                        // some content
                    }
                }
                """,
                """
                import org.junit.jupiter.api.Disabled;
                import org.junit.jupiter.api.Test;
                
                public class MyTest {
                
                    @Test
                    @Disabled
                    public void test() {
                        // some content
                    }
                }
                """
            ));
        }
    }

    @Nested class Attribute_expectedExceptions {

        @Test void isMigratedToBodyWrappedInAssertThrows_forLiteralJavaExceptionThrown() {
            // language=java
            rewriteRun(java(
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
                import org.junit.jupiter.api.Assertions;
                import org.junit.jupiter.api.Test;
                
                public class MyTest {
                
                    @Test
                    public void test() {
                        Assertions.assertThrows(IllegalArgumentException.class, () -> {
                            throw new IllegalArgumentException("boom");
                        });
                    }
                }
                """
            ));
        }

        @Test void Attribute_expectedExceptionsMessageRegExp() {
            // language=java
            rewriteRun(java(
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
                import org.junit.jupiter.api.Assertions;
                import org.junit.jupiter.api.Test;
                
                public class MyTest {
                
                    @Test
                    public void test() {
                        final Throwable thrown = Assertions.assertThrows(IllegalArgumentException.class, () -> {
                            throw new IllegalArgumentException("boom     !");
                        });
                        Assertions.assertTrue(thrown.getMessage().matches("boom.*!"));
                    }
                }
                """
            ));
        }

        @Test void isMigratedToBodyWrappedInAssertThrows_forLiteralCustomExceptionThrown() {
            // language=java
            rewriteRun(java(
                """
                package com.abc;
                public class MyException extends Exception {
                    public MyException(String message) {
                        super(message);
                    }
                }
                """
            ), java(
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
                import org.junit.jupiter.api.Assertions;
                import org.junit.jupiter.api.Test;
                
                public class MyTest {
                
                    @Test
                    public void test() {
                        Assertions.assertThrows(MyException.class, () -> {
                            throw new MyException("my exception");
                        });
                    }
                }
                """
            ));
        }

        @SuppressWarnings("ConstantConditions")
        @Test void isMigratedToBodyWrappedInAssertThrows_forSingleStatement() {
            // language=java
            rewriteRun(java(
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
                import org.junit.jupiter.api.Assertions;
                import org.junit.jupiter.api.Test;
                
                public class MyTest {
                
                    @Test
                    public void test() {
                        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> {
                            int arr = new int[]{}[0];
                        });
                    }
                }
                """
            ));
        }

        @Test void isMigratedToBodyWrappedInAssertThrows() {
            // language=java
            rewriteRun(java(
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
                import org.junit.jupiter.api.Assertions;
                import org.junit.jupiter.api.Test;
                
                public class MyTest {
                
                    @Test
                    public void test() {
                        Assertions.assertThrows(IllegalArgumentException.class, () -> {
                            String foo = "foo";
                            throw new IllegalArgumentException("boom");
                        });
                    }
                }
                """
            ));
        }

        @Nested class Array {

            @Test void extractsFirstElementOfMultiple() {
                // language=java
                rewriteRun(java(
                    """
                    import org.testng.annotations.Test;
                    
                    public class MyTest {
                    
                        @Test(expectedExceptions = { RuntimeException.class, IllegalAccessError.class, UnknownError.class } )
                        public void test() {
                            throw new RuntimeException("Whooopsie!");
                        }
                    }
                    """,
                    """
                    import org.junit.jupiter.api.Assertions;
                    import org.junit.jupiter.api.Test;
                    
                    public class MyTest {
                    
                        @Test
                        public void test() {
                            Assertions.assertThrows(RuntimeException.class, () -> {
                                throw new RuntimeException("Whooopsie!");
                            });
                        }
                    }
                    """
                  )
                );
            }
            @SuppressWarnings("DefaultAnnotationParam")
            @Test void doesNotAddAssert_ifEmpty() {
                // language=java
                rewriteRun(java(
                    """
                    import org.testng.annotations.Test;
                    
                    public class MyTest {
                    
                        @Test(expectedExceptions = { } )
                        public void test() {
                            throw new RuntimeException("Not really caught nor tested");
                        }
                    }
                    """,
                    """
                    import org.junit.jupiter.api.Test;
                    
                    public class MyTest {
                    
                        @Test
                        public void test() {
                            throw new RuntimeException("Not really caught nor tested");
                        }
                    }
                    """
                ));
            }
        }
    }

    @Nested class Attribute_groups {

        @Test void isMigratedToSingleTagAnnotation_whenLiteralValue() {
            // language=java
            rewriteRun(java(
                """
                import org.testng.annotations.Test;
                
                public class MyTest {
                
                    @Test(groups = "Fast")
                    public void test() {
                    }
                }
                """,
                """
                import org.junit.jupiter.api.Tag;
                import org.junit.jupiter.api.Test;
                
                public class MyTest {
                
                    @Test
                    @Tag("Fast")
                    public void test() {
                    }
                }
                """
            ));
        }

        @Test void isMigratedToSingleTagAnnotation_whenSingleArrayValue() {
            // language=java
            rewriteRun(java(
                """
                import org.testng.annotations.Test;
                
                public class MyTest {
                
                    @Test(groups = { "Fast" })
                    public void test() {
                    }
                }
                """,
                """
                import org.junit.jupiter.api.Tag;
                import org.junit.jupiter.api.Test;
                
                public class MyTest {
                
                    @Test
                    @Tag("Fast")
                    public void test() {
                    }
                }
                """
            ));
        }

        @Test void isMigratedToMultipleTagAnnotations_forEveryArrayValue() {
            // language=java
            rewriteRun(java(
                """
                import org.testng.annotations.Test;
                
                public class MyTest {
                
                    @Test(groups = { "Fast", "Integration", "Regression-1312" })
                    public void test() {
                    }
                }
                """,
                """
                import org.junit.jupiter.api.Tag;
                import org.junit.jupiter.api.Test;
                
                public class MyTest {
                
                    @Test
                    @Tag("Fast")
                    @Tag("Integration")
                    @Tag("Regression-1312")
                    public void test() {
                    }
                }
                """
            ));
        }

        @SuppressWarnings("DefaultAnnotationParam")
        @Test void isIgnored_forEmptyArray() {
            // language=java
            rewriteRun(java(
                """
                import org.testng.annotations.Test;
                
                public class MyTest {
                
                    @Test(groups = { })
                    public void test() {
                    }
                }
                """,
                """
                import org.junit.jupiter.api.Test;
                
                public class MyTest {
                
                    @Test
                    public void test() {
                    }
                }
                """
            ));
        }

        @Test void isIgnored_forEmptyString() {
            // language=java
            rewriteRun(java(
                """
                import org.testng.annotations.Test;
                
                public class MyTest {
                
                    @Test(groups = "")
                    public void test() {
                    }
                }
                """,
                """
                import org.junit.jupiter.api.Test;
                
                public class MyTest {
                
                    @Test
                    public void test() {
                    }
                }
                """
            ));
        }
    }

    @Nested class Attribute_timeOut {

        @Test void isMigratedToTimeoutAnnotation() {
            // language=java
            rewriteRun(java(
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
            ));
        }

        /**
         * Unfortunately doesn't keep annotation on same line
         * TODO investigate how this could be achieved
         */
        @Test void isMigratedToTimeoutAnnotation_butNotPreservingSameLine() {
            // language=java
            rewriteRun(java(
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
            ));
        }
    }

    @Nested class MultipleAttributes {

        @Test void expectedExceptions_and_timeOut() {
            // language=java
            rewriteRun(java(
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
                import org.junit.jupiter.api.Assertions;
                import org.junit.jupiter.api.Test;
                import org.junit.jupiter.api.Timeout;
                
                import java.util.concurrent.TimeUnit;
                
                public class MyTest {
                
                    @Test
                    @Timeout(value = 500, unit = TimeUnit.MILLISECONDS)
                    public void test() {
                        Assertions.assertThrows(IllegalArgumentException.class, () -> {
                            throw new IllegalArgumentException("boom");
                        });
                    }
                }
                """
            ));
        }
    }

    /**
     * Covering issue #4
     */
    @Nested class Attributes_NotImplemented_willBeRetained {
        
        @Test void onClassLevelAnnotations() {
            // language=java
            rewriteRun(java(
              """
                package foo.bar;
                
                import org.testng.annotations.Test;
                
                @Test(threadPoolSize = 8)
                class Baz {
                    public void shouldDoStuff() {
                        //
                    }
                }
                """,
              """
                package foo.bar;
                
                import org.junit.jupiter.api.Test;
                
                /* ❗️ ❗️ ❗️
                   At least one `@Test`-attribute could not be migrated to JUnit 5. Kindly review the remainder below
                   and manually apply any changes you may require to retain the existing test suite's behavior. Delete
                ↓  the annotation and this comment when satisfied, or use `git reset --hard` to roll back the migration.
                
                   If you think this is a mistake or have an idea how this migration could be implemented instead, any
                   feedback to https://github.com/Philzen/rewrite-TestNG-to-JUnit5/issues will be greatly appreciated.
                */
                @org.testng.annotations.Test(threadPoolSize = 8)
                class Baz {
                    @Test
                    public void shouldDoStuff() {
                        //
                    }
                }
                """
            ));
        }
        
        @Test void onClassAndMethodLevelAnnotations() {
            // language=java
            rewriteRun(java(
              """
                package foo.bar;
                
                import org.testng.annotations.Test;
                
                @Test(threadPoolSize = 8)
                class Baz {
                
                    public void shouldDoStuff() {
                    }
                
                    @Test(successPercentage = 40)
                    public void shouldDoMoreStuff() {
                    }
                }
                """,
              """
                package foo.bar;
                
                import org.junit.jupiter.api.Test;
                
                /* ❗️ ❗️ ❗️
                   At least one `@Test`-attribute could not be migrated to JUnit 5. Kindly review the remainder below
                   and manually apply any changes you may require to retain the existing test suite's behavior. Delete
                ↓  the annotation and this comment when satisfied, or use `git reset --hard` to roll back the migration.
                
                   If you think this is a mistake or have an idea how this migration could be implemented instead, any
                   feedback to https://github.com/Philzen/rewrite-TestNG-to-JUnit5/issues will be greatly appreciated.
                */
                @org.testng.annotations.Test(threadPoolSize = 8)
                class Baz {
                
                    @Test
                    public void shouldDoStuff() {
                    }
                
                    @Test
                    /* ❗️ ❗️ ❗️
                       At least one `@Test`-attribute could not be migrated to JUnit 5. Kindly review the remainder below
                       and manually apply any changes you may require to retain the existing test suite's behavior. Delete
                    ↓  the annotation and this comment when satisfied, or use `git reset --hard` to roll back the migration.
                   \s
                       If you think this is a mistake or have an idea how this migration could be implemented instead, any
                       feedback to https://github.com/Philzen/rewrite-TestNG-to-JUnit5/issues will be greatly appreciated.
                    */
                    @org.testng.annotations.Test(successPercentage = 40)
                    public void shouldDoMoreStuff() {
                    }
                }
                """
            ));
        }

        @Test void onMethodLevelAnnotation() {
            // language=java
            rewriteRun(java(
                """
                package de.foo.bar;
                
                import org.testng.annotations.Test;
                
                class Baz {
                    @Test(threadPoolSize = 8) public void shouldDoStuff() {
                        //
                    }
                }
                """,
                """
                package de.foo.bar;
                
                import org.junit.jupiter.api.Test;
                
                class Baz {
                    @Test
                    /* ❗️ ❗️ ❗️
                       At least one `@Test`-attribute could not be migrated to JUnit 5. Kindly review the remainder below
                       and manually apply any changes you may require to retain the existing test suite's behavior. Delete
                    ↓  the annotation and this comment when satisfied, or use `git reset --hard` to roll back the migration.
                   \s
                       If you think this is a mistake or have an idea how this migration could be implemented instead, any
                       feedback to https://github.com/Philzen/rewrite-TestNG-to-JUnit5/issues will be greatly appreciated.
                    */
                    @org.testng.annotations.Test(threadPoolSize = 8)
                    public void shouldDoStuff() {
                        //
                    }
                }
                """
            ));
        }
        
        @Test void onMethodLevelAnnotation_whileOthersAreMigrated() {
            // language=java
            rewriteRun(java(
              """
                package de.foo.bar;
                
                import org.testng.annotations.Test;
                
                class Baz {
                
                    @Test(description = "Yeah!", threadPoolSize = 8) public void shouldDoStuff() {
                        //
                    }
                }
                """,
                """
                package de.foo.bar;
                
                import org.junit.jupiter.api.DisplayName;
                import org.junit.jupiter.api.Test;
                
                class Baz {
                
                    @Test
                    @DisplayName("Yeah!")
                    /* ❗️ ❗️ ❗️
                       At least one `@Test`-attribute could not be migrated to JUnit 5. Kindly review the remainder below
                       and manually apply any changes you may require to retain the existing test suite's behavior. Delete
                    ↓  the annotation and this comment when satisfied, or use `git reset --hard` to roll back the migration.
                   \s
                       If you think this is a mistake or have an idea how this migration could be implemented instead, any
                       feedback to https://github.com/Philzen/rewrite-TestNG-to-JUnit5/issues will be greatly appreciated.
                    */
                    @org.testng.annotations.Test(threadPoolSize = 8)
                    public void shouldDoStuff() {
                        //
                    }
                }
                """
            ));
        }
    }
}
