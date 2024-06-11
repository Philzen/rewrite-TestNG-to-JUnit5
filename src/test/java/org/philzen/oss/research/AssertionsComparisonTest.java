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
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

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
    }

    void thisWillPass(final ThrowableAssert.ThrowingCallable code) {
        assertThatNoException().isThrownBy(code);
    }

    void thisWillFail(final ThrowableAssert.ThrowingCallable code) {
        assertThatExceptionOfType(AssertionError.class).isThrownBy(code);
    }
}
