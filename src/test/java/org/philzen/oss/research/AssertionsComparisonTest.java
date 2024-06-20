package org.philzen.oss.research;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testng.Assert;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.*;

class AssertionsComparisonTest {

    static final Collection<String> ABC_list = ImmutableList.of("a", "b", "c");
    static final Collection<String> CBA_list = ImmutableList.of("c", "b", "a");

    static final String[] ABC_array = {"a", "b", "c"};
    static final String[] CBA_array = {"c", "b", "a"};

    static final Map<Integer, String> ASC_numMap = ImmutableMap.of(42, "Meaning of Life", 1312, "All Computers Are Broken");
    static final Map<Integer, String> DESC_numMap = ImmutableMap.of(1312, "All Computers Are Broken", 42, "Meaning of Life");
    static final Map<String, String> OTHER_map = ImmutableMap.of("foo", "far", "baz", "qux");

    static final Set<String> ABC_set = ImmutableSet.of("a", "b", "c");
    static final Set<String> CBA_set = ImmutableSet.of("c", "b", "a");
    static final Set<String> XYZ_set = ImmutableSet.of("x", "y", "z");

    @SuppressWarnings("AssertBetweenInconvertibleTypes")
    @Nested class assertEquals {

        @Tag("mismatch")
        @Test void array() {
            thisWillFail(() -> Assert.assertEquals(ABC_array, CBA_array));
            thisWillFail(() -> Assertions.assertEquals(CBA_array, ABC_array));

            thisWillPass(() -> Assert.assertEquals(ABC_array.clone(), ABC_array));
            thisWillFail(() -> Assertions.assertEquals(ABC_array, ABC_array.clone()));

            // possible migration
            thisWillPass(() -> Assertions.assertEquals(Arrays.toString(ABC_array), Arrays.toString(ABC_array.clone())));
        }
        
        @Tag("mismatch")
        @Test void iterator() {
            thisWillFail(() -> Assert.assertEquals(ABC_list.iterator(), CBA_list.iterator()));
            thisWillFail(() -> Assertions.assertEquals(CBA_list.iterator(), ABC_list.iterator()));

            thisWillPass(() -> Assert.assertEquals(ABC_list.iterator(), ABC_list.iterator()));
            thisWillFail(() -> Assertions.assertEquals(ABC_list.iterator(), ABC_list.iterator()));

            // possible migration
            thisWillPass(() -> Assertions.assertArrayEquals(
              StreamSupport.stream(Spliterators.spliteratorUnknownSize(ABC_list.iterator(), 0), false).toArray(),
              StreamSupport.stream(Spliterators.spliteratorUnknownSize(ABC_list.iterator(), 0), false).toArray()
            ));
            
            thisWillFail(() -> Assertions.assertArrayEquals(
              StreamSupport.stream(Spliterators.spliteratorUnknownSize(ABC_list.iterator(), 0), false).toArray(),
              StreamSupport.stream(Spliterators.spliteratorUnknownSize(CBA_list.iterator(), 0), false).toArray()
            ));
        }

        @Test void collection() {
            Collection<String> actual = new ArrayList<>(ABC_list);

            thisWillPass(() -> Assert.assertEquals(actual, new ArrayList<>(ABC_list)));
            thisWillPass(() -> Assertions.assertEquals(new ArrayList<>(ABC_list), actual));

            thisWillFail(() -> Assert.assertEquals(actual, CBA_list));
            thisWillFail(() -> Assertions.assertEquals(CBA_list, actual));
        }

        @Test void doubleDelta() {
            final double actual = 1d;

            thisWillPass(() -> Assert.assertEquals(actual, 2d, 1d));
            thisWillPass(() -> Assertions.assertEquals(2d, actual, 1d));

            thisWillFail(() -> Assert.assertEquals(actual, 2d, .999d));
            thisWillFail(() -> Assertions.assertEquals(2d, actual, .999d));
        }

        @Tag("missing")
        @Test void doubleArrayDelta() {
            final double[] expected = new double[] {0d, 10d};
            final double[] actual = new double[] {1d, 9d};
            thisWillPass(() -> Assert.assertEquals(actual, expected, 1d));
            // there is no equivalent in Jupiter :/

            thisWillFail(() -> Assert.assertEquals(actual, expected, .999d));
            // there is no equivalent in Jupiter :/

            // possible migration equivalent
            thisWillPass(() -> Assertions.assertAll(() -> {
                Assertions.assertEquals(expected.length, actual.length, "Arrays don't have the same size.");
                for (int i = 0; i < actual.length; i++) {
                    Assertions.assertEquals(expected[i], actual[i], 1d);
                }
            }));
            thisWillFail(() -> Assertions.assertAll(() -> {
                Assertions.assertEquals(expected.length, actual.length, "Arrays don't have the same size.");
                for (int i = 0; i < actual.length; i++) {
                    Assertions.assertEquals(expected[i], actual[i], .999d);
                }
            }));
        }

        @Test void floatDelta() {
            final float actual = 1f;

            thisWillPass(() -> Assert.assertEquals(actual, 2f, 1f));
            thisWillPass(() -> Assertions.assertEquals(2f, actual, 1f));

            thisWillFail(() -> Assert.assertEquals(actual, 2f, .999f));
            thisWillFail(() -> Assertions.assertEquals(2f, actual, .999f));
        }

        @Tag("missing")
        @Test void floatArrayDelta() {
            final double[] expected = new double[] {0d, 10f};
            final double[] actual = new double[] {1f, 9f};
            thisWillPass(() -> Assert.assertEquals(actual, expected, 1f));
            // there is no equivalent in Jupiter :/

            thisWillFail(() -> Assert.assertEquals(actual, expected, .999f));
            // there is no equivalent in Jupiter :/

            // possible migration equivalent
            thisWillPass(() -> Assertions.assertAll(() -> {
                Assertions.assertEquals(expected.length, actual.length, "Arrays don't have the same size.");
                for (int i = 0; i < actual.length; i++) {
                    Assertions.assertEquals(expected[i], actual[i], 1f);
                }
            }));
            thisWillFail(() -> Assertions.assertAll(() -> {
                Assertions.assertEquals(expected.length, actual.length, "Arrays don't have the same size.");
                for (int i = 0; i < actual.length; i++) {
                    Assertions.assertEquals(expected[i], actual[i], .999f);
                }
            }));
        }

        @Test void iterable() {
            final Iterable<String> actual = ABC_list;
            final Iterable<String> expected = new ArrayList<>(ABC_list);

            thisWillPass(() -> Assert.assertEquals(actual, expected));
            thisWillPass(() -> Assertions.assertEquals(expected, actual));

            thisWillFail(() -> Assert.assertEquals(actual, CBA_list));
            thisWillFail(() -> Assertions.assertEquals(CBA_list, actual));
        }

        @Test void map() {
            final Map<Integer, String> actual = ASC_numMap;

            thisWillPass(() -> Assert.assertEquals(actual, new LinkedHashMap<>(ASC_numMap)));
            thisWillPass(() -> Assertions.assertEquals(new LinkedHashMap<>(ASC_numMap), actual));

            // order does not matter
            thisWillPass(() -> Assert.assertEquals(actual, DESC_numMap));
            thisWillPass(() -> Assertions.assertEquals(DESC_numMap, actual));

            thisWillFail(() -> Assert.assertEquals(actual, OTHER_map));
            thisWillFail(() -> Assertions.assertEquals(OTHER_map, actual));
        }

        @Test void set() {
            final Set<Integer> actual = ASC_numMap.keySet();

            // order does not matter
            thisWillPass(() -> Assert.assertEquals(actual, DESC_numMap.keySet()));
            thisWillPass(() -> Assertions.assertEquals(DESC_numMap.keySet(), actual));

            thisWillFail(() -> Assert.assertEquals(actual, OTHER_map.keySet()));
            thisWillFail(() -> Assertions.assertEquals(OTHER_map.keySet(), actual));
        }
    }

    /**
     * The check is not as deep as one may guess from the name, it just does an additional
     * assertArrayEquals (where order matters!) for array (values only) and identity assertion for object (values only) 
     * in the map (which assertEquals does not), however it does not traverse further into nested maps and alike
     */
    @Nested class assertEqualsDeep {
        
        final Map<String, Object> expected = Map.of("1", "one", "array", ABC_array, "map", ASC_numMap);
        // ↓ matches because Objects.equals returns true 
        final Map<String, Object> matchingActual = Map.of("1", "one", "array", ABC_array, "map", Map.copyOf(ASC_numMap));
        // ↓ matches because Array elements match returns true 
        final Map<String, Object> alsoMatchingActual = Map.of("1", "one", "array", ABC_array.clone(), "map", Map.copyOf(ASC_numMap));
        // ↓ will fail as order of array keys does matter here
        final Map<String, Object> failingActual = Map.of("1", "one", "array", CBA_array, "map", ASC_numMap);
        
        @Test void map() {
            thisWillPass(() -> Assert.assertEqualsDeep(matchingActual, expected));
            thisWillPass(() -> Assert.assertEqualsDeep(alsoMatchingActual, expected));
            thisWillFail(() -> Assert.assertEqualsDeep(failingActual, expected));

            // there is no equivalent in JUnit5, potential migration (not pretty, but works)
            thisWillPass(
              () -> Assertions.assertIterableEquals(
                expected.entrySet().stream().map(
                  entry -> entry.getValue() == null || !entry.getValue().getClass().isArray() ? entry
                    // convert array to List as the assertion needs an Iterable for proper comparison
                    : new AbstractMap.SimpleEntry<>(entry.getKey(), Arrays.asList((Object[]) entry.getValue()))
                ).collect(Collectors.toSet()),
                matchingActual.entrySet().stream().map(
                  entry -> entry.getValue() == null || !entry.getValue().getClass().isArray() ? entry
                    // convert array to List as the assertion needs an Iterable for proper comparison
                    : new AbstractMap.SimpleEntry<>(entry.getKey(), Arrays.asList((Object[]) entry.getValue()))
                ).collect(Collectors.toSet())
            ));
            
            thisWillPass(
              () -> Assertions.assertIterableEquals(
                expected.entrySet().stream().map(
                  entry -> entry.getValue() == null || !entry.getValue().getClass().isArray() ? entry
                    // convert array to List as the assertion needs an Iterable for proper comparison
                    : new AbstractMap.SimpleEntry<>(entry.getKey(), Arrays.asList((Object[]) entry.getValue()))
                ).collect(Collectors.toSet()),
                alsoMatchingActual.entrySet().stream().map(
                  entry -> entry.getValue() == null || !entry.getValue().getClass().isArray() ? entry
                    // convert array to List as the assertion needs an Iterable for proper comparison
                    : new AbstractMap.SimpleEntry<>(entry.getKey(), Arrays.asList((Object[]) entry.getValue()))
                ).collect(Collectors.toSet())
            ));

            thisWillFail(
              () -> Assertions.assertIterableEquals(
                expected.entrySet().stream().map(
                  entry -> entry.getValue() == null || !entry.getValue().getClass().isArray() ? entry
                    // convert array to List as the assertion needs an Iterable for proper comparison
                    : new AbstractMap.SimpleEntry<>(entry.getKey(), Arrays.asList((Object[]) entry.getValue()))
                ).collect(Collectors.toSet()),
                failingActual.entrySet().stream().map(
                  entry -> entry.getValue() == null || !entry.getValue().getClass().isArray() ? entry
                    // convert array to List as the assertion needs an Iterable for proper comparison
                    : new AbstractMap.SimpleEntry<>(entry.getKey(), Arrays.asList((Object[]) entry.getValue()))
                ).collect(Collectors.toSet())
            ));

            // SPECIAL EDGE CASE (hoorray, generics frenzy!): casting with (Object[]) will not compile if the
            // compiler can infer that the Map does not contain any arrays
            thisWillPass(
              () -> Assertions.assertIterableEquals(
                Map.of("test", 1).entrySet().stream().map(
                  entry -> entry.getValue() == null || !entry.getValue().getClass().isArray() ? entry
                    // convert array to List as the assertion needs an Iterable for proper comparison
                    : new AbstractMap.SimpleEntry<>(entry.getKey(), Arrays.asList(Object[].class.cast(entry.getValue())))
                ).collect(Collectors.toSet()),
                Map.of("test", 1).entrySet().stream().map(
                  entry -> entry.getValue() == null || !entry.getValue().getClass().isArray() ? entry
                    // convert array to List as the assertion needs an Iterable for proper comparison
                    : new AbstractMap.SimpleEntry<>(entry.getKey(), Arrays.asList(Object[].class.cast(entry.getValue())))
                ).collect(Collectors.toSet())
            ));
        }
    }

    @Tag("missing")
    @Nested class assertEqualsNoOrder { // there is no equivalent in Jupiter

        @Test void collection() {
            final Collection<String> expected = ABC_list;

            thisWillPass(() -> Assert.assertEqualsNoOrder(CBA_list, expected));
            thisWillFail(() -> Assert.assertEqualsNoOrder(List.of("x", "y"), expected));

            // possible migration (string only)
            thisWillPass(() -> Assertions.assertLinesMatch(expected.stream().sorted(), CBA_list.stream().sorted()));
            thisWillFail(() -> Assertions.assertLinesMatch(expected.stream().sorted(), Stream.of("x", "y").sorted()));

            // possible migration (any type)
            thisWillPass(() -> Assertions.assertArrayEquals(
              expected.stream().sorted().toArray(), CBA_list.stream().sorted().toArray()
            ));
            thisWillFail(() -> Assertions.assertArrayEquals(
              expected.stream().sorted().toArray(), Stream.of("x", "y").sorted().toArray()
            ));
        }

        @Test void iterator() {
            thisWillPass(() -> Assert.assertEqualsNoOrder(CBA_list.iterator(), ABC_list.iterator()));
            thisWillFail(() -> Assert.assertEqualsNoOrder(List.of("x", "y").iterator(), ABC_list.iterator()));

            // possible migration
            thisWillPass(() -> Assertions.assertArrayEquals(
              StreamSupport.stream(Spliterators.spliteratorUnknownSize(CBA_list.iterator(), 0), false).sorted().toArray(),
              StreamSupport.stream(Spliterators.spliteratorUnknownSize(ABC_list.iterator(), 0), false).sorted().toArray()
            ));
            thisWillFail(() -> Assertions.assertArrayEquals(
              StreamSupport.stream(Spliterators.spliteratorUnknownSize(CBA_list.iterator(), 0), false).sorted().toArray(),
              StreamSupport.stream(Spliterators.spliteratorUnknownSize(List.of("x", "y").iterator(), 0), false).sorted().toArray()
            ));
        }

        @Test void objectArray() {
            final Object[] expected = new String[]{"a", "b", "c", "d"};

            thisWillPass(() -> Assert.assertEqualsNoOrder(new String[]{"b", "a", "d", "c"}, expected));

            thisWillFail(() -> Assert.assertEqualsNoOrder(new String[]{"b", "b", "a", "d", "c"}, expected));

            // possible migration
            thisWillPass(() -> Assertions.assertArrayEquals(
                Arrays.stream(expected).sorted().toArray(),
                Arrays.stream(new String[]{"b", "a", "d", "c"}).sorted().toArray()
            ));

            // possible migration
            thisWillFail(() -> Assertions.assertEquals(
                Arrays.stream(expected).sorted(),
                Arrays.stream(new String[]{"b", "b", "a", "d", "c"}).sorted()
            ));
        }
    }

    @Nested class assertNotEquals {

        @Tag("mismatch")
        @Test void array() {
            thisWillPass(() -> Assert.assertNotEquals(ABC_array, CBA_array));
            thisWillPass(() -> Assertions.assertNotEquals(CBA_array, ABC_array));

            thisWillFail(() -> Assert.assertNotEquals(ABC_array.clone(), ABC_array));
            thisWillPass(() -> Assertions.assertNotEquals(ABC_array, ABC_array.clone()));

            // possible migration
            thisWillFail(() -> Assertions.assertNotEquals(Arrays.toString(ABC_array), Arrays.toString(ABC_array.clone())));
        }

        @Test void collection() {
            thisWillPass(() -> Assert.assertNotEquals(ABC_list, CBA_list));
            thisWillPass(() -> Assertions.assertNotEquals(CBA_list, ABC_list));

            thisWillFail(() -> Assert.assertNotEquals(ImmutableList.copyOf(ABC_list), ABC_list));
            thisWillFail(() -> Assertions.assertNotEquals(ABC_list, ImmutableList.copyOf(ABC_list)));
        }

        @Tag("mismatch")
        @Test void iterator() {
            thisWillPass(() -> Assert.assertNotEquals(ABC_list.iterator(), CBA_list.iterator()));
            thisWillPass(() -> Assertions.assertNotEquals(CBA_list.iterator(), ABC_list.iterator()));

            thisWillFail(() -> Assert.assertNotEquals(ABC_list.iterator(), ABC_list.iterator()));
            thisWillPass(() -> Assertions.assertNotEquals(ABC_list.iterator(), ABC_list.iterator()));

            // possible migration
            thisWillPass(() -> Assertions.assertNotEquals(
              Arrays.toString(StreamSupport.stream(Spliterators.spliteratorUnknownSize(ABC_list.iterator(), 0), false).toArray()),
              Arrays.toString(StreamSupport.stream(Spliterators.spliteratorUnknownSize(CBA_list.iterator(), 0), false).toArray())
            ));
            thisWillFail(() -> Assertions.assertNotEquals(
              Arrays.toString(StreamSupport.stream(Spliterators.spliteratorUnknownSize(ABC_list.iterator(), 0), false).toArray()),
              Arrays.toString(StreamSupport.stream(Spliterators.spliteratorUnknownSize(ABC_list.iterator(), 0), false).toArray())
            ));
        }

        @Test void map() {
            thisWillPass(() -> Assert.assertNotEquals(ASC_numMap, OTHER_map));
            thisWillPass(() -> Assertions.assertNotEquals(OTHER_map, ASC_numMap));

            // order does not matter
            thisWillFail(() -> Assert.assertNotEquals(ASC_numMap, DESC_numMap));
            thisWillFail(() -> Assertions.assertNotEquals(DESC_numMap, ASC_numMap));

            thisWillFail(() -> Assert.assertNotEquals(ASC_numMap, ASC_numMap));
            thisWillFail(() -> Assertions.assertNotEquals(ASC_numMap, ASC_numMap));
        }

        @Test void set() {
            thisWillPass(() -> Assert.assertNotEquals(ABC_set, XYZ_set));
            thisWillPass(() -> Assertions.assertNotEquals(XYZ_set, ABC_set));

            // order does not matter
            thisWillFail(() -> Assert.assertNotEquals(ABC_set, CBA_set));
            thisWillFail(() -> Assertions.assertNotEquals(CBA_set, ABC_set));

            thisWillFail(() -> Assert.assertNotEquals(ABC_set, ABC_set));
            thisWillFail(() -> Assertions.assertNotEquals(ABC_set, ABC_set));
        }

        @Test void doubleDelta() {
            final double actual = 1d;
            thisWillPass(() -> Assert.assertNotEquals(actual, 2d, .999d));
            thisWillPass(() -> Assertions.assertNotEquals(2d, actual, .999d));

            thisWillFail(() -> Assert.assertNotEquals(actual, 2d, 1d));
            thisWillFail(() -> Assertions.assertNotEquals(2d, actual, 1d));
        }

        @Test void floatDelta() {
            final float actual = 1f;

            thisWillPass(() -> Assert.assertNotEquals(actual, 2f, .999f));
            thisWillPass(() -> Assertions.assertNotEquals(2f, actual, .999f));

            thisWillFail(() -> Assert.assertNotEquals(actual, 2f, 1f));
            thisWillFail(() -> Assertions.assertNotEquals(2f, actual, 1f));
        }
    }

    @Test void assertNotSame() {
        final Collection<String> expected = ABC_list;

        thisWillPass(() -> Assertions.assertNotSame(new ArrayList<>(ABC_list), expected));
        thisWillPass(() -> Assert.assertNotSame(expected, new ArrayList<>(ABC_list)));

        thisWillFail(() -> Assert.assertNotSame(ABC_list, expected));
        thisWillFail(() -> Assertions.assertNotSame(expected, ABC_list));
    }

    @Test void assertSame() {
        final Collection<String> expected = ABC_list;

        thisWillPass(() -> Assert.assertSame(ABC_list, expected));
        thisWillPass(() -> Assertions.assertSame(expected, ABC_list));

        thisWillFail(() -> Assert.assertSame(new ArrayList<>(ABC_list), expected));
        thisWillFail(() -> Assertions.assertSame(expected, new ArrayList<>(ABC_list)));
    }
    
    @SuppressWarnings("unused")
    @Nested class assertThrows {
        
        @Test void onlyRunnable() {
            thisWillPass(() -> Assert.assertThrows(() -> { throw new RuntimeException(); }));
            thisWillFail(() -> Assert.assertThrows(() -> { final var meaningful = 42; }));

            // not a 1:1 equivalent in terms of arguments, but functionally completely equivalent
            thisWillPass(() -> Assertions.assertThrows(Throwable.class, () -> { throw new RuntimeException(); }));
            thisWillFail(() -> Assertions.assertThrows(Throwable.class, () -> { final var meaningful = 42; }));
        }
        
        @Test void expectedExceptionClassCheck() {
            thisWillPass(() -> Assert.assertThrows(RuntimeException.class, () -> { throw new RuntimeException(); }));
            thisWillFail(() -> Assert.assertThrows(Error.class, () -> { throw new RuntimeException(); }));

            thisWillPass(() -> Assertions.assertThrows(RuntimeException.class, () -> { throw new RuntimeException(); }));
            thisWillFail(() -> Assertions.assertThrows(Error.class, () -> { throw new RuntimeException(); }));
            
            // additional test to show that they both do NOT check for the exact type, "instance of"-truthyness is enough 
            thisWillPass(() -> Assert.assertThrows(Exception.class, () -> { throw new RuntimeException(); }));
            thisWillFail(() -> Assert.assertThrows(Error.class, () -> { throw new RuntimeException(); }));

            thisWillPass(() -> Assertions.assertThrows(Exception.class, () -> { throw new RuntimeException(); }));
            thisWillFail(() -> Assertions.assertThrows(Error.class, () -> { throw new RuntimeException(); }));
        }

        /**
         * This is the actual function being called by {@link Assert#assertThrows(Assert.ThrowingRunnable)}
         * and {@link Assert#assertThrows(Class, Assert.ThrowingRunnable)}, so these migrations are identical
         */
        @Test void expectThrows() {
            thisWillPass(() -> Assert.expectThrows(RuntimeException.class, () -> { throw new RuntimeException(); }));
            thisWillFail(() -> Assert.expectThrows(Error.class, () -> { throw new RuntimeException(); }));

            thisWillPass(() -> Assertions.assertThrows(RuntimeException.class, () -> { throw new RuntimeException(); }));
            thisWillFail(() -> Assertions.assertThrows(Error.class, () -> { throw new RuntimeException(); }));

            // additional test to show that they both do NOT check for the exact type, "instance of"-truthyness is enough 
            thisWillPass(() -> Assert.expectThrows(Exception.class, () -> { throw new RuntimeException(); }));
            thisWillFail(() -> Assert.expectThrows(Error.class, () -> { throw new RuntimeException(); }));

            thisWillPass(() -> Assertions.assertThrows(Exception.class, () -> { throw new RuntimeException(); }));
            thisWillFail(() -> Assertions.assertThrows(Error.class, () -> { throw new RuntimeException(); }));
        }
    }

    void thisWillPass(final ThrowableAssert.ThrowingCallable code) {
        assertThatNoException().isThrownBy(code);
    }

    void thisWillFail(final ThrowableAssert.ThrowingCallable code) {
        assertThatExceptionOfType(AssertionError.class).isThrownBy(code);
    }
}
