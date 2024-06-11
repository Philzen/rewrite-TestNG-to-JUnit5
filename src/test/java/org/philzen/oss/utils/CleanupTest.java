package org.philzen.oss.utils;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.internal.lang.NonNullApi;
import org.openrewrite.java.AnnotationMatcher;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

@NonNullApi
class CleanupTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(RewriteTest.toRecipe(() -> new JavaIsoVisitor<>() {

            static final String TYPE_TO_REMOVE = "java.lang.Deprecated";
            private static final AnnotationMatcher MATCHER = new AnnotationMatcher("@" + TYPE_TO_REMOVE);

            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext executionContext) {
                if (classDecl.getLeadingAnnotations().stream().noneMatch(MATCHER::matches)) {
                    return classDecl;
                }

                for (final J.Annotation annotation : classDecl.getLeadingAnnotations().stream().filter(MATCHER::matches).toList()) {
                    maybeRemoveImport(TYPE_TO_REMOVE);
                    classDecl = Cleanup.removeAnnotation(classDecl, annotation);
                }

                return classDecl;
            }
        }));
    }

    @Nested class removeAnnotation {

        @Test void onClassWithModifier() {
            rewriteRun(
              // language=java
              java(
                """
                @Deprecated
                public class BazTest {
                
                }
                """,
                """
                public class BazTest {
                
                }
                """
              )
            );
        }

        @Test void onClassWithoutModifier() {
            rewriteRun(
              // language=java
              java(
                """
                @Deprecated
                class BazTest {
                
                }
                """,
                """
                class BazTest {
                
                }
                """
              )
            );
        }

        /**
         * This test is a bit convoluted as it will fail with "Recipe was expected to make a change but made no changes"
         * when not also including removal of an import (only happens on typed classes without modifiers)
         */
        @Test void onTypedClass() {
            rewriteRun(
              // language=java
              java(
                """
                import java.lang.Deprecated;
                
                @Deprecated
                class BazTest<String> {
                
                }
                """,
                """
                class BazTest<String> {
                
                }
                """
              )
            );
        }

        @Test void preservingExistingAnnotationsAfterIt() {
            rewriteRun(
              // language=java
              java(
                """
                import javax.annotation.concurrent.ThreadSafe;
                
                @Deprecated
                @ThreadSafe
                public class BazTest {
                
                }
                """,
                """
                import javax.annotation.concurrent.ThreadSafe;
                
                @ThreadSafe
                public class BazTest {
                
                }
                """
              )
            );
        }

        @Test void preservingExistingAnnotationsBeforeIt() {
            rewriteRun(
              // language=java
              java(
                """
                import javax.annotation.concurrent.ThreadSafe;
                
                @ThreadSafe @Deprecated
                public class BazTest {
                
                }
                """,
                """
                import javax.annotation.concurrent.ThreadSafe;
                
                @ThreadSafe
                public class BazTest {
                
                }
                """
              )
            );
        }
    }
}
