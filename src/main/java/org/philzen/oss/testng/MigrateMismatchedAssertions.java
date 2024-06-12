package org.philzen.oss.testng;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.NonNullApi;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;
import org.philzen.oss.utils.Parser;

import java.util.function.Function;

@NonNullApi
public class MigrateMismatchedAssertions extends Recipe {

    @Override
    public String getDisplayName() {
        return "Replace `Assert#assertEquals(actual[], expected[], delta [, message])` for float and double inputs";
    }

    @Override
    public String getDescription() {
        return "Replaces `org.testng.Assert#assertEquals(actual[], expected[], delta [, message])` with custom `org.junit.jupiter.api.Assertions#assertAll(() -> {})`.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        JavaVisitor<ExecutionContext> javaVisitor = new JavaVisitor<ExecutionContext>() {

            final Function<String, JavaTemplate> before = (type) -> JavaTemplate
                    .builder("org.testng.Assert.assertEquals(#{actual:anyArray(%s)}, #{expected:anyArray(%s)}, #{delta:any(%s)});".replace("%s", type))
                    .javaParser(Parser.runtime()).build();

            final Function<String, JavaTemplate> beforeWithMsg = (type) -> JavaTemplate
                    .builder("org.testng.Assert.assertEquals(#{actual:anyArray(%s)}, #{expected:anyArray(%s)}, #{delta:any(%s)}, #{message:any(java.lang.String)});".replace("%s", type))
                    .javaParser(Parser.runtime()).build();

            final JavaTemplate after = JavaTemplate
                    .builder("Assertions.assertAll(()->{\n    Assertions.assertEquals(#{expected:anyArray(float)}.length, #{actual:anyArray(float)}.length, \"Arrays don't have the same size.\");\n    for (int i = 0; i < #{actual}.length; i++) {\n        Assertions.assertEquals(#{expected}[i], #{actual}[i], #{delta:any(float)});\n    }\n});")
                    .imports("org.junit.jupiter.api.Assertions")
                    .javaParser(Parser.jupiter()).build();

            final JavaTemplate afterWithMsg = JavaTemplate
                    .builder("Assertions.assertAll(()->{\n    Assertions.assertEquals(#{expected:anyArray(float)}.length, #{actual:anyArray(float)}.length, \"Arrays don't have the same size.\");\n    for (int i = 0; i < #{actual}.length; i++) {\n        Assertions.assertEquals(#{expected}[i], #{actual}[i], #{delta:any(float)}, #{message:any(String)});\n    }\n});")
                    .imports("org.junit.jupiter.api.Assertions")
                    .javaParser(Parser.jupiter()).build();

            @Override
            public J visitMethodInvocation(J.MethodInvocation elem, ExecutionContext ctx) {
                JavaTemplate.Matcher matcher;
                if ((matcher = before.apply("float").matcher(getCursor())).find()
                || (matcher = before.apply("double").matcher(getCursor())).find())
                {
                    imports();
                    return after.apply(
                            getCursor(),
                            elem.getCoordinates().replace(),
                            matcher.parameter(1),
                            matcher.parameter(0),
                            matcher.parameter(2)
                    );
                } else if ((matcher = beforeWithMsg.apply("float").matcher(getCursor())).find()
                        || (matcher = beforeWithMsg.apply("double").matcher(getCursor())).find())
                {
                    imports();
                    return afterWithMsg.apply(
                            getCursor(),
                            elem.getCoordinates().replace(),
                            matcher.parameter(1),
                            matcher.parameter(0),
                            matcher.parameter(2),
                            matcher.parameter(3)
                    );
                }

                return super.visitMethodInvocation(elem, ctx);
            }

            private void imports() {
                maybeRemoveImport("org.testng.Assert");
                maybeAddImport("org.junit.jupiter.api.Assertions");
            }
        };

        return Preconditions.check(
                Preconditions.and(
                        new UsesType<>("org.testng.Assert", true),
                        new UsesMethod<>("org.testng.Assert assertEquals(..)")
                ),
                javaVisitor
        );
    }
}
