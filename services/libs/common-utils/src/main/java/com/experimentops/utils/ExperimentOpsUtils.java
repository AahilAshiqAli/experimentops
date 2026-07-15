package com.experimentops.utils;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ExperimentOpsUtils {
    private ExperimentOpsUtils(){}

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String LETTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String DIGITS = "0123456789";
    private static final String SPECIAL_CHARS = "!@#$%^&*";

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

    public static long getSecondsDifference(Timestamp creationDate) {
        Instant creationDateInstant = creationDate.toInstant();
        Instant currentDateInstant =  Timestamp.valueOf(LocalDateTime.now(ZoneOffset.UTC)).toInstant();
        return ChronoUnit.SECONDS.between(creationDateInstant, currentDateInstant);
    }

    @NonNull
    public static String generatePassword(int length, int digitsLength, int specialCharLength) {
        if (length <= 0) {
            throw new IllegalArgumentException("password length must be greater than zero");
        }
        if (digitsLength < 0 || specialCharLength < 0) {
            throw new IllegalArgumentException("password character counts cannot be negative");
        }

        int letterLength = length - digitsLength - specialCharLength;
        if (letterLength < 0) {
            throw new IllegalArgumentException("password length must fit required digits and special characters");
        }

        List<Character> characters = new ArrayList<>(length);
        addRandomCharacters(characters, DIGITS, digitsLength);
        addRandomCharacters(characters, SPECIAL_CHARS, specialCharLength);
        addRandomCharacters(characters, LETTERS, letterLength);
        Collections.shuffle(characters, SECURE_RANDOM);

        StringBuilder password = new StringBuilder(length);
        for (Character character : characters) {
            password.append(character);
        }
        return password.toString();
    }

    private static void addRandomCharacters(List<Character> characters, String source, int count) {
        for (int i = 0; i < count; i++) {
            characters.add(source.charAt(SECURE_RANDOM.nextInt(source.length())));
        }
    }

}
