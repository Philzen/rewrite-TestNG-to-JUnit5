package org.philzen.oss.testng;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.NonNullApi;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.*;
import org.openrewrite.java.search.FindImports;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.Markup;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Value
@NonNullApi
@EqualsAndHashCode(callSuper = true)
public class UpdateTestAnnotationToJunit5 extends Recipe {

    @Override
    public String getDisplayName() {
        return "Migrate TestNG `@Test` annotations to JUnit 5";
    }

    @Override
    public String getDescription() {
        return "Update usages of TestNG's `@org.testng.annotations.Test` annotation to JUnit 5's `@org.junit.jupiter.api.Test` annotation.";
    }

    @Nullable
    static private JavaParser.Builder<?, ?> javaParser;
    static private JavaParser.Builder<?, ?> javaParser() {
        if (javaParser == null) {
            javaParser = JavaParser.fromJavaVersion().classpath("junit-jupiter-api");
        }
        return javaParser;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(Preconditions.or(
                new UsesType<>("org.testng.annotations.Test", false),
                new FindImports("org.testng.annotations.Test", null).getVisitor()
        ), new UpdateTestAnnotationToJunit5Visitor());
    }

    // inspired by https://github.com/openrewrite/rewrite-testing-frameworks/blob/4e8ba68b2a28a180f84de7bab9eb12b4643e342e/src/main/java/org/openrewrite/java/testing/junit5/UpdateTestAnnotation.java#
    private static class UpdateTestAnnotationToJunit5Visitor extends JavaIsoVisitor<ExecutionContext> {

        private static final AnnotationMatcher TESTNG_TEST = new AnnotationMatcher("@org.testng.annotations.Test");

        private final JavaTemplate displayNameAnnotation = JavaTemplate
                .builder("@DisplayName(#{any(java.lang.String)})")
                .imports("org.junit.jupiter.api.DisplayName")
                .javaParser(javaParser()).build();

        private final JavaTemplate disabledAnnotation = JavaTemplate
                .builder("@Disabled")
                .imports("org.junit.jupiter.api.Disabled")
                .javaParser(javaParser()).build();

        private final JavaTemplate junitExecutable = JavaTemplate
                .builder("org.junit.jupiter.api.function.Executable o = () -> #{};")
                .javaParser(javaParser()).build();

        private final JavaTemplate tagAnnotation = JavaTemplate
                .builder("@Tag(#{any(java.lang.String)})")
                .imports("org.junit.jupiter.api.Tag")
                .javaParser(javaParser()).build();

        private final JavaTemplate timeoutAnnotation = JavaTemplate
                .builder("@Timeout(value = #{any(long)}, unit = TimeUnit.MILLISECONDS)")
                .imports("org.junit.jupiter.api.Timeout", "java.util.concurrent.TimeUnit")
                .javaParser(javaParser()).build();

        @Override
        public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
            J.CompilationUnit c = super.visitCompilationUnit(cu, ctx);
            if (!c.findType("org.testng.annotations.Test").isEmpty()) {
                // Update other references like `Test.class`.
                c = (J.CompilationUnit) new ChangeType("org.testng.annotations.Test", "org.junit.jupiter.api.Test", true)
                        .getVisitor().visitNonNull(c, ctx);
            }

            maybeRemoveImport("org.testng.annotations.Test");
            doAfterVisit(new JavaIsoVisitor<ExecutionContext>() {
                @Override
                public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
                    return cu.withClasses(ListUtils.map(cu.getClasses(), clazz -> (J.ClassDeclaration) visit(clazz, ctx)))
                        // take one more pass over the imports now that we've had a chance to add warnings to all
                        // uses of @Test through the rest of the source file
                        .withImports(ListUtils.map(cu.getImports(), anImport -> (J.Import) visit(anImport, ctx)));
                }

                @Override
                public J.Import visitImport(J.Import anImport, ExecutionContext ctx) {
                    if ("org.testng.annotations.Test".equals(anImport.getTypeName())) {
                        return Markup.error(anImport, new IllegalStateException("This import should have been removed by this recipe."));
                    }
                    return anImport;
                }

                @Override
                public JavaType visitType(@Nullable JavaType javaType, ExecutionContext ctx) {
                    if (TypeUtils.isOfClassType(javaType, "org.testng.annotations.Test")) {
                        getCursor().putMessageOnFirstEnclosing(J.class, "danglingTestRef", true);
                    }
                    return javaType;
                }

                @Override
                public J postVisit(J tree, ExecutionContext ctx) {
                    if (getCursor().getMessage("danglingTestRef", false)) {
                        return Markup.warn(tree, new IllegalStateException("This still has a type of `org.testng.annotations.Test`"));
                    }
                    return tree;
                }
            });

            return c;
        }

        @Override
        public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
            final ChangeTestAnnotation cta = new ChangeTestAnnotation();
            J.MethodDeclaration m = (J.MethodDeclaration) cta.visitNonNull(method, ctx, getCursor().getParentOrThrow());
            if (m == method) {
                return super.visitMethodDeclaration(method, ctx);
            }

            if (cta.description != null && !J.Literal.isLiteralValue(cta.description, "")) {
                maybeAddImport("org.junit.jupiter.api.DisplayName");
                m = displayNameAnnotation.apply(
                    updateCursor(m),
                    m.getCoordinates().addAnnotation(Comparator.comparing(J.Annotation::getSimpleName).reversed()),
                    cta.description
                );
            }

            if (J.Literal.isLiteralValue(cta.enabled, Boolean.FALSE)) {
                maybeAddImport("org.junit.jupiter.api.Disabled");
                m = disabledAnnotation.apply(
                        updateCursor(m),
                        m.getCoordinates().addAnnotation(Comparator.comparing(J.Annotation::getSimpleName).reversed())
                );
            }

            if (cta.expectedException instanceof J.FieldAccess
                // TestNG actually allows any type of Class here, however anything but a Throwable doesn't make sense 
                && TypeUtils.isAssignableTo("java.lang.Throwable", ((J.FieldAccess) cta.expectedException).getTarget().getType()))
            {
                m = junitExecutable.apply(updateCursor(m), m.getCoordinates().replaceBody(), m.getBody());
                final J.Block body = m.getBody();
                assert body != null;
                final J.Lambda lambda = (J.Lambda)
                        ((J.VariableDeclarations) body.getStatements().get(0))
                        .getVariables().get(0).getInitializer();

                maybeAddImport("org.junit.jupiter.api.Assertions");
                final List<Object> parameters = Arrays.asList(cta.expectedException, lambda);
                final String code = "Assertions.assertThrows(#{any(java.lang.Class)}, #{any(org.junit.jupiter.api.function.Executable)});";
                if (!(cta.expectedExceptionMessageRegExp instanceof J.Literal)) {
                    m = JavaTemplate.builder(code).javaParser(javaParser())
                        .imports("org.junit.jupiter.api.Assertions").build()
                        .apply(updateCursor(m), m.getCoordinates().replaceBody(), parameters.toArray());
                } else {
                    m = JavaTemplate.builder(
                            "final Throwable thrown = " + code + System.lineSeparator()
                                + "Assertions.assertTrue(thrown.getMessage().matches(#{any(java.lang.String)}));"
                        ).javaParser(javaParser()).imports("org.junit.jupiter.api.Assertions").build()
                        .apply(
                            updateCursor(m), 
                            m.getCoordinates().replaceBody(), 
                            ListUtils.concat(parameters, cta.expectedExceptionMessageRegExp).toArray()
                        );
                }
            }

            if (cta.groups != null) {
                maybeAddImport("org.junit.jupiter.api.Tag");
                if (cta.groups instanceof J.Literal && !J.Literal.isLiteralValue(cta.groups, "")) {
                    m = tagAnnotation.apply(
                            updateCursor(m),
                            m.getCoordinates().addAnnotation(Comparator.comparing(J.Annotation::getSimpleName).reversed()),
                            cta.groups
                    );
                } else if (cta.groups instanceof J.NewArray && ((J.NewArray) cta.groups).getInitializer() != null) {
                    final List<Expression> groups = ((J.NewArray) cta.groups).getInitializer();
                    for (Expression group : groups) {
                        if (group instanceof J.Empty) continue;
                        m = tagAnnotation.apply(
                                updateCursor(m),
                                m.getCoordinates().addAnnotation(Comparator.comparing(J.Annotation::getSimpleName).reversed()),
                                group
                        );
                    }
                }
            }

            if (cta.timeout != null) {
                maybeAddImport("java.util.concurrent.TimeUnit");
                maybeAddImport("org.junit.jupiter.api.Timeout");
                m = timeoutAnnotation.apply(
                        updateCursor(m),
                        m.getCoordinates().addAnnotation(Comparator.comparing(J.Annotation::getSimpleName)),
                        cta.timeout
                );
            }

            return super.visitMethodDeclaration(m, ctx);
        }

        private static class ChangeTestAnnotation extends JavaIsoVisitor<ExecutionContext> {

            private boolean found;

            @Nullable
            Expression description, enabled, expectedException, expectedExceptionMessageRegExp, groups, timeout;

            @Override
            public J.Annotation visitAnnotation(J.Annotation a, ExecutionContext ctx) {
                if (found || !TESTNG_TEST.matches(a)) {
                    return a;
                }

                // While unlikely, it's possible that a method has an inner class/lambda/etc. with methods that have test annotations
                // Avoid considering any but the first test annotation found
                found = true;
                if (a.getArguments() != null) {
                    for (Expression arg : a.getArguments()) {
                        if (!(arg instanceof J.Assignment)) {
                            continue;
                        }
                        final J.Assignment assign = (J.Assignment) arg;
                        final String assignParamName = ((J.Identifier) assign.getVariable()).getSimpleName();
                        final Expression e = assign.getAssignment();
                        if ("description".equals(assignParamName)) {
                            description = e;
                        } else if ("enabled".equals(assignParamName)) {
                            enabled = e;
                        } else if ("expectedExceptions".equals(assignParamName)) {
                            // if attribute was given in { array form }, pick the first element (null is not allowed)
                            expectedException = !(e instanceof J.NewArray)
                                ? e : Objects.requireNonNull(((J.NewArray) e).getInitializer()).get(0);
                        } else if ("expectedExceptionsMessageRegExp".equals(assignParamName)) {
                            expectedExceptionMessageRegExp = e;
                        } else if ("groups".equals(assignParamName)) {
                            groups = e;
                        } else if ("timeOut".equals(assignParamName)) {
                            timeout = e;
                        }
                    }
                }

                if (a.getAnnotationType() instanceof J.FieldAccess) {
                    return JavaTemplate.builder("@org.junit.jupiter.api.Test")
                            .javaParser(javaParser())
                            .build()
                            .apply(getCursor(), a.getCoordinates().replace());
                } else {
                    return a.withArguments(null)
                            .withType(JavaType.ShallowClass.build("org.junit.jupiter.api.Test"));
                }
            }
        }
    }
}
