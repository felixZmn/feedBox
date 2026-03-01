package de._0x2b.feedBox;

import java.util.Objects;

public record AppConfig(String dbHost, int dbPort, String dbUsername, String dbPassword, String dbName,
        long refreshInterval, int appPort, int networkTimeout, String userAgent) {

    // Factory method to load from environment variables
    public static AppConfig fromEnvironment() {
        Builder builder = new Builder();

        // Required parameters
        builder.dbHost(getEnvOrThrow("PG_HOST")).dbPort(Integer.parseInt(getEnvOrThrow("PG_PORT")))
                .dbUsername(getEnvOrThrow("PG_USERNAME")).dbName(getEnvOrThrow("PG_DB"))
                .dbPassword(getEnvOrThrow("PG_PASSWORD"));

        // Optional parameters with defaults
        String refreshIntervalStr = System.getenv("REFRESH_INTERVAL");
        if (refreshIntervalStr != null) {
            builder.refreshInterval(Long.parseLong(refreshIntervalStr));
        }
        String appPort = System.getenv("APP_PORT");
        if (appPort != null) {
            builder.appPort(Integer.parseInt(appPort));
        }
        String networkTimeout = System.getenv("NETWORK_TIMEOUT");
        if (networkTimeout != null) {
            builder.networkTimeout(Integer.parseInt(networkTimeout));
        }
        String userAgent = System.getenv("USER_AGENT");
        if (userAgent != null) {
            builder.userAgent(userAgent);
        }

        return builder.build();
    }

    private static String getEnvOrThrow(String key) {
        String value = System.getenv(key);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException("Environment variable " + key + " is required but not set");
        }
        return value;
    }

    // Builder pattern for configuration
    public static class Builder {
        int appPort = 7070;
        int networkTimeout = 30;
        String userAgent = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/144.0.0.0 Safari/537.36";
        private String dbHost;
        private int dbPort;
        private String dbUsername;
        private String dbPassword;
        private String dbName;
        private long refreshInterval = 60;

        public Builder dbHost(String dbHost) {
            this.dbHost = dbHost;
            return this;
        }

        public Builder dbPort(int dbPort) {
            this.dbPort = dbPort;
            return this;
        }

        public Builder dbUsername(String dbUsername) {
            this.dbUsername = dbUsername;
            return this;
        }

        public Builder dbPassword(String dbPassword) {
            this.dbPassword = dbPassword;
            return this;
        }

        public Builder dbName(String dbName) {
            this.dbName = dbName;
            return this;
        }

        public Builder refreshInterval(long refreshInterval) {
            this.refreshInterval = refreshInterval;
            return this;
        }

        public Builder appPort(int appPort) {
            this.appPort = appPort;
            return this;
        }

        public Builder networkTimeout(int networkTimeout) {
            this.networkTimeout = networkTimeout;
            return this;
        }

        public Builder userAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        public AppConfig build() {
            // Validate required fields
            Objects.requireNonNull(dbHost, "Database host is required");
            Objects.requireNonNull(dbUsername, "Database username is required");
            Objects.requireNonNull(dbPassword, "Database password is required");
            Objects.requireNonNull(dbName, "Database name is required");

            if (dbPort <= 0 || dbPort > 65535) {
                throw new IllegalArgumentException("Invalid database port: " + dbPort);
            }

            if (appPort <= 0 || appPort > 65535) {
                throw new IllegalArgumentException("Invalid database port: " + appPort);
            }

            if (networkTimeout <= 0) {
                throw new IllegalArgumentException("Timeout for Network Requests must be larger than 0");
            }

            if (userAgent.isEmpty() || userAgent.isBlank()) {
                throw new IllegalArgumentException("User Agent must not be empty");
            }

            return new AppConfig(dbHost, dbPort, dbUsername, dbPassword, dbName, refreshInterval, appPort,
                    networkTimeout, userAgent);
        }
    }
}