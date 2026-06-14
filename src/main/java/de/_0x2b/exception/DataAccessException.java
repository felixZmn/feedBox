package de._0x2b.exception;

import java.sql.SQLException;

/**
 * Wraps a {@link SQLException} so the resource layer can map data-access
 * failures to an HTTP 500 response. Use this when you want callers to
 * react to a real database failure (lost connection, constraint violation
 * other than duplicate, broken SQL, etc.) instead of getting a silently
 * empty result.
 */
public class DataAccessException extends RuntimeException {
    public DataAccessException(String message, SQLException cause) {
        super(message, cause);
    }
}
