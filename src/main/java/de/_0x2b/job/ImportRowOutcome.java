package de._0x2b.job;

/**
 * One OPML feed-outline result for the client (sanitized; no SQL/stacks).
 */
public record ImportRowOutcome(
        String name,
        String xmlUrl,
        String status,
        String reason) {

    public static final String OK = "ok";
    public static final String FAIL = "fail";
    public static final String SKIP = "skip";

    public static ImportRowOutcome ok(String name, String xmlUrl) {
        return new ImportRowOutcome(truncate(name, 120), truncate(xmlUrl, 300), OK, null);
    }

    public static ImportRowOutcome fail(String name, String xmlUrl, String reason) {
        return new ImportRowOutcome(truncate(name, 120), truncate(xmlUrl, 300), FAIL, truncate(reason, 160));
    }

    public static ImportRowOutcome skip(String name, String xmlUrl, String reason) {
        return new ImportRowOutcome(truncate(name, 120), truncate(xmlUrl, 300), SKIP, truncate(reason, 160));
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        String trimmed = value.strip();
        if (trimmed.length() <= max) {
            return trimmed;
        }
        return trimmed.substring(0, max - 1) + "…";
    }
}
