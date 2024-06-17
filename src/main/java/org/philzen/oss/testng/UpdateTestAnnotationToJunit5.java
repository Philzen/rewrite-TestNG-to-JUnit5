package org.philzen.oss.testng;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.NonNullApi;
import org.openrewrite.java.AnnotationMatcher;
import org.openrewrite.java.ChangeType;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.search.FindImports;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeUtils;
import org.philzen.oss.utils.Class;
import org.philzen.oss.utils.*;

import java.util.*;

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
    public static final Set<String> supportedAttributes = new HashSet<>(Arrays.asList(
        "description", "enabled", "expectedExceptions", "expectedExceptionsMessageRegExp", "groups", "timeOut"
    ));

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
            cu = super.visitCompilationUnit(cu, ctx);
            if (getCursor().getMessage("DO_NOT_CHANGE_TYPE", false) || cu.findType(TESTNG_TYPE).isEmpty()) {
                return cu;
            }

            maybeRemoveImport(TESTNG_TYPE);
            return (J.CompilationUnit) 
                new ChangeType(TESTNG_TYPE, JUPITER_TYPE, true).getVisitor().visitNonNull(cu, ctx);
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

        /**
         * Scenarios:
         * 1. Noop – no TestNG annotation exists
         * 2. no attributes → not much to do, just return to cu visitor which will change type
         * 3. all migratable attributes → process the additional JUnit5 templates
         * 4. any non-migratable attributes → add misfit to method and process the additional JUnit5 templates 
         * 5. ALL non-migratable attributes → advise CU visitor DO_NOT_CHANGE_TYPE ??? 
         */
        @Override
        public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration m, ExecutionContext ctx) {
            final ProcessAnnotationAttributes cta = new ProcessAnnotationAttributes();
            m = (J.MethodDeclaration) cta.visitNonNull(m, ctx, getCursor().getParentOrThrow());
            
            if (cta.parsed.isEmpty()) { // no attributes need to be migrated
                final String neededOnAllMethods = getCursor().getNearestMessage("ADD_TO_ALL_METHODS");
                final boolean isContainedInInnerClass = Boolean.TRUE.equals(Method.isContainedInInnerClass(m));
                if (neededOnAllMethods == null || !Method.isPublic(m) || isContainedInInnerClass) {
                    getCursor().dropParentUntil(p -> p instanceof J.CompilationUnit)
                        // advise visitCompilationUnit to keep type if non-migratable attributes were found
                        .putMessage("DO_NOT_CHANGE_TYPE", cta.misfit != null);
                    return m;
                }
                
                return JavaTemplate.builder(neededOnAllMethods).javaParser(Parser.jupiter())
                    .imports(JUPITER_TYPE).build()
                    .apply(getCursor(), m.getCoordinates().addAnnotation(Sort.BELOW));
            }

            if (cta.misfit != null) {
                // add the non-migratable TestNG annotation alongside the new JUnit5 annotation
                m = autoFormat(m.withLeadingAnnotations(ListUtils.concat(m.getLeadingAnnotations(), cta.misfit)), ctx);
            }
            
            if (cta.parsed.containsKey("description") && !J.Literal.isLiteralValue(cta.parsed.get("description"), "")) {
                maybeAddImport(JUPITER_API_NAMESPACE + ".DisplayName");
                m = displayNameAnnotation.apply(
                    updateCursor(m), m.getCoordinates().addAnnotation(Sort.BELOW), cta.parsed.get("description")
                );
            }

            if (J.Literal.isLiteralValue(cta.parsed.get("enabled"), Boolean.FALSE)) {
                maybeAddImport(JUPITER_API_NAMESPACE + ".Disabled");
                m = disabledAnnotation.apply(updateCursor(m), m.getCoordinates().addAnnotation(Sort.BELOW));
            }

            if (cta.parsed.get("expectedExceptions") instanceof J.FieldAccess
                // TestNG actually allows any type of Class here, however anything but a Throwable doesn't make sense 
                && TypeUtils.isAssignableTo("java.lang.Throwable", ((J.FieldAccess) cta.parsed.get("expectedExceptions")).getTarget().getType()))
            {
                m = junitExecutable.apply(updateCursor(m), m.getCoordinates().replaceBody(), m.getBody());

                maybeAddImport(JUPITER_ASSERTIONS_TYPE);
                final List<Object> parameters = Arrays.asList(cta.parsed.get("expectedExceptions"), Method.getFirstStatementLambdaAssignment(m));
                final String code = "Assertions.assertThrows(#{any(java.lang.Class)}, #{any(org.junit.jupiter.api.function.Executable)});";
                if (!(cta.parsed.get("expectedExceptionsMessageRegExp") instanceof J.Literal)) {
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
                            ListUtils.concat(parameters, cta.parsed.get("expectedExceptionsMessageRegExp")).toArray()
                        );
                }
            }

            if (cta.parsed.get("groups") != null) {
                maybeAddImport(JUPITER_API_NAMESPACE + ".Tag");
                if (cta.parsed.get("groups") instanceof J.Literal && !J.Literal.isLiteralValue(cta.parsed.get("groups"), "")) {
                    m = tagAnnotation.apply(updateCursor(m), m.getCoordinates().addAnnotation(Sort.BELOW), cta.parsed.get("groups"));
                } else if (cta.parsed.get("groups") instanceof J.NewArray && ((J.NewArray) cta.parsed.get("groups")).getInitializer() != null) {
                    final List<Expression> groups = ((J.NewArray) cta.parsed.get("groups")).getInitializer();
                    for (Expression group : groups) {
                        if (group instanceof J.Empty) continue;
                        m = tagAnnotation.apply(updateCursor(m), m.getCoordinates().addAnnotation(Sort.BELOW), group);
                    }
                }
            }

            if (cta.parsed.get("timeOut") != null) {
                maybeAddImport("java.util.concurrent.TimeUnit");
                maybeAddImport(JUPITER_API_NAMESPACE + ".Timeout");
                m = timeoutAnnotation.apply(updateCursor(m), m.getCoordinates().addAnnotation(Sort.ABOVE), cta.parsed.get("timeOut"));
            }

            return m;
        }

        /**
         * Parses all annotation arguments, retains all that are migratable 
         * and removes them from the visited <code>@Test</code>-annotation
         */
        private static class ProcessAnnotationAttributes extends JavaIsoVisitor<ExecutionContext> {

            /**
             * TODO explanation
             */
            J.Annotation misfit;
            final Map<String, Expression> parsed = new HashMap<>(6, 100);

            @Override
            public J.Annotation visitAnnotation(J.Annotation a, ExecutionContext ctx) {
                if (a.getArguments() == null || !TESTNG_TEST.matches(a)) {
                    return a;
                }

                final List<Expression> misfitAttributes = new ArrayList<>(a.getArguments().size());
                for (Expression arg : a.getArguments()) {
                    final J.Assignment assign = (J.Assignment) arg;
                    final String assignParamName = ((J.Identifier) assign.getVariable()).getSimpleName();
                    final Expression e = assign.getAssignment();
                    if (supportedAttributes.contains(assignParamName)) {
                        if (!"expectedExceptions".equals(assignParamName)) {
                            parsed.put(assignParamName, e);
                        } else {
                            // TODO move this attribute treatment into the method visitation
                            // if attribute was given in { array form }, pick the first element (null is not allowed)
                            parsed.put(
                                assignParamName,
                                !(e instanceof J.NewArray)? e : Objects.requireNonNull(((J.NewArray) e).getInitializer()).get(0)
                            );
                        }
                    } else {
                        misfitAttributes.add(arg);
                    }
                }

                if (!misfitAttributes.isEmpty()) {
                    misfit = a.withArguments(misfitAttributes)
                        // ↓ change to full qualification
                        .withAnnotationType(((J.Identifier) a.getAnnotationType()).withSimpleName(TESTNG_TYPE));

                    if (parsed.isEmpty()) {
                        return a;
                    }
                } 

                // remove all attribute arguments (JUnit 5 @Test annotation doesn't allow any) 
                return a.withArguments(null);
            }
        }
    }
}
