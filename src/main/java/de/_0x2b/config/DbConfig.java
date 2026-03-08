package de._0x2b.config;

import java.util.Objects;

import static de._0x2b.config.ConfigUtils.getEnv;

public record DbConfig(String host, int port, String dbName, String username, String password) {
    public DbConfig {
        Objects.requireNonNull(host, "PG_HOST cannot be null");
        Objects.requireNonNull(dbName, "PG_DB cannot be null");
        Objects.requireNonNull(username, "PG_USERNAME cannot be null");
        Objects.requireNonNull(password, "PG_PASSWORD cannot be null");
    }

    public static DbConfig fromEnv() {
        return new DbConfig(
                getEnv("PG_HOST"),
                Integer.parseInt(getEnv("PG_PORT")),
                getEnv("PG_DB"),
                getEnv("PG_USERNAME"),
                getEnv("PG_PASSWORD")
        );
    }
}