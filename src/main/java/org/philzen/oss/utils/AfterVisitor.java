package org.philzen.oss.utils;

import lombok.RequiredArgsConstructor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.NonNullApi;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.Markup;

@NonNullApi
@RequiredArgsConstructor
public class AfterVisitor extends JavaIsoVisitor<ExecutionContext> {

    /**
     * The fully qualified type of the annotation that has to be gone now
     */
    private final String typeBeGone;
    
    @Override
    public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
        return cu.withClasses(ListUtils.map(cu.getClasses(), clazz -> (J.ClassDeclaration) visit(clazz, ctx)))
                // take one more pass over the imports now that we've had a chance to add warnings to all
                // uses of the type (that should have been removed) through the rest of the source file
                .withImports(ListUtils.map(cu.getImports(), anImport -> (J.Import) visit(anImport, ctx)));
    }

    @Override
    public J.Import visitImport(J.Import anImport, ExecutionContext ctx) {
        if (typeBeGone.equals(anImport.getTypeName())) {
            return Markup.error(anImport, new IllegalStateException("This import should have been removed by this recipe."));
        }
        return anImport;
    }

    @Override
    public JavaType visitType(@Nullable JavaType javaType, ExecutionContext ctx) {
        if (TypeUtils.isOfClassType(javaType, typeBeGone)) {
            getCursor().putMessageOnFirstEnclosing(J.class, "danglingTestRef", true);
        }
        return javaType;
    }

    @Override
    public J postVisit(J tree, ExecutionContext ctx) {
        if (getCursor().getMessage("danglingTestRef") != null) {
            return Markup.warn(tree, new IllegalStateException(
                    String.format("This still has a type of `%s`", typeBeGone)
            ));
        }
        return tree;
    }
}
