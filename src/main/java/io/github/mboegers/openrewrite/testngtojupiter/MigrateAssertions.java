package io.github.mboegers.openrewrite.testngtojupiter;
/*
 * Copyright 2015-2024 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

import com.google.errorprone.refaster.annotation.AfterTemplate;
import com.google.errorprone.refaster.annotation.BeforeTemplate;
import org.junit.jupiter.api.Assertions;
import org.openrewrite.java.template.RecipeDescriptor;
import org.testng.Assert;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Spliterators;
import java.util.stream.StreamSupport;

@RecipeDescriptor(
        name = "Migrate TestNG Asserts to Jupiter",
        description = "Migrate all TestNG Assertions to JUnit Jupiter Assertions."
)
public class MigrateAssertions {

    @RecipeDescriptor(
            name = "Migrate `Assert#assertEquals(?, ?)` for array parameters",
            description = "Replace `org.testng.Assert#assertEquals(?, ?)` with `org.junit.jupiter.api.Assertions#assertArrayEquals(?, ?)`."
    )
    public static class MigrateAssertEqualsArray {

        @BeforeTemplate void before(Object[] actual, Object[] expected) {
            Assert.assertEquals(actual, expected);
        }

        @AfterTemplate void after(Object[] actual, Object[] expected) {
            Assertions.assertArrayEquals(expected, actual);
        }
    }

    @RecipeDescriptor(
        name = "Migrate `Assert#assertEquals(?, ?, String)` for Set, Object",
        description = "Replace `org.testng.Assert#assertEquals(?, ?, String)` with `org.junit.jupiter.api.Assertions#assertEquals(?, ?, String)`."
    )
    public static class MigrateAssertEqualsArrayWithMsg {

        @BeforeTemplate void before(Object[] actual, Object[] expected, String msg) {
            Assert.assertEquals(actual, expected, msg);
        }

        @AfterTemplate void after(Object[] actual, Object[] expected, String msg) {
            Assertions.assertArrayEquals(expected, actual, msg);
        }
    }

    @RecipeDescriptor(
        name = "Migrate `Assert#assertEquals(Iterator<?>, Iterator<?>)`",
        description = "Migrates `org.testng.Assert#assertEquals(Iterator<?>, Iterator<?>)` " +
            "to `org.junit.jupiter.api.Assertions#assertArrayEquals(Object[], Object[])`."
    )
    public static class MigrateAssertEqualsIterator {

        @BeforeTemplate void before(Iterator<?> actual, Iterator<?> expected) {
            Assert.assertEquals(actual, expected);
        }

        @AfterTemplate void after(Iterator<?> actual, Iterator<?> expected) {
            Assertions.assertArrayEquals(
                StreamSupport.stream(Spliterators.spliteratorUnknownSize(expected, 0), false).toArray(),
                StreamSupport.stream(Spliterators.spliteratorUnknownSize(actual, 0), false).toArray()
            );
        }
    }

    @RecipeDescriptor(
        name = "Migrate `Assert#assertEquals(Iterator<?>, Iterator<?>, String)`",
        description = "Migrates `org.testng.Assert#assertEquals(Iterator<?>, Iterator<?>, String)` " +
            "to `org.junit.jupiter.api.Assertions#assertArrayEquals(Object[], Object[], String)`."
    )
    public static class MigrateAssertEqualsIteratorWithMsg {

        @BeforeTemplate void before(Iterator<?> actual, Iterator<?> expected, String msg) {
            Assert.assertEquals(actual, expected, msg);
        }

        @AfterTemplate void after(Iterator<?> actual, Iterator<?> expected, String msg) {
            Assertions.assertArrayEquals(
                StreamSupport.stream(Spliterators.spliteratorUnknownSize(expected, 0), false).toArray(),
                StreamSupport.stream(Spliterators.spliteratorUnknownSize(actual, 0), false).toArray(),
                msg
            );
        }
    }

    @RecipeDescriptor(
            name = "Replace `Assert#assertEquals(?, ?)` for primitive values, boxed types and other non-array objects",
            description = "Replace `org.testng.Assert#assertEquals(?, ?)` with `org.junit.jupiter.api.Assertions#assertEquals(?, ?)`."
                    + "Always run *after* `MigrateAssertEqualsArrayRecipe` and `MigrateAssertEqualsIteratorRecipe`."
    )
    public static class MigrateAssertEquals {

        @BeforeTemplate void before(Object actual, Object expected) {
            Assert.assertEquals(actual, expected);
        }

        @AfterTemplate void after(Object actual, Object expected) {
            Assertions.assertEquals(expected, actual);
        }
    }

    @RecipeDescriptor(
            name = "Replace `Assert#assertEquals(?, ?, String)` for primitive values, boxed types and other non-array objects",
            description = "Replace `org.testng.Assert#assertEquals(?, ?, String)` with `org.junit.jupiter.api.Assertions#assertEquals(?, ?, String)`."
                + "Always run *after* `MigrateAssertEqualsArrayRecipe` and `MigrateAssertEqualsIteratorRecipe`."
    )
    public static class MigrateAssertEqualsWithMsg {

        @BeforeTemplate void before(Object actual, Object expected, String msg) {
            Assert.assertEquals(actual, expected, msg);
        }

        @AfterTemplate
        void after(Object actual, Object expected, String msg) {
            Assertions.assertEquals(expected, actual, msg);
        }
    }

    @RecipeDescriptor(
            name = "Replace `Assert#assertNotEquals(Object[], Object[])`",
            description = "Replace `org.testng.Assert#assertNotEquals(Object[], Object[])` with `org.junit.jupiter.api.Assertions#assertNotEquals(Arrays.toString(Object[], Arrays.toString(Object[]))`."
    )
    public static class MigrateAssertNotEqualsArray {

        @BeforeTemplate void before(Object[] actual, Object[] expected) {
            Assert.assertNotEquals(actual, expected);
        }

        @AfterTemplate void after(Object[] actual, Object[] expected) {
            Assertions.assertNotEquals(Arrays.toString(expected), Arrays.toString(actual));
        }
    }

    @RecipeDescriptor(
            name = "Replace `Assert#assertNotEquals(Object[], Object[], String)`",
            description = "Replace `org.testng.Assert#assertNotEquals(Object[], Object[])` with `org.junit.jupiter.api.Assertions#assertNotEquals(Arrays.toString(Object[], Arrays.toString(Object[]), String)`."
    )
    public static class MigrateAssertNotEqualsArrayWithMsg {

        @BeforeTemplate void before(Object[] actual, Object[] expected, String message) {
            Assert.assertNotEquals(actual, expected, message);
        }

        @AfterTemplate void after(Object[] actual, Object[] expected, String message) {
            Assertions.assertNotEquals(Arrays.toString(expected), Arrays.toString(actual), message);
        }
    }

    @RecipeDescriptor(
        name = "Migrate `Assert#assertNotEquals(Iterator<?>, Iterator<?>)`",
        description = "Migrates `org.testng.Assert#assertNotEquals(Iterator<?>, Iterator<?>)` " +
            "to `org.junit.jupiter.api.Assertions#assertNotEquals(String, String)` using `Arrays.toString()`."
    )
    public static class MigrateAssertNotEqualsIterator {

        @BeforeTemplate void before(Iterator<?> actual, Iterator<?> expected) {
            Assert.assertNotEquals(actual, expected);
        }

        @AfterTemplate void after(Iterator<?> actual, Iterator<?> expected) {
            Assertions.assertNotEquals(
                Arrays.toString(StreamSupport.stream(Spliterators.spliteratorUnknownSize(expected, 0), false).toArray()),
                Arrays.toString(StreamSupport.stream(Spliterators.spliteratorUnknownSize(actual, 0), false).toArray())
            );
        }
    }

    @RecipeDescriptor(
        name = "Migrate `Assert#assertEquals(Iterator<?>, Iterator<?>, String)`",
        description = "Migrates `org.testng.Assert#assertEquals(Iterator<?>, Iterator<?>, String)` " +
            "to `org.junit.jupiter.api.Assertions#assertArrayEquals(Object[], Object[], String)` using `Arrays.toString()`."
    )
    public static class MigrateAssertNotEqualsIteratorWithMsg {

        @BeforeTemplate void before(Iterator<?> actual, Iterator<?> expected, String msg) {
            Assert.assertNotEquals(actual, expected, msg);
        }

        @AfterTemplate void after(Iterator<?> actual, Iterator<?> expected, String msg) {
            Assertions.assertNotEquals(
                Arrays.toString(StreamSupport.stream(Spliterators.spliteratorUnknownSize(expected, 0), false).toArray()),
                Arrays.toString(StreamSupport.stream(Spliterators.spliteratorUnknownSize(actual, 0), false).toArray()),
                msg
            );
        }
    }

    @RecipeDescriptor(
        name = "Replace `Assert#assertNotEquals(?, ?)`",
        description = "Replace `org.testng.Assert#assertNotEquals(?, ?)` with `org.junit.jupiter.api.Assertions#assertNotEquals(?, ?)`."
            + "Always run *after* `MigrateAssertNotEqualsArrayRecipe` and `MigrateAssertNotEqualsIteratorRecipe`."
    )
    public static class MigrateAssertNotEquals {

        @BeforeTemplate void before(Object actual, Object expected) {
            Assert.assertNotEquals(actual, expected);
        }

        @AfterTemplate void after(Object actual, Object expected) {
            Assertions.assertNotEquals(expected, actual);
        }
    }
    
    @RecipeDescriptor(
        name = "Replace `Assert#assertNotEquals(?, ?, String)`",
        description = "Replace `org.testng.Assert#assertNotEquals(?, ?, String)` with `org.junit.jupiter.api.Assertions#assertNotEquals(?, ?, String)`."
            + "Always run *after* `MigrateAssertNotEqualsArrayWithMsgRecipe` and `MigrateAssertNotEqualsIteratorWithMsgRecipe`."
    )
    public static class MigrateAssertNotEqualsWithMsg {

        @BeforeTemplate void before(Object actual, Object expected, String msg) {
            Assert.assertNotEquals(actual, expected, msg);
        }

        @AfterTemplate void after(Object actual, Object expected, String msg) {
            Assertions.assertNotEquals(expected, actual, msg);
        }
    }

    @RecipeDescriptor(
            name = "Replace `Assert#assertFalse(boolean)`",
            description = "Replace `org.testng.Assert#assertFalse(boolean)` with `org.junit.jupiter.api.Assertions#assertFalse(boolean)`."
    )
    public static class MigrateAssertFalse {

        @BeforeTemplate
        void before(boolean expr) {
            Assert.assertFalse(expr);
        }

        @AfterTemplate
        void after(boolean expr) {
            Assertions.assertFalse(expr);
        }
    }

    @RecipeDescriptor(
            name = "Replace `Assert#assertFalse(boolean, String)`",
            description = "Replace `org.testng.Assert#assertFalse(boolean, String)` with `org.junit.jupiter.api.Assertions#assertFalse(boolean, String)`."
    )
    public static class MigrateAssertFalseWithMsg {

        @BeforeTemplate
        void before(boolean expr, String msg) {
            Assert.assertFalse(expr, msg);
        }

        @AfterTemplate
        void after(boolean expr, String msg) {
            Assertions.assertFalse(expr, msg);
        }
    }

    @RecipeDescriptor(
            name = "Replace `Assert#assertTrue(boolean)`",
            description = "Replace `org.testng.Assert#assertTrue(boolean)` with `org.junit.jupiter.api.Assertions#assertTrue(boolean)`."
    )
    public static class MigrateAssertTrue {

        @BeforeTemplate
        void before(boolean expr) {
            Assert.assertTrue(expr);
        }

        @AfterTemplate
        void after(boolean expr) {
            Assertions.assertTrue(expr);
        }
    }

    @RecipeDescriptor(
            name = "Replace `Assert#assertTrue(boolean, String)`",
            description = "Replace `org.testng.Assert#assertTrue(boolean, String)` with `org.junit.jupiter.api.Assertions#assertTrue(boolean, String)`."
    )
    public static class MigrateAssertTrueWithMsg {

        @BeforeTemplate
        void before(boolean expr, String msg) {
            Assert.assertTrue(expr, msg);
        }

        @AfterTemplate
        void after(boolean expr, String msg) {
            Assertions.assertTrue(expr, msg);
        }
    }

    @RecipeDescriptor(
            name = "Replace `Assert#assertNull(Object)`",
            description = "Replace `org.testng.Assert#assertNull(Object)` with `org.junit.jupiter.api.Assertions#assertNull(Object)`."
    )
    public static class MigrateAssertNull {

        @BeforeTemplate void before(Object expr) {
            Assert.assertNull(expr);
        }

        @AfterTemplate void after(Object expr) {
            Assertions.assertNull(expr);
        }
    }

    @RecipeDescriptor(
            name = "Replace `Assert#assertNull(Object, String)`",
            description = "Replace `org.testng.Assert#assertNull(Object, String)` with `org.junit.jupiter.api.Assertions#assertNull(Object, String)`."
    )
    public static class MigrateAssertNullWithMsg {

        @BeforeTemplate void before(Object expr, String msg) {
            Assert.assertNull(expr, msg);
        }

        @AfterTemplate void after(Object expr, String msg) {
            Assertions.assertNull(expr, msg);
        }
    }
<<<<<<< HEAD
=======

    @RecipeDescriptor(
            name = "Replace `Assert#assertNotNull(Object)`",
            description = "Replace `org.testng.Assert#assertNotNull(Object)` with `org.junit.jupiter.api.Assertions#assertNotNull(Object)`."
    )
    public static class MigrateAssertNotNull {

        @BeforeTemplate void before(Object expr) {
            Assert.assertNotNull(expr);
        }

        @AfterTemplate void after(Object expr) {
            Assertions.assertNotNull(expr);
        }
    }

    @RecipeDescriptor(
            name = "Replace `Assert#assertNotNull(Object, String)`",
            description = "Replace `org.testng.Assert#assertNotNull(Object, String)` with `org.junit.jupiter.api.Assertions#assertNotNull(Object, String)`."
    )
    public static class MigrateAssertNotNullWithMsg {

        @BeforeTemplate void before(Object expr, String msg) {
            Assert.assertNotNull(expr, msg);
        }

        @AfterTemplate void after(Object expr, String msg) {
            Assertions.assertNotNull(expr, msg);
        }
    }

    @RecipeDescriptor(
            name = "Replace `Assert#fail()`",
            description = "Replace `org.testng.Assert#fail()` with `org.junit.jupiter.api.Assertions#fail()`."
    )
    public static class MigrateFailNoArgs {

        @BeforeTemplate void before() {
            Assert.fail();
        }

        @AfterTemplate void after() {
            Assertions.fail();
        }
    }

    @RecipeDescriptor(
            name = "Replace `Assert#fail(String)`",
            description = "Replace `org.testng.Assert#fail(String)` with `org.junit.jupiter.api.Assertions#fail(String)`."
    )
    public static class MigrateFailWithMessage {

        @BeforeTemplate void before(String message) {
            Assert.fail(message);
        }

        @AfterTemplate void after(String message) {
            Assertions.fail(message);
        }
    }

    @RecipeDescriptor(
            name = "Replace `Assert#fail(String)`",
            description = "Replace `org.testng.Assert#fail(String)` with `org.junit.jupiter.api.Assertions#fail(String)`."
    )
    public static class MigrateFailWithMessageAndCause {

        @BeforeTemplate void before(String message, Throwable cause) {
            Assert.fail(message, cause);
        }

        @AfterTemplate void after(String message, Throwable cause) {
            Assertions.fail(message, cause);
        }
    }
>>>>>>> 3c937e8 (fixup! Implement assertNull() migration)
}
