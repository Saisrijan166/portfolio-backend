package com.srijan.portfolio.util;

public final class EnvironmentUtils {

    private EnvironmentUtils() {
    }

    public static String get(String key) {
        String propertyValue = System.getProperty(key);
        if (propertyValue != null && !propertyValue.isBlank()) {
            return propertyValue.trim();
        }

        String envValue = System.getenv(key);
        if (envValue != null && !envValue.isBlank()) {
            return envValue.trim();
        }

        return null;
    }
}
