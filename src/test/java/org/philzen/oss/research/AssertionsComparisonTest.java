package org.philzen.oss.research;

import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testng.Assert;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

class AssertionsComparisonTest {

    static final String[] ABC_array = {"a", "b", "c"};
    static final String[] CBA_array = {"c", "b", "a"};

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
    }

    void thisWillPass(final ThrowableAssert.ThrowingCallable code) {
        assertThatNoException().isThrownBy(code);
    }

    void thisWillFail(final ThrowableAssert.ThrowingCallable code) {
        assertThatExceptionOfType(AssertionError.class).isThrownBy(code);
    }
}
