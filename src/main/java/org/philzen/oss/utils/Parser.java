package org.philzen.oss.utils;

import org.openrewrite.java.JavaParser;

public enum Parser {;

    private static final class JavaParserHolder {
        static final JavaParser.Builder<?, ?> jupiter = 
            JavaParser.fromJavaVersion().classpath("junit-jupiter-api");
        
        static final JavaParser.Builder<?, ?> runtimeClasspath = 
            JavaParser.fromJavaVersion().classpath(JavaParser.runtimeClasspath());
    }

    /**
     * Get a {@link JavaParser.Builder} with junit-jupiter-api added to the classpath
     */
    public static JavaParser.Builder<?, ?> jupiter() {
        return JavaParserHolder.jupiter;
    }

    /**
     * Get a {@link JavaParser.Builder} for the full runtime classpath
     */
    public static JavaParser.Builder<?, ?> runtime() {
        return JavaParserHolder.runtimeClasspath;
    }
}
