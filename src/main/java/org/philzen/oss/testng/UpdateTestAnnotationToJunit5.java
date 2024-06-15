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
import org.openrewrite.java.AnnotationMatcher;
import org.openrewrite.java.ChangeType;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.search.FindImports;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;
import org.philzen.oss.utils.Class;
import org.philzen.oss.utils.*;

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
    public static final String JUPITER_ASSERTIONS_TYPE = JUPITER_API_NAMESPACE + ".Assertions";

    // inspired by https://github.com/openrewrite/rewrite-testing-frameworks/blob/4e8ba68b2a28a180f84de7bab9eb12b4643e342e/src/main/java/org/openrewrite/java/testing/junit5/UpdateTestAnnotation.java#
    private static class UpdateTestAnnotationToJunit5Visitor extends JavaIsoVisitor<ExecutionContext> {

        private static final AnnotationMatcher TESTNG_TEST = new AnnotationMatcher("@org.testng.annotations.Test");

        private final JavaTemplate displayNameAnnotation = JavaTemplate
                .builder("@DisplayName(#{any(java.lang.String)})")
                .imports(JUPITER_API_NAMESPACE + ".DisplayName")
                .javaParser(Parser.jupiter()).build();

        private final JavaTemplate disabledAnnotation = JavaTemplate
                .builder("@Disabled")
                .imports(JUPITER_API_NAMESPACE + ".Disabled")
                .javaParser(Parser.jupiter()).build();

        private final JavaTemplate junitExecutable = JavaTemplate
                .builder(JUPITER_API_NAMESPACE + ".function.Executable o = () -> #{};")
                .javaParser(Parser.jupiter()).build();

        private final JavaTemplate tagAnnotation = JavaTemplate
                .builder("@Tag(#{any(java.lang.String)})")
                .imports(JUPITER_API_NAMESPACE + ".Tag")
                .javaParser(Parser.jupiter()).build();

        private final JavaTemplate timeoutAnnotation = JavaTemplate
                .builder("@Timeout(value = #{any(long)}, unit = TimeUnit.MILLISECONDS)")
                .imports(JUPITER_API_NAMESPACE + ".Timeout", "java.util.concurrent.TimeUnit")
                .javaParser(Parser.jupiter()).build();

        @Override
        public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
            J.CompilationUnit c = super.visitCompilationUnit(cu, ctx);
            if (!c.findType(TESTNG_TYPE).isEmpty()) {
                // Update other references like `Test.class`.
                c = (J.CompilationUnit) new ChangeType(TESTNG_TYPE, JUPITER_TYPE, true)
                        .getVisitor().visitNonNull(c, ctx);
            }

            maybeRemoveImport(TESTNG_TYPE);
            doAfterVisit(new AfterVisitor(TESTNG_TYPE));

            return c;
        }

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, 
                                                        ExecutionContext executionContext) {

            final J.Annotation testAnnotation = Class.getAnnotation(classDecl, TESTNG_TEST);
            if (testAnnotation != null) {
                classDecl = Cleanup.removeAnnotation(classDecl, testAnnotation);

                getCursor().putMessage(
                    // don't know a good way to determine if annotation is fully qualified, therefore determining
                    // it from the toString() method and passing on a code template for the JavaTemplate.Builder
                    "ADD_TO_ALL_METHODS", "@" + (testAnnotation.toString().contains(".") ? JUPITER_TYPE : "Test")
                );
            }

            return super.visitClassDeclaration(classDecl, executionContext);
        }

        @Override
        public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
            final ChangeTestAnnotation cta = new ChangeTestAnnotation();
            J.MethodDeclaration m = (J.MethodDeclaration) cta.visitNonNull(method, ctx, getCursor().getParentOrThrow());
            
            // method identity changes when `@Test` annotation was found and migrated by ChangeTestAnnotation
            if (m == method) {
                final String neededOnAllMethods = getCursor().getNearestMessage("ADD_TO_ALL_METHODS");
                final boolean isContainedInInnerClass = Boolean.TRUE.equals(Method.isContainedInInnerClass(m));
                if (neededOnAllMethods == null || !Method.isPublic(m) || isContainedInInnerClass || m.isConstructor()) {
                    return super.visitMethodDeclaration(m, ctx);
                }
                
                return JavaTemplate.builder(neededOnAllMethods).javaParser(Parser.jupiter())
                    .imports(JUPITER_TYPE).build()
                    .apply(getCursor(), method.getCoordinates().addAnnotation(Comparator.comparing(J.Annotation::getSimpleName).reversed()));
            }

            if (cta.description != null && !J.Literal.isLiteralValue(cta.description, "")) {
                maybeAddImport(JUPITER_API_NAMESPACE + ".DisplayName");
                m = displayNameAnnotation.apply(
                    updateCursor(m),
                    m.getCoordinates().addAnnotation(Comparator.comparing(J.Annotation::getSimpleName).reversed()),
                    cta.description
                );
            }

            if (J.Literal.isLiteralValue(cta.enabled, Boolean.FALSE)) {
                maybeAddImport(JUPITER_API_NAMESPACE + ".Disabled");
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

                maybeAddImport(JUPITER_ASSERTIONS_TYPE);
                final List<Object> parameters = Arrays.asList(cta.expectedException, Method.getFirstStatementLambdaAssignment(m));
                final String code = "Assertions.assertThrows(#{any(java.lang.Class)}, #{any(org.junit.jupiter.api.function.Executable)});";
                if (!(cta.expectedExceptionMessageRegExp instanceof J.Literal)) {
                    m = JavaTemplate.builder(code).javaParser(Parser.jupiter())
                        .imports(JUPITER_ASSERTIONS_TYPE).build()
                        .apply(updateCursor(m), m.getCoordinates().replaceBody(), parameters.toArray());
                } else {
                    m = JavaTemplate.builder(
                            "final Throwable thrown = " + code + System.lineSeparator()
                                + "Assertions.assertTrue(thrown.getMessage().matches(#{any(java.lang.String)}));"
                        ).javaParser(Parser.jupiter()).imports(JUPITER_ASSERTIONS_TYPE).build()
                        .apply(
                            updateCursor(m), 
                            m.getCoordinates().replaceBody(), 
                            ListUtils.concat(parameters, cta.expectedExceptionMessageRegExp).toArray()
                        );
                }
            }

            if (cta.groups != null) {
                maybeAddImport(JUPITER_API_NAMESPACE + ".Tag");
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
                maybeAddImport(JUPITER_API_NAMESPACE + ".Timeout");
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

                // change @Test annotation type to JUnit 5 and remove all attribute arguments
                return a.withArguments(null).withType(JavaType.ShallowClass.build(JUPITER_TYPE));
            }
        }
    }
}
