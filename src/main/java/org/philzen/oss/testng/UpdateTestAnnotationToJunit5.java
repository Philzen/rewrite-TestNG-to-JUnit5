package org.philzen.oss.testng;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
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
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;
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
    
    public static final String DESCRIPTION = "description";
    public static final String ENABLED = "enabled";
    public static final String EXPECTED_EXCEPTIONS = "expectedExceptions";
    public static final String EXPECTED_EXCEPTIONS_MSG_REG_EXP = "expectedExceptionsMessageRegExp";
    public static final String GROUPS = "groups";
    public static final String TIMEOUT = "timeOut";
    public static final Set<String> supportedAttributes = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
        DESCRIPTION, ENABLED, EXPECTED_EXCEPTIONS, EXPECTED_EXCEPTIONS_MSG_REG_EXP, GROUPS, TIMEOUT
    )));

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
            if (cu.findType(TESTNG_TYPE).isEmpty()) {
                return cu;
            }

            maybeRemoveImport(TESTNG_TYPE);
            return (J.CompilationUnit) 
                new ChangeType(TESTNG_TYPE, JUPITER_TYPE, true).getVisitor().visitNonNull(cu, ctx);
        }

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
            final J.Annotation testngAnnotation = Class.getAnnotation(classDecl, TESTNG_TEST);
            if (testngAnnotation != null) {
                final AnnotationVisitor av = new AnnotationVisitor(Collections.emptySet());
                av.visitAnnotation(testngAnnotation, ctx);
                if (av.misfit != null) {
                    classDecl = autoFormat(
                        classDecl.withLeadingAnnotations(ListUtils.concat(classDecl.getLeadingAnnotations(), av.misfit)), 
                        ctx
                    );
                }

                classDecl = Cleanup.removeAnnotation(classDecl, testngAnnotation);
                getCursor().putMessage(
                    // don't know a good way to determine if annotation is fully qualified, therefore determining
                    // it from the toString() method and passing on a code template for the JavaTemplate.Builder
                    "ADD_TO_ALL_METHODS", "@" + (testngAnnotation.toString().contains(".") ? JUPITER_TYPE : "Test")
                );
            }

            return super.visitClassDeclaration(classDecl, ctx);
        }

        @Override
        public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration m, ExecutionContext ctx) {
            final AnnotationVisitor av = new AnnotationVisitor(supportedAttributes);
            m = (J.MethodDeclaration) av.visitNonNull(m, ctx, getCursor().getParentOrThrow());

            if (av.misfit != null) {
                // add the non-migratable TestNG annotation alongside the new JUnit5 annotation
                m = autoFormat(m.withLeadingAnnotations(ListUtils.concat(m.getLeadingAnnotations(), av.misfit)), ctx);
            }

            if (av.parsed.isEmpty()) { // no attributes need to be migrated
                final String neededOnAllMethods = getCursor().getNearestMessage("ADD_TO_ALL_METHODS");
                if (neededOnAllMethods == null || Method.hasAnnotation(m, neededOnAllMethods) || m.isConstructor()
                    || !Method.isPublic(m) || Boolean.TRUE.equals(Method.isContainedInInnerClass(m))) {
                    return m;
                }
                
                return JavaTemplate.builder(neededOnAllMethods).javaParser(Parser.jupiter())
                    .imports(JUPITER_TYPE).build()
                    .apply(getCursor(), m.getCoordinates().addAnnotation(Sort.BELOW));
            }

            if (av.had(DESCRIPTION) && !J.Literal.isLiteralValue(av.get(DESCRIPTION), "")) {
                maybeAddImport(JUPITER_API_NAMESPACE + ".DisplayName");
                m = displayNameAnnotation.apply(
                    updateCursor(m), m.getCoordinates().addAnnotation(Sort.BELOW), av.get(DESCRIPTION)
                );
            }

            if (J.Literal.isLiteralValue(av.get(ENABLED), Boolean.FALSE)) {
                maybeAddImport(JUPITER_API_NAMESPACE + ".Disabled");
                m = disabledAnnotation.apply(updateCursor(m), m.getCoordinates().addAnnotation(Sort.BELOW));
            }

            final Expression expectedExceptionsValue = av.get(EXPECTED_EXCEPTIONS);
            final Expression firstExpectedException = (expectedExceptionsValue instanceof J.NewArray)
                // if attribute was given in { array form }, pick the first element (null is not allowed)
                ? Objects.requireNonNull(((J.NewArray) expectedExceptionsValue).getInitializer()).get(0)
                : expectedExceptionsValue;
            if (firstExpectedException instanceof J.FieldAccess
                // TestNG actually allows any type of Class here, however anything but a Throwable doesn't make sense 
                && TypeUtils.isAssignableTo("java.lang.Throwable", ((J.FieldAccess) firstExpectedException).getTarget().getType()))
            {
                m = junitExecutable.apply(updateCursor(m), m.getCoordinates().replaceBody(), m.getBody());

                maybeAddImport(JUPITER_ASSERTIONS_TYPE);
                final List<Object> parameters = Arrays.asList(firstExpectedException, Method.getFirstStatementLambdaAssignment(m));
                final String code = "Assertions.assertThrows(#{any(java.lang.Class)}, #{any(org.junit.jupiter.api.function.Executable)});";
                if (!(av.get(EXPECTED_EXCEPTIONS_MSG_REG_EXP) instanceof J.Literal)) {
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
                            ListUtils.concat(parameters, av.get(EXPECTED_EXCEPTIONS_MSG_REG_EXP)).toArray()
                        );
                }
            }

            if (av.had(GROUPS)) {
                final Expression groupsValue = av.get(GROUPS); 
                maybeAddImport(JUPITER_API_NAMESPACE + ".Tag");
                if (groupsValue instanceof J.Literal && !J.Literal.isLiteralValue(groupsValue, "")) {
                    m = tagAnnotation.apply(updateCursor(m), m.getCoordinates().addAnnotation(Sort.BELOW), groupsValue);
                } else if (groupsValue instanceof J.NewArray && ((J.NewArray) groupsValue).getInitializer() != null) {
                    final List<Expression> groups = ((J.NewArray) groupsValue).getInitializer();
                    for (Expression group : groups) {
                        if (group instanceof J.Empty) { 
                            continue; 
                        }
                        m = tagAnnotation.apply(updateCursor(m), m.getCoordinates().addAnnotation(Sort.BELOW), group);
                    }
                }
            }

            if (av.had(TIMEOUT)) {
                maybeAddImport("java.util.concurrent.TimeUnit");
                maybeAddImport(JUPITER_API_NAMESPACE + ".Timeout");
                m = timeoutAnnotation.apply(updateCursor(m), m.getCoordinates().addAnnotation(Sort.ABOVE), av.get(TIMEOUT));
            }

            return m;
        }

        /**
         * Parses annotation arguments, stores those that are migratable in a map (member <code>parsed</code>) 
         * and removes all arguments from the visited <code>@Test</code>-annotation.
         * <br>
         * The {@link AnnotationVisitor#misfit}-field will hold a fully qualified NgUnit @Test annotation 
         * retaining any arguments that are not migratable, if any were encountered. 
         */
        @RequiredArgsConstructor
        private static class AnnotationVisitor extends JavaIsoVisitor<ExecutionContext> {

            private final Set<String> supportedAttributes;
            
            /**
             * A fully qualified TestNG @Test annotation retaining any arguments that are not migratable 
             * or <code>null</code>, if none such arguments were encountered
             */
            J.Annotation misfit = null;

            static final String MISFIT_COMMENT = " ❗\uFE0F ❗\uFE0F ❗\uFE0F\n"
                + "   At least one `@Test`-attribute could not be migrated to JUnit 5. Kindly review the remainder below\n"
                + "   and manually apply any changes you may require to retain the existing test suite's behavior. Delete\n"
                + "↓  the annotation and this comment when satisfied, or use `git reset --hard` to roll back the migration.\n\n"
                + "   If you think this is a mistake or have an idea how this migration could be implemented instead, any\n"
                + "   feedback to https://github.com/Philzen/rewrite-TestNG-to-JUnit5/issues will be greatly appreciated.\n";

            /**
             * A map containing the migratable annotation arguments, if TestNG annotation was found
             */
            final Map<String, Expression> parsed = new HashMap<>(6, 100);

            public boolean had(String attribute) {
                return parsed.containsKey(attribute);
            }
            
            @Nullable
            public Expression get(String attribute) {
                return parsed.get(attribute);
            }
            
            @Override
            public J.Annotation visitAnnotation(J.Annotation a, ExecutionContext ctx) {
                final List<Expression> arguments = a.getArguments(); 
                if (arguments == null || !TESTNG_TEST.matches(a)) {
                    return a;
                }

                final List<Expression> misfitAttributes = new ArrayList<>(arguments.size());
                for (Expression arg : arguments) {
                    final J.Assignment assign = (J.Assignment) arg;
                    final String assignParamName = ((J.Identifier) assign.getVariable()).getSimpleName();
                    final Expression e = assign.getAssignment();
                    if (supportedAttributes.contains(assignParamName)) {
                        parsed.put(assignParamName, e);
                    } else {
                        misfitAttributes.add(arg);
                    }
                }

                if (!misfitAttributes.isEmpty()) {
                    misfit = a.withArguments(misfitAttributes)
                        // ↓ change to full qualification
                        .withAnnotationType(((J.Identifier) a.getAnnotationType()).withSimpleName(TESTNG_TYPE))
                        .withPrefix(Space.build("\n", Collections.emptyList()))
                        .withComments(Collections.singletonList(
                            new TextComment(true, MISFIT_COMMENT, "\n", Markers.EMPTY))
                        );
                } 

                // remove all attribute arguments (JUnit 5 @Test annotation doesn't allow any) 
                return a.withArguments(null);
            }
        }
    }
}
