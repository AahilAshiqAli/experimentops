package com.experimentops.utils;

import java.util.Base64;
import java.util.Collections;
import java.util.Map;

public final class JwtUtil {
    private JwtUtil() {}

    public static Map<String, Object> parseToken(String token) {
        String[] parts = token.split("\\.");
        if (parts.length < 2) {
            return Collections.emptyMap();
        }
        String payload = decodeBase64Url(parts[1]);
        return JSONUtil.toMapFromJson(payload);
    }

    public static <T> T getClaim(String claim, Map<String, Object> claims, Class<T> clazz) {
        Object value = claims.get(claim);
        return clazz.isInstance(value) ? clazz.cast(value) : null;
    }

    private static String decodeBase64Url(String encodedString) {
        String base64String = encodedString.replace('-', '+').replace('_', '/');
        switch (base64String.length() % 4) {
            case 0 -> { }
            case 2 -> base64String += "==";
            case 3 -> base64String += "=";
            default -> throw new IllegalArgumentException("Illegal base64url string");
        }
        return new String(Base64.getDecoder().decode(base64String));
    }
}
