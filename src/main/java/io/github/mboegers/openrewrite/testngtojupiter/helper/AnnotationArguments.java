package io.github.mboegers.openrewrite.testngtojupiter.helper;

import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Answer questions regarding annotation arguments and their values
 *
 * @see J.Annotation
 */
public enum AnnotationArguments {;

    /**
     * Extracts all assignments with the given argument name from the annotation
     *
     * @param annotation   to extract the assignments from
     * @param argumentName to extract
     */
    public static List<Expression> extractAssignments(J.Annotation annotation, String argumentName) {
        
        final List<Expression> arguments = annotation.getArguments();
        if (arguments == null) {
            return Collections.emptyList();
        }

        return arguments.stream()
                .filter(J.Assignment.class::isInstance)
                .map(J.Assignment.class::cast)
                .filter(a -> argumentName.equals(((J.Identifier) a.getVariable()).getSimpleName()))
                .map(J.Assignment::getAssignment)
                .collect(Collectors.toList());
    }

    /**
     * Extract an annotation argument as literal
     *
     * @param annotation   to extract literal from
     * @param argumentName to extract
     * @param valueClass   expected type of the value
     * @param <T>          Type of the value
     * @return the value or Optional#empty
     */
    public static <T> Optional<T> extractLiteral(J.Annotation annotation, String argumentName, Class<T> valueClass) {

        final List<Expression> arguments = annotation.getArguments();
        if (arguments == null) {
            return Optional.empty();
        }

        return extractAssignments(annotation, argumentName).stream()
                .filter(J.Literal.class::isInstance)
                .map(J.Literal.class::cast)
                .findAny()
                .map(J.Literal::getValue)
                .map(valueClass::cast);
    }
}
