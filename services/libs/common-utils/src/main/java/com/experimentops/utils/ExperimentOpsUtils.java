package com.experimentops.utils;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public class ExperimentOpsUtils {
    private ExperimentOpsUtils(){}

    public static boolean toNative(Boolean bool) {
        return bool != null && bool;
    }

    public static int toNative(@Nullable Integer input, int defVal) {
        return input != null ? input : defVal;
    }

    @Nullable public static BigDecimal toBigDecimal(@Nullable Integer input) {
        if (input == null) {
            return null;
        }
        return new BigDecimal(input);
    }

    @Nullable public static BigDecimal toBigDecimal(@Nullable String input) {
        if (input == null) {
            return null;
        }
        return new BigDecimal(input);
    }

    @Nullable public static BigDecimal toBigDecimal(@Nullable Double input) {
        if (input == null) {
            return null;
        }
        return BigDecimal.valueOf(input);
    }

    @Nullable public static Double toDouble(@Nullable String input) {
        if (input == null) {
            return null;
        }
        return Double.parseDouble(input);
    }

    @NonNull
    public static String uuid() {
        return UUID.randomUUID().toString();
    }


    public static <T> boolean isEmpty(@Nullable Collection<T> collection) {
        return collection == null || collection.isEmpty();
    }

    public static <K, V> boolean isEmpty(@Nullable Map<K, V> map) {
        return map == null || map.isEmpty();
    }


}
