package de._0x2b.config;

import java.util.function.Function;

public final class ConfigUtils {
    private ConfigUtils() {
    }

    /**
     * Gets a required environment variable.
     *
     * @throws IllegalStateException if the variable is missing.
     */
    static String getEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required env var: " + name);
        }
        return value;
    }

    /**
     * Gets an environment variable with a default value.
     */
    static String getEnv(String name, String defaultValue) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value;
    }
}