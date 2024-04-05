package me.alek.honey.util;

import java.util.function.BiPredicate;
import java.util.function.Predicate;

public class PredicateUtils {

    public static final BiPredicate<String, String> STRING_EQUAL_COMPARATOR = String::equalsIgnoreCase;
    public static final BiPredicate<String, String> STRING_NOT_EQUAL_COMPARATOR = notBi(String::equalsIgnoreCase);

    public static final BiPredicate<Object, Object> EQUAL_COMPARATOR = Object::equals;
    public static final BiPredicate<Object, Object> NOT_EQUAL_COMPARATOR = notBi(Object::equals);

    public static boolean equals(Object obj1, Object obj2) {
        return EQUAL_COMPARATOR.test(obj1, obj2);
    }

    public static boolean notEquals(Object obj1, Object obj2) {
        return NOT_EQUAL_COMPARATOR.test(obj1, obj2);
    }

    public static <T> Predicate<T> not(Predicate<T> predicate) {
        return (t) -> !predicate.test(t);
    }

    @SafeVarargs
    public static <T> Predicate<T> or(Predicate<T>... predicates) {
        return (t) -> {
            for (Predicate<T> predicate : predicates) {
                if (predicate.test(t)) return true;
            }
            return false;
        };
    }

    @SafeVarargs
    public static <T> Predicate<T> and(Predicate<T>... predicates) {
        return (t) -> {
            for (Predicate<T> predicate : predicates) {
                if (!predicate.test(t)) return false;
            }
            return true;
        };
    }

    public static <T, F> BiPredicate<T, F> notBi(BiPredicate<T, F> predicate) {
        return (t, f) -> !predicate.test(t, f);
    }

}
