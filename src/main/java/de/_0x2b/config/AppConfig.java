package de._0x2b.config;

import java.util.Objects;

import static de._0x2b.config.ConfigUtils.getEnv;

public record AppConfig(
        int refreshInterval, int port, int timeout, String userAgent
) {
    public AppConfig {
        Objects.requireNonNull(userAgent, "USER_AGENT cannot be null");
    }

    public static AppConfig fromEnv() {
        return new AppConfig(
                Integer.parseInt(getEnv("REFRESH_INTERVAL", "60")),
                Integer.parseInt(getEnv("APP_PORT", "7070")),
                Integer.parseInt(getEnv("NETWORK_TIMEOUT", "30")),
                getEnv("USER_AGENT", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/144.0.0.0 Safari/537.36")
        );
    }
}
