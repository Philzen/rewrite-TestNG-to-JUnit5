package org.philzen.oss.utils;

import org.openrewrite.internal.lang.NonNullApi;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

@NonNullApi
public enum Method {;

    /**
     * Whether this method is declared in a nested class and not on the main class scope
     * @return Returns <code>null</code> in the fringe case that {@link J.MethodDeclaration#getMethodType()} is 
     *         <code>null</code> as it's not possible to query the parent scope information then (respectively
     *         it's not clear what <code>{@link J.MethodDeclaration#getMethodType()} == null</code> means) 
     */
    @Nullable
    public static Boolean isContainedInInnerClass(J.MethodDeclaration method) {
        final JavaType.Method methodType = method.getMethodType(); 
        if (methodType == null) {
            return null;
        }
        
        return methodType.getDeclaringType().getOwningClass() != null;
    }
    
    public static boolean isPublic(J.MethodDeclaration method) {
        return method.getModifiers().stream().anyMatch(mod -> mod.toString().equals("public"));
    }

    /**
     * Suppose you have a method that looks like this:
     * <pre><code>
     *     void method() {
     *         Supplier<String> x = () -> { return "x" };
     *     }
     * </code></pre><br>
     * Then this method will return the {@link J.Lambda} that represents <code>() -> { return "x" }</code>.
     * @return The lambda or <code>null</code>, if no such expression exists on the first statement of the method body
     */
    @Nullable
    public static J.Lambda getFirstStatementLambdaAssignment(J.MethodDeclaration method)  {
        final J.Block body = method.getBody();
        if (body == null) {
            return null;
        }
        
        return (J.Lambda) ((J.VariableDeclarations) body.getStatements().get(0)).getVariables().get(0).getInitializer();
    }

    public static boolean hasAnnotation(J.MethodDeclaration method, String literal) {
        return method.getLeadingAnnotations().stream().anyMatch(annotation -> annotation.toString().equals(literal));
    }
}
