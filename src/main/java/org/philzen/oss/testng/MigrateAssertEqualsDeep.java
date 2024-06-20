package org.philzen.oss.testng;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.NonNullApi;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;
import org.philzen.oss.utils.Parser;

import java.util.Arrays;

@NonNullApi
public class MigrateAssertEqualsDeep extends Recipe {

    @Override
    public String getDisplayName() {
        return "Migrates `Assert#assertEqualsDeep(Map, Map)`";
    }

    @Override
    public String getDescription() {
        return "Migrate `org.testng.Assert#assertNotEquals(Map, Map)` to "
            + "`org.junit.jupiter.api.Assertions#assertIterableEquals(Set<Map.Entry>, Set<Map.Entry>)`.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        final JavaIsoVisitor<ExecutionContext> javaVisitor = new JavaIsoVisitor<ExecutionContext>() {
            
            final JavaTemplate before = JavaTemplate
                .builder("org.testng.Assert.assertEqualsDeep(#{actual:any(java.util.Map<?,?>)}, #{expected:any(java.util.Map<?,?>)});")
                .javaParser(Parser.runtime()).build();

            final String[] afterImports = {
                "java.util.AbstractMap", "java.util.Arrays",
                "java.util.Map", "java.util.stream.Collectors",
                "org.junit.jupiter.api.Assertions"
            };
            
            final JavaTemplate after = JavaTemplate
                .builder(
                    "Assertions.assertIterableEquals(\n" 
                        + "  #{expected:any(Map<?,?>)}.entrySet().stream().map(\n"
                        + "    entry -> entry.getValue() == null || !entry.getValue().getClass().isArray() ? entry\n"
                        + "      // convert array to List as the assertion needs an Iterable for proper comparison\n"
                        + "      : new AbstractMap.SimpleEntry<>(entry.getKey(), Arrays.asList(Object[].class.cast(entry.getValue())))\n"
                        + "  ).collect(Collectors.toSet()),\n"
                        + "  #{actual:any(Map<?,?>)}.entrySet().stream().map(\n"
                        + "    entry -> entry.getValue() == null || !entry.getValue().getClass().isArray() ? entry\n"
                        + "      // convert array to List as the assertion needs an Iterable for proper comparison\n"
                        + "      : new AbstractMap.SimpleEntry<>(entry.getKey(), Arrays.asList(Object[].class.cast(entry.getValue())))\n"
                        + "  ).collect(Collectors.toSet())\n"
                        + ");"
                ).imports(afterImports)
                .javaParser(Parser.jupiter()).build();

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation elem, ExecutionContext ctx) {
                final JavaTemplate.Matcher matcher = before.matcher(getCursor());
                if (matcher.find()) {
                    maybeRemoveImport("org.testng.Assert");
                    Arrays.stream(afterImports).forEach(this::maybeAddImport);
                    return after.apply(
                        getCursor(), elem.getCoordinates().replace(), matcher.parameter(1), matcher.parameter(0)
                    );
                }

                return super.visitMethodInvocation(elem, ctx);
            }
        };

        return Preconditions.check(
            Preconditions.and(
                new UsesType<>("org.testng.Assert", true),
                new UsesType<>("java.util.Map", true),
                new UsesMethod<>("org.testng.Assert assertEqualsDeep(..)")
            ),
            javaVisitor
        );
    }    
}
