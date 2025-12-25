package de._0x2b.feedBox;

public record AppConfig(String dbHost, int dbPort, String dbName, String dbUser, String dbPass, int appPort,
        int refreshRate) {
    public static AppConfig loadFromEnv() {
        return new AppConfig(
                getEnvOrThrow("PG_HOST"),
                Integer.parseInt(getEnvOrThrow("PG_PORT")),
                getEnvOrThrow("PG_DB"),
                getEnvOrThrow("PG_USER"),
                getEnvOrThrow("PG_PASSWORD"),
                Integer.parseInt(System.getenv().getOrDefault("PORT", "7070")),
                Integer.parseInt(System.getenv().getOrDefault("REFRESH_RATE", "60")));
    }

    private static String getEnvOrThrow(String key) {
        String val = System.getenv(key);
        if (val == null)
            throw new IllegalStateException(key + " not set");
        return val;
    }
}