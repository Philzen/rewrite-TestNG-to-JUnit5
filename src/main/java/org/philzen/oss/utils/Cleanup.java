package org.philzen.oss.utils;

import org.openrewrite.ExecutionContext;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JContainer;
import org.openrewrite.java.tree.Space;

import java.util.ArrayList;
import java.util.List;

public enum Cleanup {;

    /**
     * Removes an annotation and cleans the space that it occupied.
     * Same could be achieved with {@link org.openrewrite.java.RemoveAnnotationVisitor}, however
     * that would also traverse the whole LST underneath the class, yielding suboptimal performance.<br><br>
     *
     * Space cleaning algorithm borrowed from {@link org.openrewrite.java.RemoveAnnotationVisitor#visitClassDeclaration(J.ClassDeclaration, ExecutionContext)}
     */
    public static J.ClassDeclaration removeAnnotation(J.ClassDeclaration classDeclaration, J.Annotation a) {

        classDeclaration.getLeadingAnnotations().remove(a);
        if (!classDeclaration.getLeadingAnnotations().isEmpty()) {
            final List<J.Annotation> newLeadingAnnotations = new ArrayList<>();
            for (final J.Annotation other : classDeclaration.getLeadingAnnotations()) {
                newLeadingAnnotations.add(other.withPrefix(other.getPrefix().withWhitespace("")));
            }
            return classDeclaration.withLeadingAnnotations(newLeadingAnnotations);
        }

        final List<J.Modifier> modifiers = classDeclaration.getModifiers();
        if (!modifiers.isEmpty()) {
            return classDeclaration.withModifiers(Space.formatFirstPrefix(modifiers, Space.firstPrefix(modifiers).withWhitespace("")));
        }

        final JContainer<J.TypeParameter> typeParameters = classDeclaration.getPadding().getTypeParameters();
        if (typeParameters != null) {
            return classDeclaration.getPadding().withTypeParameters(typeParameters.withBefore(typeParameters.getBefore().withWhitespace("")));
        }

        final J.ClassDeclaration.Padding padding = classDeclaration.getPadding();
        return padding.withKind(padding.getKind().withPrefix(padding.getKind().getPrefix().withWhitespace("")));
    }
}
