package org.philzen.oss;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.NonNullApi;
import org.openrewrite.java.AnnotationMatcher;
import org.openrewrite.java.ChangeType;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.search.FindImports;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.TextComment;
import org.openrewrite.marker.Markers;
import org.philzen.oss.utils.Class;

import java.util.Collections;
import java.util.Objects;

@Value
@NonNullApi
@EqualsAndHashCode(callSuper = true)
public class ChangeTypeProblem extends Recipe {

    @Override
    public String getDisplayName() {
        return "Migrate TestNG `@Test` annotations to JUnit 5";
    }

    @Override
    public String getDescription() {
        return String.format(
                "Update usages of TestNG's `@%s` annotation to JUnit 5's `@%s` annotation.", TESTNG_TYPE, JUPITER_TYPE
        );
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(Preconditions.or(
                new UsesType<>(TESTNG_TYPE, false),
                new FindImports(TESTNG_TYPE, null).getVisitor()
        ), new UpdateTestAnnotationToJunit5Visitor());
    }

    public static final String TESTNG_TYPE = "org.testng.annotations.Test";
    public static final String JUPITER_API_NAMESPACE = "org.junit.jupiter.api";
    public static final String JUPITER_TYPE = JUPITER_API_NAMESPACE + ".Test";

    // inspired by https://github.com/openrewrite/rewrite-testing-frameworks/blob/4e8ba68b2a28a180f84de7bab9eb12b4643e342e/src/main/java/org/openrewrite/java/testing/junit5/UpdateTestAnnotation.java#
    private static class UpdateTestAnnotationToJunit5Visitor extends JavaIsoVisitor<ExecutionContext> {

        private static final AnnotationMatcher TESTNG_TEST = new AnnotationMatcher("@org.testng.annotations.Test");

        @Override
        public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
            cu = super.visitCompilationUnit(cu, ctx);
            if (cu.findType(TESTNG_TYPE).isEmpty()) {
                return cu;
            }

            maybeRemoveImport(TESTNG_TYPE);
            return (J.CompilationUnit) 
                new ChangeType(TESTNG_TYPE, JUPITER_TYPE, true).getVisitor().visitNonNull(cu, ctx);
        }

        final static String MISFIT_COMMENT = " ❗\uFE0F ❗\uFE0F ❗\uFE0F\n"
            + "   At least one `@Test`-attribute could not be migrated to JUnit 5. Kindly review the remainder below\n"
            + "   and manually apply any changes you may require to retain the existing test suite's behavior. Delete\n"
            + "↓  the annotation and this comment when satisfied, or use `git reset --hard` to roll back the migration.\n\n"
            + "   If you think this is a mistake or have an idea how this migration could be implemented instead, any\n"
            + "   feedback to https://github.com/Philzen/rewrite-TestNG-to-JUnit5/issues will be greatly appreciated.\n";

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, 
                                                        ExecutionContext executionContext) {

            final J.Annotation testngAnnotation = Objects.requireNonNull(Class.getAnnotation(classDecl, TESTNG_TEST));
            final J.Annotation commentedAndFullyQualified = testngAnnotation
                .withAnnotationType(((J.Identifier) testngAnnotation.getAnnotationType()).withSimpleName(TESTNG_TYPE))
                .withPrefix(Space.build("\n", Collections.emptyList()))
                .withComments(Collections.singletonList(
                    new TextComment(true, MISFIT_COMMENT, "\n", Markers.EMPTY))
                );
            
            classDecl = autoFormat(
                classDecl.withLeadingAnnotations(Collections.singletonList(commentedAndFullyQualified)), 
                executionContext
            );

            return super.visitClassDeclaration(classDecl, executionContext);
        }
    }
}
