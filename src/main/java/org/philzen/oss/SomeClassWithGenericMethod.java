package org.philzen.oss;

import java.util.Map;

public class SomeClassWithGenericMethod {
    
    public static void someMethod(Map<?, ?> map) {
        // do something
    }
    
    public static void someSimilarMethod(Map<?, ?> map) {
        // do something
    }
    
    public static void otherMethod(Iterable<?> iterable) {
        // do something
    }
}
