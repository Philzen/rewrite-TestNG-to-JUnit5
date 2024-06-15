package org.philzen.oss.utils;

import org.openrewrite.internal.lang.NonNullApi;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.AnnotationMatcher;
import org.openrewrite.java.tree.J;

import java.util.Optional;

@NonNullApi
public enum Class {;
    
    @Nullable
    public static J.Annotation getAnnotation(J.ClassDeclaration classDeclaration, AnnotationMatcher annotation) {
        final Optional<J.Annotation> maybeAnnotation = classDeclaration.getLeadingAnnotations()
            .stream().filter(annotation::matches).findFirst();

        return maybeAnnotation.orElse(null);
    }
}
