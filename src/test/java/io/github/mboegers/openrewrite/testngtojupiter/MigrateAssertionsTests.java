/*
 * Copyright 2015-2024 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */
package io.github.mboegers.openrewrite.testngtojupiter;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.openrewrite.java.Assertions.java;

class MigrateAssertionsTests implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion()
            .logCompilationWarningsAndErrors(true)
            .classpath("junit-jupiter-api", "testng"))
          .recipe(new MigrateAssertionsRecipes());
    }

    static Supplier<Stream<Arguments>> assertEqualsWithMessageArgumentStream = () -> Stream.of(
        arguments("boolean", "boolean"),
        arguments("boolean", "java.lang.Boolean"),
        arguments("java.lang.Boolean", "boolean"),
        arguments("java.lang.Boolean", "java.lang.Boolean"),

        arguments("byte", "byte"),
        arguments("byte", "java.lang.Byte"),
        arguments("java.lang.Byte", "byte"),
        arguments("java.lang.Byte", "java.lang.Byte"),

        arguments("char", "char"),
        arguments("char", "java.lang.Character"),
        arguments("java.lang.Character", "char"),
        arguments("java.lang.Character", "java.lang.Character"),

        arguments("double", "double"),
        arguments("double", "java.lang.Double"),
        arguments("java.lang.Double", "double"),
        arguments("java.lang.Double", "java.lang.Double"),

        arguments("float", "float"),
        arguments("float", "java.lang.Float"),
        arguments("java.lang.Float", "float"),
        arguments("java.lang.Float", "java.lang.Float"),

        arguments("java.lang.Short", "java.lang.Short"),
        arguments("java.lang.Short", "short"),
        arguments("short", "java.lang.Short"),
        arguments("short", "short"),

        arguments("int", "int"),
        arguments("int", "java.lang.Integer"),
        arguments("java.lang.Integer", "int"),
        arguments("java.lang.Integer", "java.lang.Integer"),

        arguments("java.lang.Long", "java.lang.Long"),
        arguments("java.lang.Long", "long"),
        arguments("long", "long"),

        arguments("java.lang.String", "java.lang.String")
    );

    static Supplier<Stream<Arguments>> assertArrayArgumentStream = () -> Stream.of(
        arguments("boolean[]", "boolean[]"),
        arguments("byte[]", "byte[]"),
        arguments("char[]", "char[]"),
        arguments("double[]", "double[]"),
        arguments("float[]", "float[]"),
        arguments("short[]", "short[]"),
        arguments("int[]", "int[]"),
        arguments("long[]", "long[]"),
        arguments("Object[]", "Object[]")
    );

    static Stream<Arguments> assertEqualsArgumentsWithMessage() {
        return assertEqualsWithMessageArgumentStream.get();
    }

    static Stream<Arguments> assertEqualsArgumentsWithoutMessage() {
        return Stream.concat(
          assertEqualsWithMessageArgumentStream.get(),
          // ↓ there is no overload for this one with a message argument in TestNG
          Stream.of(arguments("long", "java.lang.Long"))
        );
    }

    static Stream<Arguments> toAssertArguments_shorterList() {
        return Stream.of(
          arguments("java.lang.Boolean", "java.lang.Boolean"),
          arguments("java.lang.Character", "java.lang.Character"),
          arguments("double", "double"),
          arguments("java.lang.Short", "short"),
          arguments("long", "java.lang.Long"),
          arguments("java.lang.String", "java.lang.String")
        );
    }

    static Stream<Arguments> toAssertArrayArguments() {
        return assertArrayArgumentStream.get();
    }

    @Nested class MigrateAssertEquals {

        @Nested class WithErrorMessage {

            @MethodSource("io.github.mboegers.openrewrite.testngtojupiter.MigrateAssertionsTests#assertEqualsArgumentsWithMessage")
            @ParameterizedTest void becomesAssertEquals_forPrimitiveAndBoxedArguments(String actual, String expected) {
                //language=java
                rewriteRun(java(
                  """
                  import org.testng.Assert;
                  
                  class MyTest {
                      void testMethod() {
                          %s actual;
                          %s expected;
                  
                          Assert.assertEquals(actual, expected, "Test failed badly");
                      }
                  }
                  """.formatted(actual, expected),
                  """
                  import org.junit.jupiter.api.Assertions;
                  
                  class MyTest {
                      void testMethod() {
                          %s actual;
                          %s expected;
                  
                          Assertions.assertEquals(expected, actual, "Test failed badly");
                      }
                  }
                  """.formatted(actual, expected)
                ));
            }

            @MethodSource("io.github.mboegers.openrewrite.testngtojupiter.MigrateAssertionsTests#toAssertArrayArguments")
            @ParameterizedTest void becomesAssertArrayEquals_forArrays(String actual, String expected) {
                //language=java
                rewriteRun(java(
                  """
                  import org.testng.Assert;
                  
                  class MyTest {
                      void testMethod() {
                          %s actual;
                          %s expected;
                  
                          Assert.assertEquals(actual, expected, "Test failed badly");
                      }
                  }
                  """.formatted(actual, expected),
                  """
                  import org.junit.jupiter.api.Assertions;
                  
                  class MyTest {
                      void testMethod() {
                          %s actual;
                          %s expected;
                  
                          Assertions.assertArrayEquals(expected, actual, "Test failed badly");
                      }
                  }
                  """.formatted(actual, expected)
                ));
            }
            
            @Test void becomesSpecialAssertArrayEquals_forIterators() {
                // language=java
                rewriteRun(java(
                    """
                    import java.util.Iterator;
                    import java.util.List;
                    import org.testng.Assert;
                    
                    class MyTest {
                        void testMethod() {
                            Iterator<String> actual = List.of("a", "b").iterator();
                            Iterator<String> expected = List.of("b", "a").iterator();
                    
                            Assert.assertEquals(actual, expected, "Kaboom.");
                        }
                    }
                    """,
                    """
                    import org.junit.jupiter.api.Assertions;
                    
                    import java.util.Iterator;
                    import java.util.List;
                    import java.util.Spliterators;
                    import java.util.stream.StreamSupport;
                    
                    class MyTest {
                        void testMethod() {
                            Iterator<String> actual = List.of("a", "b").iterator();
                            Iterator<String> expected = List.of("b", "a").iterator();
                    
                            Assertions.assertArrayEquals(StreamSupport.stream(Spliterators.spliteratorUnknownSize(expected, 0), false).toArray(), StreamSupport.stream(Spliterators.spliteratorUnknownSize(actual, 0), false).toArray(), "Kaboom.");
                        }
                    }
                    """
                ));
            }
        }

        @Nested class WithoutErrorMessage {

            @MethodSource("io.github.mboegers.openrewrite.testngtojupiter.MigrateAssertionsTests#assertEqualsArgumentsWithoutMessage")
            @ParameterizedTest void becomesAssertEquals_forPrimitiveAndBoxedArguments(String actual, String expected) {
                //language=java
                rewriteRun(java(
                  """
                  import org.testng.Assert;
                  
                  class MyTest {
                      void testMethod() {
                          %s actual;
                          %s expected;
                  
                          Assert.assertEquals(actual, expected);
                      }
                  }
                  """.formatted(actual, expected),
                  """
                  import org.junit.jupiter.api.Assertions;
                  
                  class MyTest {
                      void testMethod() {
                          %s actual;
                          %s expected;
                  
                          Assertions.assertEquals(expected, actual);
                      }
                  }
                  """.formatted(actual, expected)
                ));
            }

            @MethodSource("io.github.mboegers.openrewrite.testngtojupiter.MigrateAssertionsTests#toAssertArrayArguments")
            @ParameterizedTest void becomesAssertArrayEquals_forArrays(String actual, String expected) {
                //language=java
                rewriteRun(java(
                  """
                  import org.testng.Assert;
                  
                  class MyTest {
                      void testMethod() {
                          %s actual;
                          %s expected;
                  
                          Assert.assertEquals(actual, expected);
                      }
                  }
                  """.formatted(actual, expected),
                  """
                  import org.junit.jupiter.api.Assertions;
                  
                  class MyTest {
                      void testMethod() {
                          %s actual;
                          %s expected;
                  
                          Assertions.assertArrayEquals(expected, actual);
                      }
                  }
                  """.formatted(actual, expected)
                ));
            }

            @Test void becomesSpecialAssertArrayEquals_forIterators() {
                // language=java
                rewriteRun(java(
                  """
                  import java.util.Iterator;
                  import java.util.List;
                  import org.testng.Assert;
                  
                  class MyTest {
                      void testMethod() {
                          Iterator<String> actual = List.of("a", "b").iterator();
                          Iterator<String> expected = List.of("b", "a").iterator();
                  
                          Assert.assertEquals(actual, expected);
                      }
                  }
                  """,
                  """
                  import org.junit.jupiter.api.Assertions;
                  
                  import java.util.Iterator;
                  import java.util.List;
                  import java.util.Spliterators;
                  import java.util.stream.StreamSupport;
                  
                  class MyTest {
                      void testMethod() {
                          Iterator<String> actual = List.of("a", "b").iterator();
                          Iterator<String> expected = List.of("b", "a").iterator();
                  
                          Assertions.assertArrayEquals(StreamSupport.stream(Spliterators.spliteratorUnknownSize(expected, 0), false).toArray(), StreamSupport.stream(Spliterators.spliteratorUnknownSize(actual, 0), false).toArray());
                      }
                  }
                  """
                ));
            }
        }
    }

    @Nested class MigrateAssertNotEquals {

        @Nested class WithErrorMessage {

            @MethodSource("io.github.mboegers.openrewrite.testngtojupiter.MigrateAssertionsTests#toAssertArguments_shorterList")
            @ParameterizedTest void isMigratedToJupiterEquivalent(String actual, String expected) {
                //language=java
                rewriteRun(java(
                    """
                    import org.testng.Assert;
                    
                    class MyTest {
                        void testMethod() {
                            %s actual;
                            %s expected;
                    
                            Assert.assertNotEquals(actual, expected, "Test failed badly");
                        }
                    }
                    """.formatted(actual, expected), """
                    import org.junit.jupiter.api.Assertions;
                    
                    class MyTest {
                        void testMethod() {
                            %s actual;
                            %s expected;
                    
                            Assertions.assertNotEquals(expected, actual, "Test failed badly");
                        }
                    }
                    """.formatted(actual, expected)
                ));
            }

            @MethodSource("io.github.mboegers.openrewrite.testngtojupiter.MigrateAssertionsTests#toAssertArrayArguments")
            @ParameterizedTest void isMigratedToJupiterEquivalent_wrappedInArrayToString_forArrays(String actual, String expected) {
                //language=java
                rewriteRun(java(
                    """
                    import org.testng.Assert;
                    
                    class MyTest {
                        void testMethod() {
                            %s actual;
                            %s expected;
                    
                            Assert.assertNotEquals(actual, expected, "Test failed badly");
                        }
                    }
                    """.formatted(actual, expected),
                    """
                    import org.junit.jupiter.api.Assertions;
                    
                    import java.util.Arrays;
                    
                    class MyTest {
                        void testMethod() {
                            %s actual;
                            %s expected;
                    
                            Assertions.assertNotEquals(Arrays.toString(expected), Arrays.toString(actual), "Test failed badly");
                        }
                    }
                    """.formatted(actual, expected)
                ));
            }
        }

        @Nested class WithoutErrorMessage {

            @MethodSource("io.github.mboegers.openrewrite.testngtojupiter.MigrateAssertionsTests#toAssertArguments_shorterList")
            @ParameterizedTest void withoutErrorMessage(String actual, String expected) {
                //language=java
                rewriteRun(java(
                    """
                    import org.testng.Assert;
                    
                    class MyTest {
                        void testMethod() {
                            %s actual;
                            %s expected;
                    
                            Assert.assertNotEquals(actual, expected);
                        }
                    }
                    """.formatted(actual, expected),
                    """
                    import org.junit.jupiter.api.Assertions;
                    
                    class MyTest {
                        void testMethod() {
                            %s actual;
                            %s expected;
                    
                            Assertions.assertNotEquals(expected, actual);
                        }
                    }
                    """.formatted(actual, expected)
                ));
            }

            @MethodSource("io.github.mboegers.openrewrite.testngtojupiter.MigrateAssertionsTests#toAssertArrayArguments")
            @ParameterizedTest void isMigratedToJupiterEquivalent_wrappedInArrayToString_forArrays(String actual, String expected) {
                //language=java
                rewriteRun(java(
                    """
                    import org.testng.Assert;
                    
                    class MyTest {
                        void testMethod() {
                            %s actual;
                            %s expected;
                    
                            Assert.assertNotEquals(actual, expected);
                        }
                    }
                    """.formatted(actual, expected),
                    """
                    import org.junit.jupiter.api.Assertions;

                    import java.util.Arrays;
                    
                    class MyTest {
                        void testMethod() {
                            %s actual;
                            %s expected;
                    
                            Assertions.assertNotEquals(Arrays.toString(expected), Arrays.toString(actual));
                        }
                    }
                    """.formatted(actual, expected)
                ));
            }
        }
    }

    @Nested class SkipAssertEqualsDeep {

        @ValueSource(strings = {"java.util.Map<?,?>", "java.util.Set<?>"})
        @ParameterizedTest void withErrorMessage(String type) {
            //language=java
            rewriteRun(java(
              """
              import org.testng.Assert;
              
              class MyTest {
                  void testMethod() {
                      %s actual;
                      %s expected;
              
                      Assert.assertEqualsDeep(actual, expected, "Test failed badly");
                  }
              }
              """.formatted(type, type)
            ));
        }

        @Test
        void withoutErrorMessage() {
            //language=java
            rewriteRun(java(
                    """
              import org.testng.Assert;
              
              class MyTest {
                  void testMethod() {
                      java.util.Map<?,?> actual;
                      java.util.Map<?,?> expected;
              
                      Assert.assertEqualsDeep(actual, expected);
                  }
              }
              """
            ));
        }
    }

    @Nested class MigrateAssertFalse {

        @ValueSource(strings = {"boolean", "Boolean"})
        @ParameterizedTest void withErrorMessage(String type) {
            //language=java
            rewriteRun(java(
              """
              import org.testng.Assert;
              
              class MyTest {
                  void testMethod() {
                      %s expr;
              
                      Assert.assertFalse(expr, "Test failed badly");
                  }
              }
              """.formatted(type), """
              import org.junit.jupiter.api.Assertions;
              
              class MyTest {
                  void testMethod() {
                      %s expr;
              
                      Assertions.assertFalse(expr, "Test failed badly");
                  }
              }
              """.formatted(type)
            ));
        }

        @ValueSource(strings = {"boolean", "Boolean"})
        @ParameterizedTest void withoutErrorMessage(String type) {
            //language=java
            rewriteRun(java(
              """
              import org.testng.Assert;
              
              class MyTest {
                  void testMethod() {
                      %s expr;
              
                      Assert.assertFalse(expr);
                  }
              }
              """.formatted(type), """
              import org.junit.jupiter.api.Assertions;
              
              class MyTest {
                  void testMethod() {
                      %s expr;
              
                      Assertions.assertFalse(expr);
                  }
              }
              """.formatted(type)
            ));
        }
    }

    @Nested class MigrateAssertTrue {

        @ValueSource(strings = {"boolean", "Boolean"})
        @ParameterizedTest void withErrorMessage(String type) {
            //language=java
            rewriteRun(java(
              """
              import org.testng.Assert;
              
              class MyTest {
                  void testMethod() {
                      %s expr;
              
                      Assert.assertTrue(expr, "Test failed badly");
                  }
              }
              """.formatted(type), """
              import org.junit.jupiter.api.Assertions;
              
              class MyTest {
                  void testMethod() {
                      %s expr;
              
                      Assertions.assertTrue(expr, "Test failed badly");
                  }
              }
              """.formatted(type)
            ));
        }

        @ValueSource(strings = {"boolean", "Boolean"})
        @ParameterizedTest void withoutErrorMessage(String type) {
            //language=java
            rewriteRun(java(
              """
              import org.testng.Assert;
              
              class MyTest {
                  void testMethod() {
                      %s expr;
              
                      Assert.assertTrue(expr);
                  }
              }
              """.formatted(type), """
              import org.junit.jupiter.api.Assertions;
              
              class MyTest {
                  void testMethod() {
                      %s expr;
              
                      Assertions.assertTrue(expr);
                  }
              }
              """.formatted(type)
            ));
        }
    }
}
