package org.philzen.oss.testng;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class MigrateMismatchedAssertionsTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new MigrateMismatchedAssertions());
    }

    @ValueSource(strings = {"float[]", "double[]"})
    @ParameterizedTest
    void deltaFunctionForArraysIsMigrated(String type) {
        //language=java
        rewriteRun(java(
          """
          import org.testng.Assert;
          
          class MyTest {
              void testMethod() {
                  %s actual;
                  %s expected;
          
                  Assert.assertEquals(actual, expected, %s);
              }
          }
          """.formatted(type, type, type.equals("float[]") ? "0.1f" : "0.2d"),
          """
          import org.junit.jupiter.api.Assertions;
          
          class MyTest {
              void testMethod() {
                  %s actual;
                  %s expected;
          
                  Assertions.assertAll(() -> {
                      Assertions.assertEquals(expected.length, actual.length, "Arrays don't have the same size.");
                      for (int i = 0; i < actual.length; i++) {
                          Assertions.assertEquals(expected[i], actual[i], %s);
                      }
                  });
              }
          }
          """.formatted(type, type, type.equals("float[]") ? "0.1f" : "0.2d")
        ));
    }

    @ValueSource(strings = {"float[]", "double[]"})
    @ParameterizedTest
    void deltaFunctionForArraysIsMigratedWithMessage(String type) {
        //language=java
        rewriteRun(java(
          """
          import org.testng.Assert;
          
          class MyTest {
              void testMethod() {
                  %s actual;
                  %s expected;
          
                  Assert.assertEquals(actual, expected, %s, "Those values are way off.");
              }
          }
          """.formatted(type, type, type.equals("float[]") ? "0.1f" : "0.2d"),
          """
          import org.junit.jupiter.api.Assertions;
          
          class MyTest {
              void testMethod() {
                  %s actual;
                  %s expected;
          
                  Assertions.assertAll(() -> {
                      Assertions.assertEquals(expected.length, actual.length, "Arrays don't have the same size.");
                      for (int i = 0; i < actual.length; i++) {
                          Assertions.assertEquals(expected[i], actual[i], %s, "Those values are way off.");
                      }
                  });
              }
          }
          """.formatted(type, type, type.equals("float[]") ? "0.1f" : "0.2d")
        ));
    }
}
