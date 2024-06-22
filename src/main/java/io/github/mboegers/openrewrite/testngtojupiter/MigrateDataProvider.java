/*
 * Copyright 2015-2024 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package io.github.mboegers.openrewrite.testngtojupiter;

import io.github.mboegers.openrewrite.testngtojupiter.helper.AnnotationArguments;
import io.github.mboegers.openrewrite.testngtojupiter.helper.FindAnnotatedMethods;
import io.github.mboegers.openrewrite.testngtojupiter.helper.FindAnnotation;
import io.github.mboegers.openrewrite.testngtojupiter.helper.UsesAnnotation;
import org.openrewrite.*;
import org.openrewrite.java.*;
import org.openrewrite.java.tree.J;

import java.util.Set;

public class MigrateDataProvider extends Recipe {

    private static final AnnotationMatcher DATAPROVIDER_MATCHER = new AnnotationMatcher("@org.testng.annotations.DataProvider");

    @Override
    public String getDisplayName() {
        return "Migrate @DataProvider utilities";
    }

    @Override
    public String getDescription() {
        return "Wrap `@DataProvider` methods into a Jupiter parameterized test MethodSource.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesAnnotation<>(DATAPROVIDER_MATCHER), new MigrateDataProviderVisitor());
    }

    private class MigrateDataProviderVisitor extends JavaIsoVisitor<ExecutionContext> {

        private static final JavaTemplate methodeSourceTemplate = JavaTemplate.builder("""
                        public static Stream<Arguments> #{}Source() {
                          return Arrays.stream(#{}()).map(Arguments::of);
                        }
                        """)
                .imports("org.junit.jupiter.params.provider.Arguments", "java.util.Arrays", "java.util.stream.Stream")
                .contextSensitive()
                .javaParser(JavaParser.fromJavaVersion()
                        .logCompilationWarningsAndErrors(true)
                        .classpath("junit-jupiter-api", "junit-jupiter-params", "testng"))
                .build();
        private static final RemoveAnnotationVisitor removeAnnotationVisitor = new RemoveAnnotationVisitor(DATAPROVIDER_MATCHER);

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, org.openrewrite.ExecutionContext ctx) {
            classDecl = super.visitClassDeclaration(classDecl, ctx);

            Set<J.MethodDeclaration> dataProviders = FindAnnotatedMethods.find(classDecl, DATAPROVIDER_MATCHER);

            // for each add a Wrapper that translates to Jupiter method source
            for (J.MethodDeclaration provider : dataProviders) {
                String providerMethodName = provider.getSimpleName();
                String providerName = FindAnnotation.find(provider, DATAPROVIDER_MATCHER).stream().findAny()
                        .flatMap(j -> AnnotationArguments.extract(j, "name", String.class))
                        .orElse(providerMethodName);

                classDecl = classDecl.withBody(methodeSourceTemplate.apply(
                        new Cursor(getCursor(), classDecl.getBody()), classDecl.getBody().getCoordinates().lastStatement(),
                        providerName, providerMethodName));
            }

            // remove annotation and straighten imports
            doAfterVisit(new RemoveAnnotationVisitor(DATAPROVIDER_MATCHER));
            maybeRemoveImport("org.testng.annotations.DataProvider");
            maybeAddImport("org.junit.jupiter.params.provider.Arguments");
            maybeAddImport("java.util.Arrays");
            maybeAddImport("java.util.stream.Stream");

            return classDecl;
        }
    }
}
