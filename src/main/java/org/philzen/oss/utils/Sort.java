package org.philzen.oss.utils;

import org.openrewrite.java.tree.J;

import java.util.Comparator;

public enum Sort {;
    public static final Comparator<J.Annotation> ABOVE = java.util.Comparator.comparing(J.Annotation::getSimpleName);
    public static final Comparator<J.Annotation> BELOW = java.util.Comparator.comparing(J.Annotation::getSimpleName).reversed();
}
