package org.philzen.oss;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.NonNullApi;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;

import java.util.Arrays;

@NonNullApi
public class GenericsLstIssue extends Recipe {

    @Override
    public String getDisplayName() {
        return "";
    }

    @Override
    public String getDescription() {
        return ".";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        final JavaIsoVisitor<ExecutionContext> javaVisitor = new JavaIsoVisitor<ExecutionContext>() {
            
            final JavaTemplate before = JavaTemplate
                .builder("org.philzen.oss.SomeClassWithGenericMethod.someMethod(#{arg:any(java.util.Map<?,?>)});")
                .javaParser(JavaParser.fromJavaVersion().classpath(JavaParser.runtimeClasspath())).build();

            final String[] afterImports = {
                "java.util.Map", "org.philzen.oss.SomeClassWithGenericMethod"
            };
            
            final JavaTemplate after = JavaTemplate
                .builder("SomeClassWithGenericMethod.someSimilarMethod(#{arg:any(Map<?,?>)});")
                .imports(afterImports)
                .javaParser(JavaParser.fromJavaVersion().classpath(JavaParser.runtimeClasspath())).build();

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation elem, ExecutionContext ctx) {
                final JavaTemplate.Matcher matcher = before.matcher(getCursor());
                Arrays.stream(afterImports).forEach(this::maybeAddImport);
                if (matcher.find()) {
                    maybeAddImport("org.philzen.oss.SomeClassWithGenericMethod");
                    return after.apply(
                        getCursor(), elem.getCoordinates().replace(), matcher.parameter(0)
                    );
                }

                return super.visitMethodInvocation(elem, ctx);
            }
        };

        return Preconditions.check(
            Preconditions.and(
                new UsesType<>("java.util.Map", true),
                new UsesMethod<>("org.philzen.oss.SomeClassWithGenericMethod someMethod(..)")
            ),
            javaVisitor
        );
    }    
}
